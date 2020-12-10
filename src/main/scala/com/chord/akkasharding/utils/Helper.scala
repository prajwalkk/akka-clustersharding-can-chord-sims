package com.chord.akkasharding.utils

import java.security.MessageDigest

import com.typesafe.scalalogging.LazyLogging

object Helper extends LazyLogging {


  def getIdentifier(input: String, algorithm: String = "SHA1"): Int = {
    val encryptor: MessageDigest = MessageDigest.getInstance(algorithm)
    val hashValue: Array[Byte] = encryptor.digest(input.getBytes("UTF-8"))
    val identifier = byteArrayToIntValue(hashValue)

    identifier
  }

  def byteArrayToIntValue(bytes: Array[Byte]): Int = {
    var sb: StringBuilder = new StringBuilder
    for (i <- 0 to 0) {
      sb = sb.append(String.format("%8s", Integer.toBinaryString(bytes(i) & 0xFF)).replace(' ', '0'))
    }

    Integer.parseInt(sb.toString(), 2)
  }


  def rangeValidator(leftInclude: Boolean, leftValue: BigInt, rightValue: BigInt, rightInclude: Boolean, value: BigInt): Boolean = {
    if (leftValue == rightValue) {
      true
    } else if (leftValue < rightValue) {
      if (value == leftValue && leftInclude || value == rightValue && rightInclude || (value > leftValue && value < rightValue)) {
        true
      } else {
        false
      }
    } else {
      if (value == leftValue && leftInclude || value == rightValue && rightInclude || (value > leftValue || value < rightValue)) {
        true
      } else {
        false
      }
    }
  }


}
