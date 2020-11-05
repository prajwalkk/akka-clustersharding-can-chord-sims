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
  final case class lookupToServer(key: Int)
  final case class createUser(num_users: Int) extends Command



}

class UserActor(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  import UserActor._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case createUser(n) =>
        context.log.info(s"Creating $n Users")

        for (i <- 0 until n) {
          val userId: String = "User-" + i
          val user = context.spawn(UserActor(userId), userId)
          userList(i) = user.path.toString
          context.log.info("User Created "+userList(i))
        }

        this


      case lookupToServer(key) =>


    }
}

