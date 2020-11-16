package com.chord.akka.simulation

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akka.actors.NodeGroup.CreateNodes
import com.chord.akka.actors.UserActor.{lookup_data, put_data}
import com.chord.akka.actors.UserGroup.createUser
import com.chord.akka.actors.{NodeActorTest, NodeGroup, UserActor, UserGroup}
import com.chord.akka.utils.{DataUtils, SystemConstants}
//import com.chord.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await


object Simulation extends LazyLogging {

  def select_random_node(): ActorRef[NodeActorTest.Command] = {
    implicit val timeout: Timeout = Timeout.create(nodeActorSystem.settings.config.getDuration("my-app.routes.ask-timeout"))
    val r = 0 + SystemConstants.random_user.nextInt(SystemConstants.num_nodes)
    val actor = Await.result(nodeActorSystem.classicSystem.actorSelection(NodeGroup.NodeList(r)).resolveOne(), timeout.duration)

    actor.toTyped[NodeActorTest.Command]
  }
  def select_random_user(): ActorRef[UserActor.Command] = {
    implicit val timeout: Timeout = Timeout.create(userActorSystem.settings.config.getDuration("my-app.routes.ask-timeout"))
    val r = 0 + SystemConstants.random_user.nextInt(SystemConstants.num_users)
    val actor = Await.result(userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)).resolveOne(), timeout.duration)

    actor.toTyped[UserActor.Command]
  }
  def generate_random_request(data: List[(String, String)]): Unit = {
    val user = select_random_user()
    user ! lookup_data("test1")
  }
  def initialize_chord(initialData: List[(String, String)]): Unit = {
    logger.info("Initializing Chord data")
    val user = select_random_user()

    for ((k, v) <- initialData) {
      user ! put_data(k, v)
      Thread.sleep(100)
    }


  }


  val nodeActorSystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "ChordActorSystem")
  nodeActorSystem ! CreateNodes(SystemConstants.num_nodes)


  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(), "UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(1000)



  // Generating Request and Initializing Chord
    Thread.sleep(1000)
//    HttpServer.setupServer()
    Thread.sleep(2000)
    val data: List[(String, String)] = DataUtils.read_data()
    val init_length: Int = (data.length * 0.01).toInt
  //
   initialize_chord(data.take(init_length))
  //val data_remaining: List[(String, String)] = data.drop(init_length)
  //generate_random_request(data_remaining)


}



