package com.chord.akka.utils

import java.time.LocalDateTime

import com.chord.akka.actors.FingerTableEntity2
import com.chord.akka.actors.NodeActorTest.NodeSetup


/*
*
* Created by: prajw
* Date: 15-Nov-20
*
*/
case class YamlDumpFingerTableEntity(start: String,
                                     interval: List[Int],
                                     succ: String)


object YamlDumpFingerTableEntity {


  def apply(fingerTableEntity: FingerTableEntity2): YamlDumpFingerTableEntity =
    YamlDumpFingerTableEntity(
      start = fingerTableEntity.start.toString,
      interval = List(fingerTableEntity.startInterval, fingerTableEntity.endInterval),
      succ = fingerTableEntity.node.get.path.name
    )

}


