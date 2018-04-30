package io.kotlintest.runner.junit4

import io.kotlintest.Spec
import io.kotlintest.TestScope
import io.kotlintest.TestResult
import io.kotlintest.TestStatus
import io.kotlintest.runner.jvm.TestRunnerListener
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import kotlin.reflect.KClass
import org.junit.runner.Description as JDescription

class JUnitTestRunnerListener(val testClass: KClass<out Spec>,
                              val notifier: RunNotifier) : TestRunnerListener {

  private fun desc(testScope: TestScope): JDescription =
      JDescription.createTestDescription(testClass.java.canonicalName, testScope.description.dropRoot().fullName())

  override fun executionStarted() {}
  override fun executionFinished(t: Throwable?) {}

  override fun executionStarted(spec: Spec) {}

  override fun executionFinished(spec: Spec, t: Throwable?) {}

  override fun executionStarted(scope: TestScope) {
    notifier.fireTestStarted(desc(scope))
  }

  override fun executionFinished(scope: TestScope, result: TestResult) {
    val desc = desc(scope)
    when (result.status) {
      TestStatus.Success -> notifier.fireTestFinished(desc)
      TestStatus.Error -> notifier.fireTestFailure(Failure(desc, result.error))
      TestStatus.Ignored -> notifier.fireTestIgnored(desc)
      TestStatus.Failure -> notifier.fireTestFailure(Failure(desc, result.error))
    }
  }


//
//  override fun executionStarted(scope: TestScope) {
//
//  }
//
//  override fun executionFinished(scope: TestScope, result: TestResult) {

//  }
//
//  override fun executionStarted() {}
//
//  override fun executionFinished(t: Throwable?) {}
}