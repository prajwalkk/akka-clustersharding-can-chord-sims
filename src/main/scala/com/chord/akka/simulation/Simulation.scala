package com.chord.akka.simulation


import akka.actor.ActorSelection
import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akka.actors.NodeActor.createNodes
import com.chord.akka.actors.UserActor.{lookup_data, put_data}
import com.chord.akka.actors.UserGroup.{UserList, createUser}
import com.chord.akka.actors.{NodeActor, UserActor, UserGroup}
import com.chord.akka.utils.SystemConstants
import com.chord.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
//import com.chord.akka.webserver.HttpServer
//
import scala.util.Random


object Simulation extends LazyLogging {
  def lookup_data_randomly(key :String):ActorRef[UserActor.Command]  ={

     implicit val timeout = Timeout.create(userActorSystem.settings.config.getDuration("my-app.routes.ask-timeout"))
    val r = Random.between(0,UserList.length)
    val actor = Await.result(userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)).resolveOne(),timeout.duration)

    return actor.toTyped[UserActor.Command]
  }
  def load_data_randomly(key :String,value:String): Unit ={
    val r = Random.between(0,UserList.length)
    userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)) ! put_data(key,value)
  }

  val chordActorSystem: ActorSystem[NodeActor.Command] = ActorSystem(NodeActor("ChordActorSystem"), "ChordActorSystem")
  chordActorSystem ! createNodes(SystemConstants.num_nodes)

  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(),"UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(1000)
  HttpServer.setupServer()
  lookup_data_randomly("Hello")

}



