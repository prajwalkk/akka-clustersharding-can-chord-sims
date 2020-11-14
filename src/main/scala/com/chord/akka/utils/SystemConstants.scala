package com.chord.akka.utils

import com.typesafe.config.{Config, ConfigFactory}

object SystemConstants {
  val config: Config = ConfigFactory.load("Simulator.conf")
  val num_nodes: Int = config.getInt("NUM_NODES")
  val num_users: Int = config.getInt("NUM_USERS")

  val M:Int = 8
}
