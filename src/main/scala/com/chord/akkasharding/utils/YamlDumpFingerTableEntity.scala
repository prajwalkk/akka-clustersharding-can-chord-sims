package com.chord.akkasharding.utils

import java.time.LocalDateTime

import com.chord.akkasharding.actors.FingerTableEntity
import com.chord.akkasharding.actors.NodeActor.NodeSetup


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


  def apply(fingerTableEntity: FingerTableEntity): YamlDumpFingerTableEntity = {
    val pattern = """.*(Node_[0-9]+).*$""".r
    val pattern(succ) = fingerTableEntity.node.get.toString
    YamlDumpFingerTableEntity(
      start = fingerTableEntity.start.toString,
      interval = List(fingerTableEntity.startInterval, fingerTableEntity.endInterval),
      succ = succ
    )
  }

}


