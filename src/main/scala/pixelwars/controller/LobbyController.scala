package pixelwars.controller
import akka.actor.typed.ActorRef
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, ListView, TextField}
import pixelwars.network.Client
import pixelwars.model.Player
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import pixelwars.MainApp
import scalafx.application.Platform
import scalafx.scene.control.ListCell
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.{KeyCode, KeyEvent}

@sfxml
class LobbyController(private val txtName: TextField,
                        private val lblStatus: Label, 
                        val listUser: ListView[Player],
                        private val listMessage: ListView[String],
                        private val txtMessage: TextField,
                        private val startButton: Button) {

    var clientRef: Option[ActorRef[Client.Command]] = None

    // Store received messages
    //val receivedText: ObservableBuffer[String] =  new ObservableBuffer[String]()

    //listMessage.items = receivedText

    // Set a custom cell factory for the listUser ListView
    listUser.cellFactory = { _ =>
      new ListCell[Player] {
        item.onChange { (_, _, player) =>
          text = if (player != null) player.name else ""
        }
      }
    }

    def handleEnterPressed(event: KeyEvent): Unit = {
          if (event.code == KeyCode.Enter) {
            listUser.items().foreach { player => 
              MainApp.mainSystem ! Client.SendMessage(player.ref, txtMessage.text())
            }
            txtMessage.clear()
        }
    }
    def handleStartButton(action: ActionEvent): Unit = {
      if (listUser.items().length > 1) {
        MainApp.mainSystem ! Client.UpdateStage()
      }
      
    }
    def handleJoin(action: ActionEvent): Unit = {
        if(txtName != null) {
          clientRef map((x)=> x ! Client.Initialize(txtName.text.value))
        }
          
    }
    def handleOnKeyPressedJoin(event: KeyEvent): Unit = {
            if (event.code == KeyCode.Enter) {
                  if(txtName != null) {
                    clientRef map((x)=> x ! Client.Initialize(txtName.text.value))
                  }
                  txtName.clear()
                }
              
            
    }

    def displayStatus(text: String): Unit = {
        lblStatus.text = text
    }
    def updateList(x: Iterable[Player]): Unit ={
      listUser.items = new ObservableBuffer[Player]() ++= x
    }
    def updateChats(x: Iterable[String]): Unit = {
      listMessage.items = new ObservableBuffer[String]() ++= x
    }
    def handleSend(actionEvent: ActionEvent): Unit ={
      listUser.items().foreach { player => 
        MainApp.mainSystem ! Client.SendMessage(player.ref, txtMessage.text())
      }
      txtMessage.clear()
    }

    // def formatMessage(text: String, from: ActorRef[Client.Command]): String = {
    //   val senderName = listUser.items().find(_.ref == from).map(_.name).getOrElse("Unknown")
    //   val msg = s"$senderName: $text"
    //   return msg
    // }

}