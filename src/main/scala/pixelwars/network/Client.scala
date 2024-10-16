package pixelwars.network
import akka.actor.typed.{ActorRef, PostStop, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.cluster.typed._
import akka.{ actor => classic }
import akka.actor.typed.scaladsl.adapter._
import scalafx.collections.ObservableHashSet
import scalafx.collections.ObservableBuffer
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.application.Platform
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.Address
import pixelwars.model.Player
import pixelwars.MainApp
import pixelwars.controller.GameController
import pixelwars.model.GameState
import scalafx.scene.paint.Color

object Client {
    val players = new ObservableHashSet[Player]()
    val chatHistory = new ObservableBuffer[String]()
    val guessHistory = new ObservableBuffer[String]() 
    var stageChanged: Boolean = false
    var admin: Boolean = false

    val unreachables = new ObservableHashSet[Address]()
    var remoteOpt: Option[ActorRef[Server.Command]] = None
    var defaultBehavior: Option[Behavior[Client.Command]] = None
    var playerName: Option[String] = None

    unreachables.onChange { (ns, _) =>
        Platform.runLater {
            MainApp.control.updateList(players.toList.filter(y => !unreachables.exists(x => x == y.ref.path.address)))
        }
    }

    players.onChange { (ns, _) =>
        Platform.runLater {
            MainApp.control.updateList(ns.toList.filter(y => !unreachables.exists(x => x == y.ref.path.address)))
        }
    }
    chatHistory.onChange { (ns, _) =>
        Platform.runLater {
            MainApp.control.updateChats(ns)
        }
    }

    guessHistory.onChange { (ns, _) =>
        Platform.runLater {
            MainApp.control2.foreach(x => x.updateListGuess(ns))
        }
    }

    // protocol for chat client server
    sealed trait Command

    // start signal for client actor
    case object start extends Command 

    // server discovery protocol
    final case object ServerRequest extends Command
    private case class ServerReponse (listing: Receptionist.Listing) extends Command

    // client joining server protocol
    case class Initialize(name: String) extends Command
    case class Joined(list: Iterable[Player]) extends Command
    case class UpdatePlayerList(list: Iterable[Player]) extends Command
    case class UpdateChatHistory(list: Iterable[String]) extends Command
    case class UpdateGuessHistory(list: Iterable[String]) extends Command


    // sending message protocol
    case class Message(msg: String, from: Player) extends Command
    case class SendMessage(target: ActorRef[Client.Command], content: String) extends Command

    // game logic protocol
    case class ChangeStage() extends Command
    case class UpdateStage() extends Command
    case class ChangeCanvasPressed(x: Double, y: Double, colour: String) extends Command
    case class ChangeCanvasDragged(x: Double, y: Double) extends Command
    case class UpdateCanvasPressed(x: Double, y: Double, colour: String) extends Command
    case class UpdateCanvasDragged(x: Double, y: Double) extends Command
    case class Guess(guess: String, from: Player) extends Command
    case class SendGuess(target: ActorRef[Client.Command], content: String) extends Command
    case class ChangePrompt(prompt: String) extends Command
    case class UpdatePrompt() extends Command
    case class ChangeAdmin() extends Command
    case class UpdateAdmin() extends Command
    case class EndGame() extends Command
    case class End() extends Command


  def apply2(): Behavior[Client.Command] = Behaviors.receive[Client.Command] { (context, message) =>
    message match {
      case SendMessage(target, content) =>
        target ! Message(content, Player(playerName.getOrElse("Empty"), context.self))
        Behaviors.same
      case Message(msg, from) =>
        Platform.runLater {
          for (remote <- remoteOpt) {
            remote ! Server.UpdateMasterChatHistory(from.name + ": " + msg)
          }
        }
        Behaviors.same

      //
      case SendGuess(target, content) =>
        target ! Guess(content, Player(playerName.getOrElse("Empty"), context.self))
        Behaviors.same
      case Guess(guess, from) =>
        Platform.runLater {
          for (remote <- remoteOpt) {
            remote ! Server.UpdateMasterGuessHistory(from.name + ": " + guess)
          }
        }
        Behaviors.same
      //

      case UpdatePlayerList(list: Iterable[Player]) =>
        players.clear()
        players ++= list
        Behaviors.same

      case UpdateChatHistory(list) =>
        chatHistory.clear
        chatHistory ++= list
        Behaviors.same

      case UpdateGuessHistory(list) => 
        guessHistory.clear
        guessHistory ++= list
        Behaviors.same

      case ChangeStage() => 
        stageChanged = true
        Platform.runLater {
          MainApp.startGame
        }
        
        Behaviors.same

      case UpdateStage() =>
        Platform.runLater {
          for (remote <- remoteOpt) {
            remote ! Server.UpdateMasterStage()
          }
        }
        Behaviors.same

      case ChangeCanvasPressed(canvasX: Double, canvasY: Double, colour: String) => 
        Platform.runLater {
          MainApp.control2.foreach(x => x.drawPressed(canvasX,canvasY,colour))
        }

        Behaviors.same

      case ChangeCanvasDragged(canvasX: Double, canvasY: Double) => 
        Platform.runLater {
          MainApp.control2.foreach(x => x.drawDragged(canvasX,canvasY))
        }

        Behaviors.same

      case UpdateCanvasPressed(x: Double, y: Double, colour: String) =>
        Platform.runLater {
          for (player <- players) {
            player.ref ! Client.ChangeCanvasPressed(x,y, colour)
          }
        }

        Behaviors.same

      case UpdateCanvasDragged(x: Double, y: Double) =>
        Platform.runLater {
          for (player <- players) {
            player.ref ! Client.ChangeCanvasDragged (x,y)
          }
        }

        Behaviors.same

      case UpdateAdmin() =>
        Platform.runLater {
          for (remote <- remoteOpt) {
            remote ! Server.UpdateMasterAdmin()
          }
        }

        Behaviors.same

      case ChangeAdmin() =>
        Platform.runLater {
          MainApp.control2.foreach(x=> x.admin = true)
          MainApp.control2.foreach(x=> x.setVisibility())
        }

        Behaviors.same

      case UpdatePrompt() =>
        Platform.runLater {
          for (remote <- remoteOpt) {
            remote ! Server.UpdateMasterPrompt()
          }
        }

        Behaviors.same

      case ChangePrompt(prompt: String) =>
        Platform.runLater {
          MainApp.control2.foreach(x => x.displayPrompt(prompt))
        }

        Behaviors.same

      case EndGame() =>
        Platform.runLater {
          for (remote <- remoteOpt) {
            remote ! Server.EndAllGames()
          }
        }

        Behaviors.same

      case End() =>
        Platform.runLater {
          MainApp.endGame()
        }

        Behaviors.same


    }
  }.receiveSignal {
    case (context, PostStop) =>
      for (name <- playerName;
           remote <- remoteOpt) {
        remote ! Server.Leave(name, context.self)
      }
      defaultBehavior.getOrElse(Behaviors.same)
  }

  // main behavior for the client actor
  def apply(): Behavior[Client.Command] =
    Behaviors.setup { context =>

        // Middleman between server and client. Converts listings from receptions into client reponses.
        val listingAdapter: ActorRef[Receptionist.Listing] =
        context.messageAdapter { listing =>
            println(s"listingAdapter:listing: ${listing.toString}")
            Client.ServerReponse(listing)
        }

        // Subscribes to the server cluster
        context.system.receptionist ! Receptionist.Subscribe(Server.ServerKey, listingAdapter)

      // Define default behavior
        defaultBehavior = Option(Behaviors.receiveMessage { message =>
        message match {
            // Start the server discovery process
            case Client.start =>
                context.self ! ServerRequest
                Behaviors.same

            case ServerRequest =>
                println(s"Finding Server...")
                context.system.receptionist !
                Receptionist.Find(Server.ServerKey, listingAdapter)
                Behaviors.same

            case ServerReponse(Server.ServerKey.Listing(listings)) =>
                val xs: Set[ActorRef[Server.Command]] = listings
                for (x <- xs) {
                remoteOpt = Some(x)
                }
                Behaviors.same

            case Joined(list) => 
                Platform.runLater {
                    MainApp.control.displayStatus("READY")
                }
                players.clear()
                players ++= list
                apply2()

            case UpdatePlayerList(list) => 
                players.clear()
                players ++= list
                Behaviors.same

            case UpdateChatHistory(list) => 
                chatHistory.clear()
                chatHistory ++= list
                Behaviors.same

            case Initialize(name) =>
                playerName = Option(name)
                for (remote <- remoteOpt) {
                    remote ! Server.Join(name, context.self)
                }
                Behaviors.same
        }
      })
      defaultBehavior.get
    }


}
