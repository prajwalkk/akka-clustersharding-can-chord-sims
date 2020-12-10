package com.can.akka.utils

import java.math.BigInteger
import java.security.MessageDigest
import java.util.UUID

object Helper {

  def getRandomCoordinate():Double=
    (Math.random()*(SystemConstants.X2))+SystemConstants.X1


  def getX(input: String):Double =
    getHashValue(input,"SHA1")

  def getY(input:String):Double =
    getHashValue(input,"MD5")

  def generateRandomName(): String = {
    val generatedName: String = UUID.randomUUID().toString

    return generatedName;
  }

  def getHashValue(input: String, algorithm: String = "SHA1"): Double = {
    val encryptor: MessageDigest = MessageDigest.getInstance(algorithm)
    val hashValue: Array[Byte] = encryptor.digest(input.getBytes())

    val identifier = byteArrayToIntDouble(hashValue)%(SystemConstants.X2-SystemConstants.X1+1)

    identifier
  }

  def byteArrayToIntDouble(bytes: Array[Byte]): Double = {
    var sb: StringBuilder = new StringBuilder
    for (i <- 0 to 2) {
      sb = sb.append(String.format("%8s", Integer.toBinaryString(bytes(i) & 0xFF)).replace(' ', '0'))
    }

    return Integer.parseInt(sb.toString(),2)/1000
  }

}
