package com.chord.akka.webserver
import com.chord.akka.actors.{LookupObject, LookupObjects, NodeActorTest, RequestObject, StoredObject}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}



//#json-formats

object JsonFormats  {
  //import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._


  implicit val usersJsonFormat: RootJsonFormat[LookupObjects] = jsonFormat1(LookupObjects)

  implicit val actionPerformedJsonFormat: RootJsonFormat[NodeActorTest.ActionSuccessful] = jsonFormat1(NodeActorTest.ActionSuccessful)


  implicit val getdataJsonFormat: RootJsonFormat[LookupObject] = jsonFormat1(LookupObject)
  implicit val requestJsonFprmat :RootJsonFormat[RequestObject] = jsonFormat2(RequestObject)
  implicit val storeddataJsonFormat : RootJsonFormat[StoredObject] = jsonFormat2(StoredObject)
}
//#json-formats