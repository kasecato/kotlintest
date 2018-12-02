package io.kotlintest.runner.jvm.spec

import arrow.core.Failure
import arrow.core.Success
import io.kotlintest.Description
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestContext
import io.kotlintest.runner.jvm.TestEngineListener
import io.kotlintest.TestType
import io.kotlintest.runner.jvm.TestCaseExecutor
import io.kotlintest.runner.jvm.instantiateSpec
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of [SpecRunner] that executes each leaf test (that is a [TestCase] which
 * has type [TestType.Test]) in a fresh instance of the [Spec] class (that is, isolated
 * from other leaf executions).
 *
 * Each branch test (that is a [TestCase] of type [TestType.Container]) is only
 * executed as part of the execution "path" to the leaf test. In other words, the branch
 * tests are not executed "stand alone".
 *
 * So, given the following structure:
 *
 *  outerTest {
 *    innerTestA {
 *      // test
 *    }
 *    innerTestB {
 *      // test
 *    }
 *  }
 *
 * Two spec instances will be created. The execution process will be:
 *
 * spec1 = instantiate spec
 * spec1.outerTest
 * spec1.innerTestA
 * spec2 = instantiate spec
 * spec2.outerTest
 * spec2.innerTestB
 *
 * A failure in a branch test will prevent nested tests from executing.
 */
class InstancePerLeafTestCaseSpecRunner(listener: TestEngineListener) : SpecRunner(listener) {

  private val logger = LoggerFactory.getLogger(this.javaClass)
  private val queue = ArrayDeque<TestCase>()

  override fun execute(spec: Spec, coroutineContext: CoroutineContext) {
    topLevelTests(spec).forEach { enqueue(it) }
    while (queue.isNotEmpty()) {
      val element = queue.removeFirst()
      execute(element, coroutineContext)
    }
  }

  private fun enqueue(testCase: TestCase) {
    logger.debug("Enqueuing test ${testCase.description.fullName()}")
    queue.add(testCase)
  }

  // Starts executing a test, but we don't know if this test will be a leaf or a branch.
  // If it turns out to be a leaf, then we're done, lovely.
  // if it turns out to be a branch then we execute the first child it finds in the same
  // spec instance, but subsequent nested tests must be queued so that they can be executed
  // in fresh spec instances.
  private fun execute(testCase: TestCase, coroutineContext: CoroutineContext) {
    logger.debug("Executing $testCase")
    // we need to execute on a separate instance of the spec class
    // so we must instantiate a new space, locate the test we're trying to run, and then run it
    instantiateSpec(testCase.spec::class).let { specOrFailure ->
      when (specOrFailure) {
        is Failure -> throw specOrFailure.exception
        is Success -> {
          val spec = specOrFailure.value
          interceptSpec(spec) {
            spec.testCases().forEach { topLevel ->
              locate(topLevel, testCase.description) {
                TestCaseExecutor(listener, it, context(it, coroutineContext)).execute()
              }
            }
          }
        }
      }
    }
  }

  private fun context(current: TestCase, coroutineContext: CoroutineContext): TestContext = object : TestContext(coroutineContext) {
    private var found = false
    override fun description(): Description = current.description
    override fun registerTestCase(testCase: TestCase) {
      if (found) enqueue(testCase) else {
        found = true
        TestCaseExecutor(listener, testCase, context(testCase, coroutineContext)).execute()
      }
    }
  }

  // takes a current test case and a target test case and attempts to locate the target
  // by executing the current and it's nested tests recursively until we find the target
  private fun locate(current: TestCase, target: Description, callback: (TestCase) -> Unit) {
    // If the current test is the same as the target, we've found what we want, and can invoke
    // the callback. Otherwise we must execute the closure and check any registered tests to
    // see if they are on the desired path. If they are, we recurse into it.
    if (current.description == target) callback(current) else if (current.description.isAncestorOf(target)) {
      runBlocking {
        current.test.invoke(object : TestContext(coroutineContext) {
          override fun description(): Description = current.description
          override fun registerTestCase(testCase: TestCase) = locate(testCase, target, callback)
        })
      }
    }
  }
}