package com.chord.akka.simulation

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akka.actors.NodeGroup.{CreateNodes, SaveAllSnapshot, SaveDataSnapshot}
import com.chord.akka.actors.UserActor.{lookup_data, put_data}
import com.chord.akka.actors.UserGroup.createUser
import com.chord.akka.actors.{NodeActor, NodeGroup, UserActor, UserGroup}
import com.chord.akka.utils.{DataUtils, SystemConstants}
import com.chord.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


object Simulation extends LazyLogging {

  val nodeActorSystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "NodeActorSystem")
  nodeActorSystem ! CreateNodes(SystemConstants.num_nodes)
  Thread.sleep(30000)
  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(), "UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)
  Thread.sleep(20000)

  nodeActorSystem ! SaveAllSnapshot
  Thread.sleep(1000)
  HttpServer.setupServer()
  Thread.sleep(2000)
  val data: List[(String, String)] = DataUtils.read_data()
  val keys = data.map(i=>i._1)
  val init_length: Int = (data.length * 0.5).toInt
  val keysInserted = keys.take(init_length)


  logger.info(s"Inserting $init_length records")
  var init_complete = false
  init_complete=initialize_chord(data.take(init_length))
  if(init_complete){
    logger.info("Starting lookups")
    generate_random_request(keysInserted)

  }

  //val data_remaining: List[(String, String)] = data.drop(init_length)
  //

  def select_random_node(): ActorRef[NodeActor.Command] = {
    implicit val timeout: Timeout = Timeout(10.seconds)
    val r = 0 + SystemConstants.random_user.nextInt(SystemConstants.num_nodes)
    val actor = Await.result(nodeActorSystem.classicSystem.actorSelection(NodeGroup.NodeList(r)).resolveOne(), timeout.duration)

    actor.toTyped[NodeActor.Command]
  }
  def select_random_user(): ActorRef[UserActor.Command] = {
    implicit val timeout: Timeout = Timeout.create(userActorSystem.settings.config.getDuration("my-app.routes.ask-timeout"))
    val r = 0 + SystemConstants.random_user.nextInt(SystemConstants.num_users)
    val actor = Await.result(userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)).resolveOne(), timeout.duration)

    actor.toTyped[UserActor.Command]
  }
  def generate_random_request(datakeys: List[String]): Unit = {
    logger.info("Read Requests started")
    implicit val timeout = Timeout(10.seconds)
    implicit val scheduler = userActorSystem.scheduler
    val user = select_random_user()
    for(key <- datakeys){
      user ! lookup_data(key)
      Thread.sleep(10)
    }
  }
  def initialize_chord(initialData: List[(String, String)]): Boolean = {
    logger.info("Initializing Chord data")
    implicit val timeout = Timeout(10.seconds)
    implicit val scheduler = userActorSystem.scheduler
    val user = select_random_user()
    for ((k, v) <- initialData) {
      user ! put_data(k,v)
      Thread.sleep(10)

    }
    logger.info("Finished Init data")
    true
  }

  def getDataDump: Unit = {
    Thread.sleep(25000)
    logger.info("Getting Data Dump")
    nodeActorSystem ! SaveDataSnapshot
    Thread.sleep(20000)
    logger.info("Graceful Shutdown")
  }

}



