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
  var num_nodes = SystemConstants.num_nodes
  if(SystemConstants.num_nodes > num_nodes){
    logger.error("Number of Nodes in the ring cannot exceed 256,changing number of Nodes to 256")
    num_nodes = 256
  }
  nodeActorSystem ! CreateNodes(num_nodes)
  Thread.sleep(30000)
  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(), "UserActorSystem")
  userActorSystem ! createUser(SystemConstants.num_users)

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
  def generate_random_lookuprequest(datakeys: List[String]): Unit = {
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
  def generate_random_putrequest(test_data:List[(String,String)]):Boolean={
    implicit val timeout = Timeout(10.seconds)
    implicit val scheduler = userActorSystem.scheduler
    val user = select_random_user()
    for ((k, v) <- test_data) {
      user ! put_data(k,v)
      Thread.sleep(10)
    }
    logger.info("Simulated User put requests")
true
  }

  def getDataDump: Unit = {
    //Thread.sleep(25000)
    logger.info("Getting Data Dump")
    nodeActorSystem ! SaveDataSnapshot
    //Thread.sleep(20000)
    logger.info("Graceful Shutdown")
  }

}



