package com.chord.akkasharding.webserver

import com.chord.akkasharding.actors.{LookupObject, NodeActor, RequestObject, StoredObject}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


//#json-formats

object JsonFormats {
  //import the default encoders for primitive types (Int, String, Lists etc)

  import DefaultJsonProtocol._


  implicit val actionPerformedJsonFormat: RootJsonFormat[NodeActor.ActionSuccessful] = jsonFormat1(NodeActor.ActionSuccessful)
  implicit val getdataJsonFormat: RootJsonFormat[LookupObject] = jsonFormat1(LookupObject)
  implicit val requestJsonFprmat: RootJsonFormat[RequestObject] = jsonFormat2(RequestObject)
  implicit val storeddataJsonFormat: RootJsonFormat[StoredObject] = jsonFormat2(StoredObject)
}

//#json-formats