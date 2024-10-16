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

import java.util.{Timer, TimerTask}
import scala.concurrent._

@sfxml
class GameController (private var drawCanvas: Canvas, 
                      private var drawingPrompt:Label,
                      private var guessMessage: TextField,
                      private var listGuess: ListView[String]
                      ) {
    
    var gc: GraphicsContext = _
    var admin: Boolean = false
    drawingPrompt.visible = false
    val timer = new Timer()

    val task = new TimerTask {
      def run(): Unit = {
        MainApp.control.listUser.items().foreach { player =>
          MainApp.mainSystem ! Client.EndGame()
        }
      }}

    def initialize(): Unit = {
        gc = drawCanvas.graphicsContext2D
        changePrompt()
        updateAdmin()
        timer.schedule(task, 10000)
    }

    def setVisibility(): Unit = {
        if (admin == true) {
            drawingPrompt.visible = true
        }
    }

    def changePrompt(): Unit = {
        Platform.runLater {
            MainApp.mainSystem ! Client.UpdatePrompt()
        }
    }

    def displayPrompt(prompt: String): Unit = {
        drawingPrompt.text = prompt
        
    }

    def updateAdmin(): Unit = {
        Platform.runLater {
            MainApp.mainSystem ! Client.UpdateAdmin()
        }
    }

    def handleKeyPressed(event: KeyEvent): Unit = {
        if (admin == false) {
            if (event.code == KeyCode.Enter) {
                MainApp.control.listUser.items().foreach { player => 
                MainApp.mainSystem ! Client.SendGuess(player.ref, guessMessage.text()) 
                }
                checkGuess()
                guessMessage.clear()
            }
        }
    }

    def checkGuess(): Unit = {
        if (guessMessage.text().toLowerCase == drawingPrompt.text.value.toLowerCase) {
            MainApp.control.listUser.items().foreach { player => 
              MainApp.mainSystem ! Client.EndGame() 
              
            }
        }
    }

    def updateListGuess(x: Iterable[String]): Unit = {
      listGuess.items = new ObservableBuffer[String]() ++= x
    }

    def handleMousePressed(event: MouseEvent): Unit = {
        if (admin == true) {
            if (event.primaryButtonDown) {
                
                Platform.runLater {
                    MainApp.mainSystem ! Client.UpdateCanvasPressed(event.x, event.y, "Black")
                }
            } else if(event.secondaryButtonDown) {
                Platform.runLater {
                    MainApp.mainSystem ! Client.UpdateCanvasPressed(event.x, event.y, "White")
                }  
            }
        }
    }

    def drawPressed(x: Double, y: Double, colour: String): Unit = {
            if (colour == "Black") {
                Platform.runLater {
                    gc.beginPath()
                    gc.moveTo(x, y)
                    gc.stroke = Color.Black
                    gc.lineWidth = 2.0
                }
            } else if (colour == "White") {
                Platform.runLater {
                    gc.beginPath()
                    gc.moveTo(x, y)
                    gc.stroke = Color.rgb(244,244,244)
                    gc.lineWidth = 20.0
                }
            }
        }

    def handleMouseDragged(event: MouseEvent): Unit = {
        if (admin == true) {
            Platform.runLater {
                MainApp.mainSystem ! Client.UpdateCanvasDragged(event.x, event.y)
            }
        }

    }

    def drawDragged(x: Double, y: Double): Unit = {
            Platform.runLater {
                gc.lineTo(x, y)
                gc.stroke()
            }
    }    

    def handleExit(event: MouseEvent): Unit = {
        MainApp.stage.close()
    }
}

                                