package com.chord.akka.simulation


import akka.actor.typed.ActorSystem
import com.chord.akka.actors.NodeActor.addNodesToChordRing
import com.chord.akka.actors.UserActor.createUsers
import com.chord.akka.actors.{NodeActor, UserActor}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging


object Simulation extends LazyLogging {

  val config: Config = ConfigFactory.load("Simulator.conf")
  val num_nodes: Int = config.getInt("NUM_NODES")
  val num_users: Int = config.getInt("NUM_USERS")
  logger.info("Creating Chord Actor System")
  val chordActorSystem: ActorSystem[NodeActor.Command] = ActorSystem(NodeActor("ChordActorSystem"), "ChordActorSystem")
  chordActorSystem ! addNodesToChordRing(num_nodes)
  logger.info("Creating User Actor System")
  val userActorSystem: ActorSystem[UserActor.Command] = ActorSystem(UserActor("UserActorSystem"), "UserActorSystem")
  userActorSystem ! createUsers(num_users)


}
