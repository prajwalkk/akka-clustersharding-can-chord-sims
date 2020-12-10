package com.chord.akkasharding

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akkasharding.actors.UserActor.{lookup_data, put_data}
import com.chord.akkasharding.actors.UserGroup.createUser
import com.chord.akkasharding.actors.{UserActor, UserGroup}
import com.chord.akkasharding.utils.{DataUtils, SystemConstants}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/*
*
* Created by: prajw
* Date: 09-Dec-20
*
*/
object UserSimulationDriver extends LazyLogging {

  val userconfig = ConfigFactory.load("usersim.conf")
  val userActorSystem: ActorSystem[UserGroup.Command] = ActorSystem(UserGroup(), "UserActorSystem", config = userconfig.getConfig("my-app"))


  def main(args: Array[String]): Unit = {

    userActorSystem ! createUser(SystemConstants.num_users)
    Thread.sleep(20000)
    val data: List[(String, String)] = DataUtils.read_data()
    val init_length: Int = (data.length * SystemConstants.INIT_PERCENT).toInt
    val (init_data, test_data) = data.splitAt(init_length)
    val keysInserted = init_data.map(i => i._1)
    val testkeys = test_data.map(i => i._1)

    logger.info(s"Inserting $init_length records")
    var init_complete = false
    init_complete = initialize_chord(init_data)
    if (init_complete) {
      logger.info("Starting look ups on initialized data")
      generate_random_lookuprequest(keysInserted)
    }


    //Thread.sleep(10000)
    logger.info("Simulating User inserting data")
    if (generate_random_putrequest(test_data)) {
      logger.info("Simulating data look ups on test data")
      generate_random_lookuprequest(testkeys)
    }
    Thread.sleep(10000)
    userActorSystem.terminate()
  }

  def generate_random_lookuprequest(datakeys: List[String]): Unit = {
    logger.info("Read Requests started")
    implicit val timeout = Timeout(10.seconds)
    implicit val scheduler = userActorSystem.scheduler
    val user = select_random_user()
    for (key <- datakeys) {
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
      user ! put_data(k, v)
      Thread.sleep(10)
    }
    logger.info("Finished Init data")
    true
  }

  def select_random_user(): ActorRef[UserActor.Command] = {
    implicit val timeout: Timeout = Timeout(25.seconds)
    val r = 0 + SystemConstants.random_user.nextInt(SystemConstants.num_users)
    val actor = Await.result(userActorSystem.classicSystem.actorSelection(UserGroup.UserList(r)).resolveOne(), timeout.duration)

    actor.toTyped[UserActor.Command]
  }

  def generate_random_putrequest(test_data: List[(String, String)]): Boolean = {
    implicit val timeout = Timeout(10.seconds)
    implicit val scheduler = userActorSystem.scheduler
    val user = select_random_user()
    for ((k, v) <- test_data) {
      user ! put_data(k, v)
      Thread.sleep(10)
    }
    logger.info("Simulated User put requests")
    true
  }


}
