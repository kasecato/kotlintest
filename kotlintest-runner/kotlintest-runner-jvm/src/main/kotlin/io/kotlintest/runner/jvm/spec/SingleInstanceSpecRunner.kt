package io.kotlintest.runner.jvm.spec

import io.kotlintest.Description
import io.kotlintest.Spec
import io.kotlintest.TestContext
import io.kotlintest.TestCase
import io.kotlintest.runner.jvm.TestCaseExecutor
import io.kotlintest.runner.jvm.TestEngineListener
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of [SpecRunner] that executes all tests in the same [Spec] instance.
 * That is, only a single instance of the spec class is ever instantiated.
 */
class SingleInstanceSpecRunner(listener: TestEngineListener) : SpecRunner(listener) {

  override fun execute(spec: Spec, coroutineContext: CoroutineContext) {
    interceptSpec(spec) {
      // creating the spec instance will have invoked the init block, resulting
      // in the top level test cases being available on the spec class
      topLevelTests(spec).forEach { TestCaseExecutor(listener, it, testContext(it.description, coroutineContext)).execute() }
    }
  }

  /**
   * Returns a [TestContext] where any registered tests are immediately executed in the same thread.
   */
  private fun testContext(description: Description, coroutineContext: CoroutineContext): TestContext = object : TestContext(coroutineContext) {

    // test names mapped to their line numbers, allows detection of duplicate test names
    // the line number is required because the same test is allowed to be invoked multiple times
    private val seen = HashMap<String, Int>()

    override fun description(): Description = description
    override fun registerTestCase(testCase: TestCase) {
      // if we have a test with this name already, but the line number is different
      // then it's a duplicate test name, so boom
      if (seen.containsKey(testCase.description.fullName()) && seen[testCase.description.fullName()] != testCase.line)
        throw IllegalStateException("Cannot add duplicate test name ${testCase.description.fullName()}")
      seen[testCase.description.fullName()] = testCase.line
      TestCaseExecutor(listener, testCase, testContext(testCase.description, coroutineContext)).execute()
    }
  }
}