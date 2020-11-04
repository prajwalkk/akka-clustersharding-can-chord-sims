package com.chord.akka.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.chord.akka.actors.UserActor.logger

import com.typesafe.scalalogging.LazyLogging

object NodeActor extends LazyLogging{
  sealed trait Command
  final case class addNodesToChordRing(id:Int) extends Command


  def apply() :Behavior[Command] =
    Behaviors.setup{context =>
      Behaviors.receiveMessage{
        case addNodesToChordRing(id:Int)=>
          logger.info("Adding Nodes"+id.toString)
          Behaviors.same
      }
    }

}
