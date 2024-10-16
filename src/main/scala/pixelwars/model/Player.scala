package pixelwars.model
import pixelwars.network.Client
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

case class Player(name: String, ref: ActorRef[Client.Command])