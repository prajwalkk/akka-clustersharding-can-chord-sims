package com.chord.akkasharding.utils


import com.chord.akkasharding.actors.NodeActor.NodeSetup

/*
*
* Created by: prajw
* Date: 15-Nov-20
*
*/
case class YamlDumpNodeProps(timestamp: String,
                             node_name: String,
                             nodeID: Int,
                             nodeRef: String,
                             nodeSuccessor: String,
                             nodePredecessor: String,
                             nodeFingerTable: Option[List[YamlDumpFingerTableEntity]] = None)


object YamlDumpNodeProps {

  def apply(timeStamp: String, nodeSetup: NodeSetup): YamlDumpNodeProps = {
    val pattern = """.*(Node_[0-9]+).*$""".r
    val pattern(nodeRef) = nodeSetup.nodeRef.toString
    val pattern(nodeSuccessor) = nodeSetup.nodeSuccessor.get.toString
    val pattern(nodePredecessor) = nodeSetup.nodePredecessor.get.toString

    YamlDumpNodeProps(
      timestamp = timeStamp,
      node_name = nodeSetup.nodeName,
      nodeID = nodeSetup.nodeID,
      nodeRef = nodeRef,
      nodeSuccessor = nodeSuccessor,
      nodePredecessor = nodePredecessor,
      nodeFingerTable = Some(nodeSetup.nodeFingerTable.map { ft =>
        YamlDumpFingerTableEntity(ft)
      })
    )
  }


}