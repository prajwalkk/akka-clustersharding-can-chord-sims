package com.chord.akka.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.LazyLogging

object UserActor extends LazyLogging {

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case createUsers(n: Int) =>
          logger.info("creating user" + n.toString)
          Behaviors.same
      }
    }

  sealed trait Command

  final case class createUsers(n: Int) extends Command


}
