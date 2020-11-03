package com.chord.akka.simulation


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

object Simulation extends LazyLogging {

  final case class addNodesToChordRing(num_nodes:Int)


  val config: Config = ConfigFactory.load("Simulator.conf")
  val num_nodes: Int = config.getInt("NUM_NODES")
  logger.info("Creating Chord Actor System")
  val chordActorSystem :ActorSystem[Simulation.addNodesToChordRing]=  ActorSystem(Simulation(),"ChordActorSystem")

  chordActorSystem ! addNodesToChordRing(num_nodes)

  def apply(): Behavior[addNodesToChordRing] =
    Behaviors.setup { _ =>
      Behaviors.receiveMessage { message =>
        logger.info(message.num_nodes.toString)
        //create ChordNodes
        Behaviors.same
      }
    }



}
