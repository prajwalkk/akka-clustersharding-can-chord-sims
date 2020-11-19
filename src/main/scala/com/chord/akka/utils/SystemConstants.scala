package com.chord.akka.utils

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Random

object SystemConstants {
  val config: Config = ConfigFactory.load("Simulator.conf")
  val num_nodes: Int = config.getInt("NUM_NODES")
  val num_users: Int = config.getInt("NUM_USERS")
  val M:Int = config.getInt("NUM_BITS")
  val INIT_PERCENT:Double = config.getDouble("INIT_PERCENT")
  val SnapShot_Interval:Int = config.getInt("SnapShot_Interval")
  val random_user = new Random()

}
