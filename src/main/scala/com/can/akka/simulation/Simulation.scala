package com.can.akka.simulation

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import akka.management.scaladsl.AkkaManagement
import akka.util.Timeout
import com.can.akka.actors.NodeGroup.NodeList
import com.can.akka.actors.User._
import com.can.akka.actors.{NodeActor, NodeGroup, User}
import com.can.akka.utils.SystemConstants
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Simulation extends LazyLogging {
  val nodesystem: ActorSystem[NodeGroup.Command] = ActorSystem(NodeGroup(), "NodeActorSystem")

  AkkaManagement(nodesystem).start()
  val sharding = ClusterSharding(nodesystem)
  val typekey = EntityTypeKey[NodeActor.Command]("NodeActor")
  val shardRegion: ActorRef[ShardingEnvelope[NodeActor.Command]] =
    sharding.init(Entity(typekey)(createBehavior = entityContext => NodeActor(entityContext.entityId)))


  val config = ConfigFactory.load("client.conf")
  val userSystem: ActorSystem[Command] = ActorSystem(User("UserSystem"), "UserSystem", config)


  def select_random_node(): EntityRef[NodeActor.Command] = {
    implicit val timeout: Timeout = Timeout(10.seconds)
    val r = 0 + SystemConstants.random_user.nextInt(NodeList.size)
    sharding.entityRefFor(typekey, NodeList(r))

  }

  def initialize_can(initialData: List[(String, String)]): Boolean = {
    logger.info("Initializing CAN data")
    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val scheduler: Scheduler = userSystem.scheduler
    val user = select_random_user()
    for ((k, v) <- initialData) {
      user ! put_data(k, v)
      Thread.sleep(1000)
    }
    logger.info("Finished Init data")
    true
  }

  def select_random_user(): ActorRef[User.Command] = {
    implicit val timeout: Timeout = Timeout.create(userSystem.settings.config.getDuration("my-app.routes.ask-timeout"))
    val r = 0 + SystemConstants.random_user.nextInt(userList.size)
    val actor = Await.result(userSystem.classicSystem.actorSelection(User.userList(r)).resolveOne(), timeout.duration)
    actor.toTyped[User.Command]
  }


}
