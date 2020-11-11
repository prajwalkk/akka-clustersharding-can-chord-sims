package com.chord.akka.simulation


import akka.actor.typed.ActorSystem
import com.chord.akka.actors.NodeGroup.createNodes
import com.chord.akka.actors.UserActor.get_data
import com.chord.akka.actors.UserGroup.{UserList, createUser}
import com.chord.akka.actors.{NodeGroup, UserGroup}
import com.chord.akka.utils.SystemConstants
import com.chord.akka.webserver.HttpServer
import scala.util.Random


object Simulation  {
  def lookup_data_randomly(key :String): Unit ={
    val r = Random.between(0,UserList.length)
    userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)) ! get_data(key)
  }


  val chordActorSystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "ChordActorSystem")
  chordActorSystem ! createNodes(SystemConstants.num_nodes)

  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(),"UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(1000)
  HttpServer.setupServer()
  lookup_data_randomly("Hello")

}



