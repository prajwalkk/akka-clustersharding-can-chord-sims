package com.chord.akka.utils


import com.chord.akka.actors.NodeActor.NodeSetup

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

  def apply(timeStamp: String, nodeSetup: NodeSetup): YamlDumpNodeProps =

    YamlDumpNodeProps(
      timestamp = timeStamp,
      node_name = nodeSetup.nodeName,
      nodeID = nodeSetup.nodeID,
      nodeRef = nodeSetup.nodeRef.path.name,
      nodeSuccessor = nodeSetup.nodeSuccessor.get.path.name,
      nodePredecessor = nodeSetup.nodePredecessor.get.path.name,
      nodeFingerTable = Some(nodeSetup.nodeFingerTable.map { ft =>
        YamlDumpFingerTableEntity(ft)
      })
    )


}