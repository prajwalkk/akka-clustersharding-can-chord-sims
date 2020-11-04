package com.chord.akka.utils

import java.security.MessageDigest

object HashUtil {
  def Hash(key:String)={
    MessageDigest.getInstance("MD5").digest(key.getBytes("UTF-8")).map("%02X".format(_)).mkString
  }

}
