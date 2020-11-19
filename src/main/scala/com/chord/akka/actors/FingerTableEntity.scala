package com.chord.akka.actors

import akka.actor.typed.ActorRef

/*
*
* Created by: prajw
* Date: 13-Nov-20
*
*/
case class FingerTableEntity(start: Int,
                             startInterval: Int,
                             endInterval: Int,
                             node: Option[ActorRef[NodeActor.Command]],
                            ) {
  override def toString: String = s"\n$start | [$startInterval, $endInterval) | ${node.get.path.name}"
}
