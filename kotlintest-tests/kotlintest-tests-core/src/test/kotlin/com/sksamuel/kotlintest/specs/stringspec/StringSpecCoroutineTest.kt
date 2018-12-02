package com.sksamuel.kotlintest.specs.stringspec

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

class StringSpecCoroutineTest : StringSpec() {

  var count = 0

  init {

    "string spec should support suspend functions" {
      longop()
      count shouldBe 1
    }

    "string spec should support async" {
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

  private suspend fun longop() {
    delay(1000)
    count += 1
  }
}

