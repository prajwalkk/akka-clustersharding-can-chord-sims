package com.chord.akka.actors


import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.UserActor.Command
import com.chord.akka.utils.SystemConstants


object UserActor {

  var userList = new Array[String](SystemConstants.num_users)

  def apply(id: String): Behavior[Command] =
    Behaviors.setup(context => new UserActor(context, id))

sealed trait Command
final case class get_data(key:String ) extends Command
final case class put_data(key:String,value:String ) extends Command


}

class UserActor(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  import UserActor._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case get_data(key) =>
        context.log.info("Key Received "+key)
       // val response = Http()(context.system).singleRequest(HttpRequest(uri="http://localhost:8080/chord").addAttribute("key",key))

        this
      case put_data(key,value)=>
        context.log.info("Data Received "+key+" "+value)
      this


    }
}

