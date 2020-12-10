package com.chord.akkasharding


import akka.actor.typed.ActorSystem
import com.chord.akkasharding.actors.NodeGroup.{SaveAllSnapshot, SaveDataSnapshot}
import com.chord.akkasharding.actors.UserGroup
import com.chord.akkasharding.simulation.Simulation
//import com.chord.akka.simulation.Simulation.{generate_random_lookuprequest, generate_random_putrequest, initialize_chord, nodeActorSystem}
import com.chord.akkasharding.simulation.Simulation.nodeActorSystem
import com.chord.akkasharding.utils.{DataUtils, SystemConstants}
import com.chord.akkasharding.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt


object SimulationDriver extends LazyLogging{


  def main(args: Array[String]): Unit = {
    logger.info("********************* Simulation Start **********************")
    val simulation = Simulation

    logger.info("Trying to start HTTP Server")
    Thread.sleep(20000)


    val snapshot = new Runnable {
      def run() {
        logger.info("Getting snapshots")
        nodeActorSystem ! SaveAllSnapshot
      }
    }
    simulation.nodeActorSystem.scheduler.scheduleAtFixedRate(5.seconds,SystemConstants.SnapShot_Interval.seconds)(snapshot)(nodeActorSystem.executionContext)
    Thread.sleep(10000)
    logger.info("Getting Data Snapshot")
    nodeActorSystem ! SaveDataSnapshot
    logger.info("Terminating Simulation")
    Thread.sleep(10000)
    //simulation.nodeActorSystem.terminate()
    logger.info("SnapShots are saved in Yaml File")

    logger.info("********************* Simulation End **********************")
    //System.exit(0)




  }


}
