package com.chord.akka.actors
import com.chord.akka.utils.Helper
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.UserGroup.Command
import com.chord.akka.utils.SystemConstants
import scala.util.Random

object UserGroup {



  def apply(): Behavior[Command] =
    Behaviors.setup(context => new UserGroup(context))

  sealed trait Command
  final case class createUser(num_users: Int) extends Command
  final case class lookup_data_randomly(key:String) extends Command


}

class UserGroup(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {

  import UserGroup._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case createUser(n) =>
        context.log.info(s"Creating $n Users")
        for (i <- 0 until n) {
          val userId: String =Helper.generateRandomName()
          val id = Helper.getIdentifier(userId.toString)
          val user = context.spawn(UserActor(id.toString), id.toString)

          context.log.info("User Created " + user.path.toString)
        }
        context.log.info(context.children.toSeq.toString())
        this
      case lookup_data_randomly(key) =>
        context.log.info("Selecting a random User to perform lookup")
//        val l = Random.between(0,context.children.knownSize.length)
        this

    }
}


