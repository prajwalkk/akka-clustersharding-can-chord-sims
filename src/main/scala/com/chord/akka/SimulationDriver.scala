package com.chord.akka


import com.chord.akka.actors.NodeGroup.{SaveAllSnapshot, SaveDataSnapshot}
import com.chord.akka.simulation.Simulation
import com.chord.akka.simulation.Simulation.{generate_random_lookuprequest, generate_random_putrequest, initialize_chord, nodeActorSystem}
import com.chord.akka.utils.{DataUtils, SystemConstants}
import com.chord.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt


object SimulationDriver extends LazyLogging{


  def main(args: Array[String]): Unit = {
    logger.info("********************* Simulation Start **********************")
    val simulation = Simulation
    Thread.sleep(20000)

    val snapshot = new Runnable {
      def run() {
        logger.info("Getting snapshots")
        nodeActorSystem ! SaveAllSnapshot
      }
    }
    simulation.nodeActorSystem.scheduler.scheduleAtFixedRate(5.seconds,15.seconds)(snapshot)(nodeActorSystem.executionContext)
    Thread.sleep(1000)
    HttpServer.setupServer()
    Thread.sleep(20000)
    val data: List[(String, String)] = DataUtils.read_data()
    val init_length: Int = (data.length * SystemConstants.INIT_PERCENT).toInt
    val(init_data,test_data) = data.splitAt(init_length)
    val keysInserted = init_data.map(i=>i._1)
    val testkeys= test_data.map(i=>i._1)

    logger.info(s"Inserting $init_length records")
    var init_complete = false
    init_complete=initialize_chord(init_data)
    if(init_complete){
      logger.info("Starting look ups on initialized data")
      generate_random_lookuprequest(keysInserted)
    }

    Thread.sleep(10000)
    logger.info("Simulating User inserting data")
    if(generate_random_putrequest(test_data)){
      logger.info("Simulating data look ups on test data")
      generate_random_lookuprequest(testkeys)
    }
    Thread.sleep(10000)
    logger.info("Getting Data Snapshot")
    nodeActorSystem ! SaveDataSnapshot
    logger.info("Terminating Simulation")
    Thread.sleep(10000)
    simulation.userActorSystem.terminate()
    simulation.nodeActorSystem.terminate()
    logger.info("SnapShots are saved in Yaml File")
    logger.info("********************* Simulation End **********************")
    System.exit(0)




  }


}
