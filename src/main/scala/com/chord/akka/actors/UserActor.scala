package com.chord.akka.actors


import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.UserActor.Command

object UserActor {

  def apply(id: String): Behavior[Command] =
    Behaviors.setup(context => new UserActor(context, id))

  sealed trait Command

  final case class createUsers(num_users: Int) extends Command

}

class UserActor(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  import UserActor._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case createUsers(n) =>
        context.log.info(s"Creating $n Users")
        val userList = new Array[String](n)
        for (i <- 0 until n) {
          val userId: String = "User-" + i
          userList(i) = userId
          context.spawn(UserActor(userId), userId)
        }
        this
    }
}

