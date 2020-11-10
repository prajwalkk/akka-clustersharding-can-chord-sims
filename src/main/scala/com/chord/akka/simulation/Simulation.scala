package com.chord.akka.simulation


import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akka.actors.NodeGroup.createNodes
import com.chord.akka.actors.UserActor.put_data
import com.chord.akka.actors.UserGroup.{UserList, createUser}
import com.chord.akka.actors.{NodeActor, NodeGroup, UserActor, UserGroup}
import com.chord.akka.utils.SystemConstants
import com.chord.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
//import com.chord.akka.webserver.HttpServer
//
import scala.util.Random


object Simulation extends LazyLogging {
  def select_random_user(key :String):ActorRef[UserActor.Command]  ={
    implicit val timeout: Timeout = Timeout.create(userActorSystem.settings.config.getDuration("my-app.routes.ask-timeout"))
    val r = (0+SystemConstants.random_user.nextInt(SystemConstants.num_users))
    val actor = Await.result(userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)).resolveOne(),timeout.duration)
    actor.toTyped[UserActor.Command]
  }


  val chordActorSystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "ChordActorSystem")
  chordActorSystem ! createNodes(SystemConstants.num_nodes)

  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(),"UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(1000)
  HttpServer.setupServer()



}



