package com.chord.akka.actors

import akka.actor.typed.ActorRef

/*
*
* Created by: prajw
* Date: 13-Nov-20
*
*/
case class FingerTableEntity2(start: Int,
                              startInterval: Int,
                              endInterval: Int,
                              node: Option[ActorRef[NodeActorTest.Command]],
                             )
