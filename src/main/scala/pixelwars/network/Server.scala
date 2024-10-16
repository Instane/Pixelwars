package pixelwars.network
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import scalafx.application.Platform
import scalafx.collections.ObservableHashSet
import scalafx.collections.ObservableBuffer
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import pixelwars.model.GameState
import pixelwars.model.Player

object Server {

    // Protocol for chat server
    sealed trait Command
    case class Join(name: String, ref: ActorRef[Client.Command]) extends Command
    case class Leave(name: String, ref: ActorRef[Client.Command]) extends Command
    case class UpdateMasterChatHistory(content: String) extends Command

    // Protocol for game
    case class UpdateMasterStage() extends Command
    case class UpdateMasterCanvasPressed(x: Double, y: Double) extends Command
    case class UpdateMasterCanvasDragged(x: Double, y: Double) extends Command
    case class UpdateMasterGuessHistory(content: String) extends Command
    case class UpdateMasterPrompt() extends Command
    case class UpdateMasterAdmin() extends Command
    case class EndAllGames() extends Command

    // ServiceKey for the server
    val ServerKey: ServiceKey[Server.Command] = ServiceKey("Server")

    // Server state containing all connected players
    val players = new ObservableHashSet[Player]()
    
    // Server state containing lobby chat history
    val chatHistory = new ObservableBuffer[String]()

    // Server state containing game guesses history
    val guessHistory = new ObservableBuffer[String]()

    // Update every player about newly joined players
    players.onChange((newPlayerList, change) => {
        val masterList = Client.UpdatePlayerList(newPlayerList.toList) 
        for (player <- newPlayerList) {
            player.ref ! masterList
        }
    })

    // Update every player about newly joined players
    chatHistory.onChange((newChatList, change) => {
        var newChats = GameState.removeConsecutiveDuplicates(newChatList)
        val masterChats = Client.UpdateChatHistory(newChats.toList) 
        for (player <- players) {
            player.ref ! masterChats
        }
    })

    // Update every player about newly joined players
    guessHistory.onChange((newGuessList, change) => {
        var newGuesses = GameState.removeConsecutiveDuplicates(newGuessList)
        val masterGuesses = Client.UpdateGuessHistory(newGuesses.toList) 
        for (player <- players) {
            player.ref ! masterGuesses
        }
    })

    // Behavior for the chat server actor
    def apply(): Behavior[Server.Command] =
        Behaviors.setup { context =>

        // Register the server with the Receptionist
        context.system.receptionist ! Receptionist.Register(ServerKey, context.self)

        Behaviors.receiveMessage { message =>
            message match {
                case Join(name, ref) =>
                    players += Player(name,ref)
                    ref ! Client.Joined(players.toList)
                    ref ! Client.UpdatePlayerList(players.toList)
                    ref ! Client.UpdateChatHistory(chatHistory.toList)
                    Behaviors.same

                case Leave(name,ref) =>
                    players -= Player(name,ref)
                    Behaviors.same

                case UpdateMasterChatHistory(content) =>
                    chatHistory += content
                    Behaviors.same
                
                case UpdateMasterGuessHistory(content) =>
                    guessHistory += content
                    Behaviors.same

                case UpdateMasterAdmin() =>
                    players.head.ref ! Client.ChangeAdmin()
                    Behaviors.same

                case UpdateMasterStage() =>
                    GameState.stageChange = true
                    for (player <- players) {
                        player.ref ! Client.ChangeStage() 
                    }
                    Behaviors.same

                case UpdateMasterPrompt() =>
                    GameState.masterPrompt = GameState.randomDrawingPrompt()
                    for (player <- players) {
                        player.ref ! Client.ChangePrompt(GameState.masterPrompt) 
                    }
                    Behaviors.same


                case EndAllGames() =>
                    for (player <- players) {
                        player.ref ! Client.End() 
                    }
                    Behaviors.same


                // case UpdateMasterCanvasPressed(x: Double, y: Double) =>
                //     GameState.canvasX = x
                //     GameState.canvasY = y
                //     Platform.runLater {
                //         for (player <- players) {
                //             player.ref ! Client.ChangeCanvasPressed(GameState.canvasX, GameState.canvasY)
                //         }
                //     }
                //     Behaviors.same

                // case UpdateMasterCanvasDragged(x: Double, y: Double) =>
                //     GameState.canvasX = x
                //     GameState.canvasY = y
                //     Platform.runLater {
                //         for (player <- players) {
                //             player.ref ! Client.ChangeCanvasDragged(GameState.canvasX, GameState.canvasY)
                //         }
                //     }
                //     Behaviors.same                    
                }
            }
        }
}

// Entry point for the server application
object ServerApp extends App {
  // Create the actor system for the ChatServer
  val mainSystem: ActorSystem[Server.Command] = ActorSystem(Server(), "MainSystem")
}
