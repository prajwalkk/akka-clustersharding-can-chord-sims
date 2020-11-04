package com.chord.akka.simulation


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import com.chord.akka.actors.UserActor
import com.chord.akka.actors.NodeActor
import com.chord.akka.actors.NodeActor.addNodesToChordRing
import com.chord.akka.actors.UserActor.createUsers



object Simulation extends LazyLogging {


  val config: Config = ConfigFactory.load("Simulator.conf")
  val num_nodes: Int = config.getInt("NUM_NODES")
  val num_users: Int = config.getInt("NUM_USERS")
  logger.info("Creating Chord Actor System")
  val chordActorSystem: ActorSystem[NodeActor.Command] = ActorSystem(NodeActor(), "ChordActorSystem")
  chordActorSystem ! addNodesToChordRing(num_nodes)
  logger.info("Creating User Actor System")
  val userActorSystem: ActorSystem[UserActor.Command] = ActorSystem(UserActor(), "UserActorSystem")
  userActorSystem ! createUsers(num_users)





}
