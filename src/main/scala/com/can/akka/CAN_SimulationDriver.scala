package com.can.akka

import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import com.can.akka.actors.NodeGroup
import com.can.akka.actors.NodeActor.LeaveNode
import com.can.akka.actors.NodeGroup.{CreateNode, NodeList}
import com.can.akka.actors.User.{createUser, lookup_data, put_data}
import com.can.akka.simulation.Simulation
import com.can.akka.simulation.Simulation._
import com.can.akka.utils.{DataUtils, SystemConstants}
import com.can.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt
import scala.util.Random

object CAN_SimulationDriver extends LazyLogging {


  def main(args: Array[String]): Unit = {
    logger.info("********************* Simulation Start **********************")

    val data: List[(String, String)] = DataUtils.read_data()
    val init_length: Int = (data.length * SystemConstants.INIT_PERCENT).toInt
    var (init_data, test_data) = data.splitAt(init_length)
    val keysInserted = init_data.map(i => i._1)

    val simulation = Simulation

    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val scheduler: Scheduler = nodesystem.scheduler


    for (i <- 0 until SystemConstants.num_nodes) {

      (nodesystem.ask[NodeGroup.ActionSuccessful](CreateNode(_,i)), timeout.duration)
      Thread.sleep(100)
    }
    Thread.sleep(10000)
    logger.info("Initial Node Creation Finished")
    userSystem ! createUser()



    var operation_running = false
    var nodes_left = 0
    var nodes_joined = 0

    val datalookup = new Runnable {
      override def run(): Unit = {
        val user = select_random_user()
        val key = keysInserted(new Random().nextInt(keysInserted.length))
        user ! lookup_data(key)
      }
    }

    val datainsert = new Runnable {
      override def run(): Unit = {
        if (test_data.nonEmpty) {
          val user = select_random_user()
          val data = test_data(new Random().nextInt(test_data.length))
          test_data = test_data.filter(_ != data)
          user ! put_data(data._1, data._2)
          keysInserted.appended(data._1)
        }
        else {
          logger.info(s"Data exhausted ,Cannot write anymore")
        }
      }
    }

    val nodeLeave = new Runnable {
      def run() {
        if (nodes_left <= SystemConstants.maxNodeRemoval && !operation_running) {
          operation_running = true
          val node = select_random_node()
          node ! LeaveNode()
          nodes_left = nodes_left + 1
          operation_running = false
        }
        else if (operation_running) {
          logger.info("An Operation in already running will try  again in next Interval")
        }
        else {
          logger.info("Reached maximum limit for simulation node leave , try increasing the max_node_removals in configuration")
        }
      }
    }
    val SimulationEnd = new Runnable {
      def run() {
        logger.info("Terminating Simulation")
        simulation.userSystem.terminate()
        simulation.nodesystem.terminate()
        logger.info("********************* Simulation End **********************")
        System.exit(0)

      }
    }
    val nodeJoin = new Runnable {
      def run() {
        implicit val timeout: Timeout = Timeout(20.seconds)
        implicit val scheduler: Scheduler = nodesystem.scheduler
        if (nodes_joined <= SystemConstants.maxNodeJoin && !operation_running) {
          operation_running = true
          val future = nodesystem.ask[NodeGroup.ActionSuccessful](CreateNode(_,NodeList.size+1))
          nodes_joined = nodes_joined + 1
          operation_running = false
        }
        else if (operation_running) {
          logger.info("An Operation in already running will try  again in next Interval")
        }
        else {
          logger.info("Reached maximum limit for simulation node joins , try increasing the max_node_addition in configuration")
        }
      }
    }

    Thread.sleep(5000)
    HttpServer.setupServer(nodesystem)
    Thread.sleep(1000)

    //initializing CAN with some data and looking up that data
    logger.info(s"Inserting $init_length records")
    var init_complete = false
    init_complete = initialize_can(init_data)

    //schedule data lookup
    simulation.nodesystem.scheduler.scheduleWithFixedDelay(20.seconds, SystemConstants.data_read_write_rate.seconds)(datalookup)(nodesystem.executionContext)

    //schedule data insert
    simulation.nodesystem.scheduler.scheduleWithFixedDelay(25.seconds, SystemConstants.data_read_write_rate.seconds)(datainsert)(nodesystem.executionContext)

    //Schedule node joins
    simulation.nodesystem.scheduler.scheduleWithFixedDelay(40.seconds, SystemConstants.nodeJoinInterval.seconds)(nodeJoin)(nodesystem.executionContext)

    //Schedule nodeLeaving every 10 seconds
    simulation.nodesystem.scheduler.scheduleWithFixedDelay(30.seconds, SystemConstants.nodeLeaveInterval.seconds)(nodeLeave)(nodesystem.executionContext)

    //Simulation End
    simulation.nodesystem.scheduler.scheduleOnce(SystemConstants.simulationEnd.seconds, SimulationEnd)(nodesystem.executionContext)
  }

}
