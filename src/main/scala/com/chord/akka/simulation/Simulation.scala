package com.chord.akka.simulation


import akka.actor.typed.ActorSystem
import com.chord.akka.actors.NodeActor.createNodes
import com.chord.akka.actors.UserGroup.createUser
import com.chord.akka.actors.{NodeActor,  UserGroup}
import com.chord.akka.utils.SystemConstants
import com.typesafe.scalalogging.LazyLogging


object Simulation extends LazyLogging {


  logger.info("Creating Chord Actor System")
  val chordActorSystem: ActorSystem[NodeActor.Command] = ActorSystem(NodeActor("ChordActorSystem"), "ChordActorSystem")
  chordActorSystem ! createNodes(SystemConstants.num_nodes)
  logger.info("Creating User Actor System")
  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(),"UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)





}
