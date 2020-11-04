package com.chord.akka.simulation


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

object Simulation extends LazyLogging {

  sealed trait Command

  final case class addNodesToChordRing(num_nodes: Int) extends Command

  final case class createUsers(num_users: Int) extends Command


  val config: Config = ConfigFactory.load("Simulator.conf")
  val num_nodes: Int = config.getInt("NUM_NODES")
  val num_users: Int = config.getInt("NUM_USERS")
  logger.info("Creating Chord Actor System")
  val chordActorSystem: ActorSystem[Simulation.addNodesToChordRing] = ActorSystem(Simulation(), "ChordActorSystem")
  chordActorSystem ! addNodesToChordRing(num_nodes)
  logger.info("Creating User Actor System")
  val userActorSystem: ActorSystem[Simulation.createUsers] = ActorSystem(Simulation(), "UserActorSystem")
  userActorSystem ! createUsers(num_users)


  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      logger.info(context.toString)
      Behaviors.receiveMessage {
        case addNodesToChordRing(n: Int) =>
          logger.info(n.toString)
          //create ChordNodes
          Behaviors.same
        case createUsers(n: Int) =>
          logger.info(n.toString)
          //create UserNodes
          Behaviors.same
      }
    }


}
