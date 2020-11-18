package com.chord.akka.simulation

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akka.actors.NodeActorTest.SaveNodeSnapshot
import com.chord.akka.actors.NodeGroup.{CreateNodes, createdNodes}
import com.chord.akka.actors.UserActor.{lookup_data, put_data}
import com.chord.akka.actors.UserGroup.createUser
import com.chord.akka.actors.{NodeActorTest, NodeGroup, UserActor, UserGroup}
import com.chord.akka.utils.Helper.logger
import com.chord.akka.utils.{DataUtils, SystemConstants}
import com.chord.akka.webserver.HttpServer
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
  def generate_random_request(datakeys: List[String]): Unit = {
    logger.info("Read Requests started")
    val user = select_random_user()
    for(key <- datakeys){
      user ! lookup_data(key)
      Thread.sleep(10)
    }

  }
  def initialize_chord(initialData: List[(String, String)]): Boolean = {
    logger.info("Initializing Chord data")

    val user = select_random_user()
    for ((k, v) <- initialData) {
      user ! put_data(k, v)
      Thread.sleep(10)
    }
  logger.info("Finished Init data")
    true
  }


  val nodeActorSystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "ChordActorSystem")

  nodeActorSystem ! CreateNodes(SystemConstants.num_nodes)
  Thread.sleep(30000)

  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(), "UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(1000)
  Thread.sleep(20000)

  createdNodes.foreach(i => i ! SaveNodeSnapshot(i.asInstanceOf[ActorRef[NodeActorTest.Command]]))


  // Generating Request and Initializing Chord
    Thread.sleep(1000)
    HttpServer.setupServer()
    Thread.sleep(2000)
    val data: List[(String, String)] = DataUtils.read_data()
    val keys = data.map(i=>i._1)

    val init_length: Int = (data.length * 0.01).toInt

    val keysInserted = keys.take(init_length)




  val init_complete=initialize_chord(data.take(init_length))
  if(init_complete){
    logger.info("Starting lookups")
  generate_random_request(keysInserted)}

  //val data_remaining: List[(String, String)] = data.drop(init_length)
  //


}



