package com.sksamuel.kotlintest.specs.annotation

import io.kotlintest.Description
import io.kotlintest.Spec
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import io.kotlintest.specs.Test

class AnnotationSpecCoroutineTest : AnnotationSpec() {

  var count = 0

  @Test
  suspend fun test1() {
    count += 1
  }

  @Test
  suspend fun test2() {
    count += 1
  }

  override fun afterSpec(description: Description, spec: Spec) {
    count shouldBe 2
  }
}