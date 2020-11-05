package com.chord.akka.simulation


import akka.actor.typed.ActorSystem
import com.chord.akka.actors.NodeActor.createNodes
import com.chord.akka.actors.UserActor.lookup_data
import com.chord.akka.actors.UserGroup.{UserList, createUser}
import com.chord.akka.actors.{NodeActor, UserGroup}
import com.chord.akka.utils.SystemConstants

import scala.util.Random


object Simulation  {
  def lookup_data_randomly(key :String): Unit ={
    val r = Random.between(0,UserList.length)
    userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)) ! lookup_data(key)
  }


  val chordActorSystem: ActorSystem[NodeActor.Command] = ActorSystem(NodeActor("ChordActorSystem"), "ChordActorSystem")
  chordActorSystem ! createNodes(SystemConstants.num_nodes)

  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(),"UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(1000)

  lookup_data_randomly("Hello")

}



