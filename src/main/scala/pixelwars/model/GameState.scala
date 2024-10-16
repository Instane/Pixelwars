package pixelwars.model
import scalafx.collections.ObservableBuffer

object GameState {
    var stageChange: Boolean = false
    var masterPrompt: String = _
    
    def randomDrawingPrompt(): String = { 
        var drawingPrompts: Array[String] = Array("Cow", "Cat", "Dog", "Horse", "Snake")
        return drawingPrompts(scala.util.Random.nextInt(drawingPrompts.length))
    }

    def removeConsecutiveDuplicates(arr: ObservableBuffer[String]): ObservableBuffer[String] = {
    // Use foldLeft to accumulate distinct elements while iterating through the original buffer
    arr.foldLeft(ObservableBuffer.empty[String]) { (resultBuffer, currentElement) =>
        // Check if the resultBuffer is not empty and the last element is equal to the current element
        if (resultBuffer.nonEmpty && resultBuffer.last == currentElement) {
            // If consecutive duplicate, do not add to the resultBuffer
            resultBuffer
            } else {
            // Otherwise, add the current element to the resultBuffer
            resultBuffer += currentElement
        }
    }
    }

}