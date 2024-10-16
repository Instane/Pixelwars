package pixelwars.controller

import akka.actor.typed.ActorRef
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, ListView, TextField}
import pixelwars.network.Client
import pixelwars.model.Player
import pixelwars.MainApp
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import pixelwars.MainApp
import scalafx.scene.canvas
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.control.ListCell
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.MouseEvent
import scalafx.scene.paint.Color
import scalafx.application.Platform
import pixelwars.model.GameState
import scalafx.scene.input.{KeyCode, KeyEvent}
import scala.concurrent._

@sfxml
class EndScreenController () {

    def handleExit(event: MouseEvent): Unit = {
        MainApp.stage.close()
    }
}


