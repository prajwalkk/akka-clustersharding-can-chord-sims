package com.chord.akkasharding.actors

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityRef

/*
*
* Created by: prajw
* Date: 13-Nov-20
*
*/
/*case class FingerTableEntity(start: Int,
                             startInterval: Int,
                             endInterval: Int,
                             node: Option[ActorRef[NodeActor.Command]],
                            ) {
  override def toString: String = s"\n$start | [$startInterval, $endInterval) | ${node.get.path.name}"
}*/
case class FingerTableEntity(start: Int,
                             startInterval: Int,
                             endInterval: Int,
                             node: Option[EntityRef[NodeActor.Command]],
                            ) {
  override def toString: String = s"\n$start | [$startInterval, $endInterval) | ${node}"
}
