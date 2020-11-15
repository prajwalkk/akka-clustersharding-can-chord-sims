//package com.example
//
//import akka.actor.testkit.typed.scaladsl.ActorTestKit
//import akka.actor.typed.{ActorRef, ActorSystem}
//import akka.http.scaladsl.marshalling.Marshal
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import com.chord.akka.actors.{LookupObject, NodeActor}
//import com.chord.akka.webserver.NodeRoutes
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.{Matchers, WordSpec}
//
//
//class UserRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
//
//
//  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
//  // so we have to adapt for now
//  lazy val testKit: ActorTestKit = ActorTestKit()
//
//  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
//
//  lazy val routes: Route = new NodeRoutes(nodeGroupSystem).lookupRoutes
//  // Here we need to implement all the abstract members of UserRoutes.
//  // We use the real UserRegistryActor to test it while we hit the Routes,
//  // but we could "mock" it by implementing it in-place or by using a TestProbe
//  // created with testKit.createTestProbe()
//  val nodeGroupSystem: ActorRef[NodeActor.Command] = testKit.spawn(NodeActor("UserActorTest"))
//
//  override def createActorSystem(): akka.actor.ActorSystem =
//    testKit.system.classicSystem
//
//  // use the json formats to marshal and unmarshall objects in the test
//
//  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
//  import com.chord.akka.webserver.JsonFormats._
//
//  "UserRoutes" should {
//    "return no entries if none present (GET /chord)" in {
//      // note that there's no need for the host part in the uri:
//      val request = HttpRequest(uri = "/chord")
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and no entries should be in the list:
//
//        entityAs[String] should ===("""{"objs":[]}""")
//      }
//    }
//
//    "be able to add users (POST /chord)" in {
//      val user = LookupObject("Kapi", "jp")
//      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures
//
//      // using the RequestBuilding DSL:
//      val request = Post("/chord").withEntity(userEntity)
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.Created)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and we know what message we're expecting back:
//
//        entityAs[String] should ===("""{"description":"object Kapi created"}""")
//      }
//    }
//
//
//    "return 1 entry if present (GET /chord/ID)" in {
//      // note that there's no need for the host part in the uri:
//      val request = HttpRequest(uri = "/chord/Kapi")
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and no entries should be in the list:
//
//        entityAs[String] should ===("""{"key":"Kapi","value":"jp"}""")
//      }
//    }
//
//    "return 0 entry if not present (GET /chord/ID)" in {
//      // note that there's no need for the host part in the uri:
//      val request = HttpRequest(uri = "/chord/Kpapi")
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.NoContentType)
//
//        // and no entries should be in the list:
//        entityAs[String] should ===("")
//      }
//    }
//  }
//}
//
