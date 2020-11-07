package com.chord.akka.webserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.chord.akka.actors.{NodeGroup, UserActor}
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}


/*
*
* Created by: prajw
* Date: 04-Nov-20
*
*/
object HttpServer extends LazyLogging {

  def setupServer(): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      //TODO change this
      //TODO add requestID
      val nodeActor = context.spawn(NodeGroup(), "UserActorTest")
      context.watch(nodeActor)

      val routes = new NodeRoutes(nodeActor)(context.system)
      startHttpServer(routes.lookupRoutes)(context.system)
//      val userRegisteryActor = context.spawn(UserActor("UserRegistry"),"UserRegistryActor")
//      context.watch(userRegisteryActor)
//      val routes = new UserRoutes(userRegisteryActor)(context.system)
//      startHttpServer(routes.userRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
  }

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    //TODO remove hardcoding
    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}")
      case Failure(exception) =>
        system.log.error("Failed to bin HTTP endpoint, terminating system. ", exception)
        system.terminate()
    }
  }
}

