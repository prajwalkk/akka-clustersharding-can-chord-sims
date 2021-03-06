package com.chord.akkasharding.utils

import com.chord.akkasharding.actors.NodeActor.NodeSetup

/*
*
* Created by: prajw
* Date: 18-Nov-20
*
*/
case class YamlDumpDataHolder(nodeName: String,
                              size: Int,
                              keys: List[Int]) {

}

case object YamlDumpDataHolder {

  def apply(nodeSetup: NodeSetup):YamlDumpDataHolder = {
    YamlDumpDataHolder(nodeName = nodeSetup.nodeName,
      size = nodeSetup.storedData.size,
      keys = nodeSetup.storedData.keys.toList)
  }
}
