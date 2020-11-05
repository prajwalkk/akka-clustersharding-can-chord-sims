package com.chord.akka.utils

import java.security.MessageDigest
import java.util.UUID

object Helper {


//  def main(args: Array[String]): Unit = {
//
//    getIdentifier(generateRandomName());
//
//  }


  def getIdentifier(input: String, algorithm: String = "SHA1"): Int = {
    val encryptor: MessageDigest = MessageDigest.getInstance(algorithm)
    val hashValue: Array[Byte] = encryptor.digest(input.getBytes("UTF-8"));
    val identifier = byteArrayToIntValue(hashValue)

    identifier
  }

  def byteArrayToIntValue(bytes: Array[Byte]): Int = {
    var sb: StringBuilder = new StringBuilder
    for (i <- 0 to 2) {
      sb = sb.append(String.format("%8s", Integer.toBinaryString(bytes(i) & 0xFF)).replace(' ', '0'))
    }
    println(sb.toString())
    return Integer.parseInt(sb.toString(), 2)
  }

  def generateRandomName(): String = {
    val generatedName: String = UUID.randomUUID().toString

    return generatedName;
  }

}
