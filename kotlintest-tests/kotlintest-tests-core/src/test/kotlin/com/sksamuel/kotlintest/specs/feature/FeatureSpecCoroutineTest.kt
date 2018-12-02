package com.sksamuel.kotlintest.specs.feature

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

class FeatureSpecCoroutineTest : FeatureSpec() {

  var count = 0

  init {

    feature("a feature") {
      scenario("can support suspend functions") {
        longop()
        count shouldBe 1
      }
      scenario("can support async") {
        val counter = AtomicInteger(0)
        val a = async {
          counter.incrementAndGet()
        }
        val b = async {
          counter.incrementAndGet()
        }
        a.await()
        b.await()
        counter.get() shouldBe 2
      }
    }
  }

  private suspend fun longop() {
    delay(1000)
    count += 1
  }
}