package com.chord.akkasharding.simulation

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import akka.management.scaladsl.AkkaManagement
import akka.util.Timeout
import com.chord.akkasharding.actors.NodeGroup.{CreateNodes, SaveAllSnapshot, SaveDataSnapshot}
import com.chord.akkasharding.actors.UserActor.{lookup_data, put_data}
import com.chord.akkasharding.actors.UserGroup.createUser
import com.chord.akkasharding.actors.{NodeActor, NodeGroup, UserActor, UserGroup}
import com.chord.akkasharding.utils.{DataUtils, SystemConstants}
import com.chord.akkasharding.webserver.HttpServer
//import com.chord.akka.webserver.HttpServer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


object Simulation extends LazyLogging {

  val nodeActorSystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "NodeActorSystem")
  //added
  AkkaManagement(nodeActorSystem).start()

  val sharding: ClusterSharding = ClusterSharding(nodeActorSystem)

  val typeKey: EntityTypeKey[NodeActor.Command] = EntityTypeKey[NodeActor.Command]("NodeActor")
  val shardRegion: ActorRef[ShardingEnvelope[NodeActor.Command]] = {
    logger.info("Sharding INIT")
    sharding.init(Entity(typeKey)(createBehavior = entityContext => NodeActor(entityContext.entityId, sharding, typeKey)))
  }

  var num_nodes: Int = SystemConstants.num_nodes

  if(SystemConstants.num_nodes > Math.pow(2,SystemConstants.M)){
    logger.warn("Number of Nodes in the ring cannot exceed 256,changing number of Nodes to 256")
    num_nodes = 256
  }
  //added
  nodeActorSystem ! CreateNodes(num_nodes,sharding,typeKey)
  Thread.sleep(30000)
  //val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(), "UserActorSystem")
  //userActorSystem ! createUser(SystemConstants.num_users)
  HttpServer.setupServer(typeKey,sharding,nodeActorSystem)
  def select_random_node(): EntityRef[NodeActor.Command] = {
    implicit val timeout: Timeout = Timeout(10.seconds)
    val r = 0 + SystemConstants.random_user.nextInt(SystemConstants.num_nodes)
    //val actor = Await.result(nodeActorSystem.classicSystem.actorSelection(NodeGroup.NodeList(r)).resolveOne(), timeout.duration)
    val actor = NodeGroup.NodeList(r)
    val entityRef: EntityRef[NodeActor.Command] = sharding.entityRefFor(typeKey, actor)
    //actor.toTyped[NodeActor.Command]
    entityRef
  }
  /*def select_random_user(): ActorRef[UserActor.Command] = {
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
  }*/

}



