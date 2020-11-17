package com.chord.akka.utils

import java.security.MessageDigest
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging

object Helper extends LazyLogging{



  def getIdentifier(input: String, algorithm: String = "SHA1"): Int = {
    val encryptor: MessageDigest = MessageDigest.getInstance(algorithm)
    val hashValue: Array[Byte] = encryptor.digest(input.getBytes("UTF-8"))
    var identifier = byteArrayToIntValue(hashValue)
    identifier
  }

  def byteArrayToIntValue(bytes: Array[Byte]): Int = {
    var sb: StringBuilder = new StringBuilder
    //TODO change to 2 later
    for (i <- 0 to 0) {
      sb = sb.append(String.format("%8s", Integer.toBinaryString(bytes(i) & 0xFF)).replace(' ', '0'))
    }

    Integer.parseInt(sb.toString(), 2)
  }

  def generateRandomName(): String = {
    val generatedName: String = UUID.randomUUID().toString

    generatedName
  }

}
