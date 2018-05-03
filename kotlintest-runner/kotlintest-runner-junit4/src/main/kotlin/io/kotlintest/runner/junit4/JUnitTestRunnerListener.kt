package io.kotlintest.runner.junit4

import io.kotlintest.Spec
import io.kotlintest.TestResult
import io.kotlintest.TestScope
import io.kotlintest.TestStatus
import io.kotlintest.runner.jvm.TestEngineListener
import io.kotlintest.runner.jvm.TestSet
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import kotlin.reflect.KClass
import org.junit.runner.Description as JDescription

class JUnitTestRunnerListener(val testClass: KClass<out Spec>,
                              val notifier: RunNotifier) : TestEngineListener {

  private fun desc(testScope: TestScope): JDescription =
      JDescription.createTestDescription(testClass.java.canonicalName, testScope.description.tail().fullName())

  override fun engineStarted(classes: List<KClass<out Spec>>) {}
  override fun engineFinished(t: Throwable?) {}

  override fun prepareScope(scope: TestScope) {
    notifier.fireTestStarted(desc(scope))
  }

  override fun completeScope(scope: TestScope, result: TestResult) {
    val desc = desc(scope)
    when (result.status) {
      TestStatus.Success -> notifier.fireTestFinished(desc)
      TestStatus.Error -> notifier.fireTestFailure(Failure(desc, result.error))
      TestStatus.Ignored -> notifier.fireTestIgnored(desc)
      TestStatus.Failure -> notifier.fireTestFailure(Failure(desc, result.error))
    }
  }

  override fun prepareSpec(spec: Spec) {}
  override fun completeSpec(spec: Spec, t: Throwable?) {}
  override fun prepareTestSet(set: TestSet) {}
  override fun testRun(set: TestSet, k: Int) {}
  override fun completeTestSet(set: TestSet, result: TestResult) {}
}
