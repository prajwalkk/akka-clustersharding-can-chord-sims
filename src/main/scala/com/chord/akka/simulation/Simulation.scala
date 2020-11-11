package com.chord.akka.simulation

import com.chord.akka.utils.DataUtils
import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akka.actors.NodeActor.addValue
import com.chord.akka.actors.NodeGroup.createNodes
import com.chord.akka.actors.UserActor.{lookup_data, put_data}
import com.chord.akka.actors.UserGroup.{UserList, createUser}
import com.chord.akka.actors.{LookupObject, NodeActor, NodeGroup, UserActor, UserGroup}
import com.chord.akka.utils.SystemConstants
import com.chord.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
//import com.chord.akka.webserver.HttpServer
//
import scala.util.Random


object Simulation extends LazyLogging {
  def select_random_user(): ActorRef[UserActor.Command] ={
    implicit val timeout: Timeout = Timeout.create(userActorSystem.settings.config.getDuration("my-app.routes.ask-timeout"))
    val r = (0+SystemConstants.random_user.nextInt(SystemConstants.num_users))
    val actor = Await.result(userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)).resolveOne(),timeout.duration)

    actor.toTyped[UserActor.Command]
  }
  def generate_random_request()={
    val user = select_random_user()
    user ! lookup_data("test1")
  }


  val chordActorSystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "ChordActorSystem")
  chordActorSystem ! createNodes(SystemConstants.num_nodes)
  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(),"UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(1000)
  HttpServer.setupServer()
  Thread.sleep(2000)
  generate_random_request()
  val data = DataUtils.read_data()




}



