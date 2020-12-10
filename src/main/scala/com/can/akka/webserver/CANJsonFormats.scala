package com.can.akka.webserver

import com.can.akka.actors.{LookupObject, NodeActor, RequestObject, StoredObject}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object CANJsonFormats {

  import DefaultJsonProtocol._
  implicit val actionPerformedJsonFormat: RootJsonFormat[NodeActor.ActionSuccessful] = jsonFormat1(NodeActor.ActionSuccessful)
  implicit val getdataJsonFormat: RootJsonFormat[LookupObject] = jsonFormat1(LookupObject)
  implicit val requestJsonFprmat: RootJsonFormat[RequestObject] = jsonFormat2(RequestObject)

}
