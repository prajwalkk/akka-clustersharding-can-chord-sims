package com.chord.akkasharding.webserver

import akka.actor.Terminated
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef, EntityTypeKey}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.chord.akkasharding.actors.NodeActor
import com.chord.akkasharding.simulation.Simulation
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import akka.{Done, actor => classic}
import akka.actor.CoordinatedShutdown


/*
*
* Created by: prajw
* Date: 04-Nov-20
*

 */
object HttpServer extends LazyLogging {

  def setupServer(typeKey: EntityTypeKey[NodeActor.Command], sharding: ClusterSharding,system: ActorSystem[_]): Unit = {


      //val nodeActor = context.spawn(NodeActor("HTTPServer"), "HTTPServer")
      val nodeActor: EntityRef[NodeActor.Command] = sharding.entityRefFor(typeKey, "HTTPServer")
     // context.watch(nodeActor)

      val routes = new NodeRoutes(nodeActor)(system)
      startHttpServer(routes.lookupRoutes,system)

      Behaviors.empty

    //commented this code out
   //val system = ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")


  }

  private def startHttpServer(routes: Route,system: ActorSystem[_]): Unit = {
    import akka.actor.typed.scaladsl.adapter._
    implicit val classicSystem: classic.ActorSystem = system.toClassic
    val shutdown = CoordinatedShutdown(classicSystem)
    import system.executionContext

    //TODO remove hardcoding
    logger.info("Binding HTTP Server")
    /*val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}")
      case Failure(exception) =>
        system.log.error("Failed to bin HTTP endpoint, terminating system. ", exception)
        system.terminate()
    }*/


    Http().bindAndHandle(routes, "localhost",8000).onComplete {
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
              "Server http://{}:{}/ graceful shutdown completed",
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

   def stopHttpServer(system: ActorSystem[NodeActor]) ={
    system.log.info("Stopping Http Server")
    system.terminate()
  }
}


