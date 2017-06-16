package xyz.ohmycs.bjwiber

import javafx.application.Application
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import tornadofx.View
import tornadofx.plusAssign

class MyView : View() {
    override val root = VBox()

    init {
        root += Button("Press Me")
        root += Label("")
    }
}

class App : tornadofx.App(MyView::class) {

    val greeting: String get() = "Hello world."

    companion object {

        @JvmStatic fun main(args: Array<String>) {
            println(App().greeting)
            val foo = Foo()
            Foo.greet()
            println(foo)
            launch(App::class.java)
        }
    }
}
