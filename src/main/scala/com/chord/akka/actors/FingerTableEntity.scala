package com.chord.akka.actors
import akka.actor.typed.ActorRef

/*
*
* Created by: prajw
* Date: 10-Nov-20
*
*/
case class FingerTableEntity(start: Int,
                             startInterval: Int,
                             endInterval: Int,
                             node: Option[ActorRef[NodeActorTest.Command]],
                             ) {

  def getInterval: Range = (startInterval until endInterval)
}
