package com.chord.akka.webserver
import com.chord.akka.actors.{InsertObject, LookupObject, LookupObjects, NodeActor, NodeActorTest, StoredObject}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}



//#json-formats

object JsonFormats  {
  //import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val getvalueJsonFormat: RootJsonFormat[LookupObject] = jsonFormat1(LookupObject)
  implicit val putvalueJsonFormat : RootJsonFormat[StoredObject] = jsonFormat2(StoredObject)
  implicit val insertvalueJsonFormat:RootJsonFormat[InsertObject] = jsonFormat2(InsertObject)
  implicit val usersJsonFormat: RootJsonFormat[LookupObjects] = jsonFormat1(LookupObjects)

  //implicit val actionPerformedJsonFormat: RootJsonFormat[NodeActor.ActionSuccessful] = jsonFormat1(NodeActor.ActionSuccessful)
  implicit val actionPerformedJsonFormat: RootJsonFormat[NodeActorTest.ActionSuccessful] = jsonFormat1(NodeActorTest.ActionSuccessful)
}
//#json-formats