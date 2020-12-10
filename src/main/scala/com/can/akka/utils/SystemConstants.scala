package com.can.akka.utils

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Random

object SystemConstants {

  val config: Config = ConfigFactory.load("ContentAddressableNetwork.conf")
  val X1: Int = config.getInt("x1")
  val Y1: Int = config.getInt("y1")
  val X2: Int = config.getInt("x2")
  val Y2: Int = config.getInt("y2")
  val random_user = new Random()

  val num_nodes = config.getInt("num_nodes")

  val INIT_PERCENT:Double = config.getDouble("INIT_PERCENT")

  val SnapShot_Interval:Int = config.getInt("SnapShot_Interval")

  val nodeLeaveInterval:Int = config.getInt("node_leaving_rate")
  val maxNodeRemoval:Int = config.getInt("max_node_removals")

  val nodeJoinInterval:Int = config.getInt("node_adding_rate")
  val maxNodeJoin:Int = config.getInt("max_node_addition")
  val data_read_write_rate:Int = config.getInt("data_read_write_rate")

  val simulationEnd:Int = config.getInt("END")





}
