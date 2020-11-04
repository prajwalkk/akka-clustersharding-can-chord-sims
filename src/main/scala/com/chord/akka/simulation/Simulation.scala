package com.chord.akka.simulation

import com.chord.akka.utils.SystemConstants
import akka.actor.typed.ActorSystem
import com.chord.akka.actors
import com.chord.akka.actors.NodeActor.createNodes
import com.chord.akka.actors.UserActor.createUsers
import com.chord.akka.actors.UserGroup.UsersCreated
import com.chord.akka.actors.{NodeActor, UserActor, UserGroup}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{Await, ExecutionContext, Future}

object Simulation extends LazyLogging {


  logger.info("Creating Chord Actor System")
  val chordActorSystem: ActorSystem[NodeActor.Command] = ActorSystem(NodeActor("ChordActorSystem"), "ChordActorSystem")
  chordActorSystem ! createNodes(SystemConstants.num_nodes)
  logger.info("Creating User Actor System")
  val userActorSystem: ActorSystem[UserActor.Command] = ActorSystem(UserActor("UserActorSystem"), "UserActorSystem")
  val userGroup: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(), "UserGroup")
  userActorSystem ! createUsers(SystemConstants.num_users,userGroup.ref)
  Thread.sleep(1000)
  logger.info(UserGroup.userList.toSeq.toString())



}
