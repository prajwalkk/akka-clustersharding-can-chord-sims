package com.can.akka.webserver

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.can.akka.actors.{NodeActor, NodeGroup}
import com.can.akka.simulation.Simulation.{sharding, typekey}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object HttpServer extends LazyLogging {


  def setupServer(system: ActorSystem[NodeGroup.Command]): Unit = {
    val node: EntityRef[NodeActor.Command] = sharding.entityRefFor(typekey, "AkkaHTTPServer")
    system.log.debug(s"Node http $node")
    val routes = new CANRoutes(node)(system)
    startHttpServer(routes.lookupRoutes)(system)

  }

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    val shutdown = CoordinatedShutdown(system)
    Http().bindAndHandle(routes, "localhost", 8000).onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "HTTPServer online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
        shutdown.addTask(
          CoordinatedShutdown.PhaseServiceRequestsDone,
          "http-graceful-terminate"
        ) { () =>
          binding.terminate(10.seconds).map { _ =>
            system.log.error(
              "AkkaHTTPServer http://{}:{}/ graceful shutdown completed",
              address.getHostString,
              address.getPort
            )
            Done
          }
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }


  }

  def stopHttpServer(system: ActorSystem[NodeActor]) = {
    system.log.info("Stopping Http Server")
    system.terminate()
  }
}
