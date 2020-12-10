package com.chord.akkasharding.utils

import org.scalatest.FunSuite

class HelperTest extends FunSuite{

  test("Range Check 1"){
    val actual = Helper.rangeValidator(leftInclude = false,37,189,rightInclude = true,38)
    val expected = true
    assert(actual == expected)
  }

  test("Range Check 2"){
    val actual = Helper.rangeValidator(leftInclude = false,37,189,rightInclude = true,190)
    val expected = false
    assert(actual == expected)
  }

  test("Range Check 3"){
    val actual = Helper.rangeValidator(leftInclude = false,189,189,rightInclude = false,38)
    val expected = true
    assert(actual == expected)
  }

  test("Range Check 4"){
    val actual = Helper.rangeValidator(leftInclude = true,189,37,rightInclude = false,37)
    val expected = false
    assert(actual == expected)
  }

  test("Hash Check"){
    val actual = Helper.getIdentifier("Node_0")
    val expected = 189
    assert(actual == expected)
  }

}
