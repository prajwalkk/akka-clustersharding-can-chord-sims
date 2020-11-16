package com.chord.akka.utils

import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

object DataUtils extends LazyLogging {

  def read_data(): List[(String, String)] = {
    logger.info("Getting Data from source")
    val keys: List[String] = Source.fromResource("Hamlet_modern.txt").getLines().toList
    val values: List[String] = Source.fromResource("Hamlet_original.txt").getLines().toList
    val DataEntry = keys zip values
    DataEntry

  }


}
