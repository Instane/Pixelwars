package pixelwars

import akka.cluster.typed._
import akka.{ actor => classic }
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.adapter._
import com.typesafe.config.ConfigFactory
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.{FXMLLoader, NoDependencyResolver}
import scalafx.Includes._
import scala.concurrent.Future
import scala.concurrent.duration._
import pixelwars.network.Client
import scalafx.scene.input.MouseEvent


object MainApp extends JFXApp {
        // Create the actor system for the ChatClient
        val mainSystem: ActorSystem[Client.Command] = ActorSystem(Client(), "MainSystem")

        // Start the process of finding the chat server actor
        mainSystem ! Client.start

        // Load the FXML layout for the main window
        val loader = new FXMLLoader(null, NoDependencyResolver)
        loader.load(getClass.getResourceAsStream("view/Lobby.fxml"))
        val border: scalafx.scene.layout.BorderPane = loader.getRoot[javafx.scene.layout.BorderPane]()
        val control = loader.getController[pixelwars.controller.LobbyController#Controller]()
        var control2 : Option[pixelwars.controller.GameController#Controller] = None
        var control3 : Option[pixelwars.controller.EndScreenController#Controller] = None
        control.clientRef = Option(mainSystem)
        val cssResource = getClass.getResource("styles/styles.css")
        
        // Set up the main stage with the loaded FXML layout and apply the DarkTheme stylesheet
        stage = new PrimaryStage() {
            scene = new Scene() {
                root = border
                stylesheets = List(cssResource.toExternalForm)
            }
        }
        
        def startGame(): Unit = {
            val loader = new FXMLLoader(null, NoDependencyResolver)
            loader.load(getClass.getResourceAsStream("view/Game.fxml"))
            val border: scalafx.scene.layout.AnchorPane = loader.getRoot[javafx.scene.layout.AnchorPane]()
            control2 = Some(loader.getController[pixelwars.controller.GameController#Controller]())
            control2.foreach(x => x.initialize)
            stage.scene = new Scene() {
            root = border
            stylesheets = List(cssResource.toExternalForm)
            
        }}

        def endGame(): Unit = {
            val loader = new FXMLLoader(null, NoDependencyResolver)
            loader.load(getClass.getResourceAsStream("view/EndScreen.fxml"))
            control3 = Some(loader.getController[pixelwars.controller.EndScreenController#Controller]())
            val border: scalafx.scene.layout.AnchorPane = loader.getRoot[javafx.scene.layout.AnchorPane]()
            stage.scene = new Scene() {
            root = border
            stylesheets = List(cssResource.toExternalForm)
            
        }}

        stage.resizable_=(false)

        // Handle the close request of the main stage by terminating the actor system
        stage.onCloseRequest = handle({
            mainSystem.terminate
        })
        
}





