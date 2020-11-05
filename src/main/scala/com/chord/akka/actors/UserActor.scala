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
final case class lookup_data(key:String ) extends Command


}

class UserActor(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  import UserActor._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case lookup_data(key) =>
        context.log.info("Key Received "+key)

        this
    }
}

