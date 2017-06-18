package xyz.ohmycs.bjwiber

import javafx.application.Application
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Worker
import javafx.scene.Parent
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import org.fxmisc.richtext.CodeArea
import sun.security.util.PendingException
import tornadofx.*
import javax.json.JsonArray
import javax.json.JsonObject

class User : JsonModel {
    var id: Int? by property<Int>()

    var nickname: String? by property<String>()

    override fun updateModel(json: JsonObject) {
        with(json) {
            id = int("id")
            nickname = string("nickname")
        }
    }
}

class MainController : Controller() {
    val api: Rest by inject()
    val lst = mutableListOf<User>().observable()

    fun getUsers(): ObservableList<User> {
        lst.clear()
        return (api.get("/users").one()["data"] as JsonArray).toModel()
    }

    init {
        api.baseURI = "http://api.iguitar.me:2525/v3"
    }
}

class MainView : View() {
    val mainController: MainController by inject()
    var urlField: TextField? by singleAssign()
    var resultField: TextArea? by singleAssign()
    var webView: WebView? by singleAssign()

    fun renderCode(code: String) {
        val window = webView!!.engine.executeScript("window") as JSObject
        window.call("renderCode", code)
    }

    override val root = borderpane {
        top = hbox {
            spacing = 3.0
            this@MainView.urlField = textfield("http://www.baidu.com") { hgrow = Priority.ALWAYS }
            combobox<String> {
                items = listOf("get", "post", "patch", "delete", "put", "option").observable()
                value = "post"
            }
            button("send") {
                action {
                    runAsync {
                        mainController.api.get(this@MainView.urlField!!.text)
                    } ui {
                        this@MainView.resultField!!.appendText(it.text())
                        println("Code = " + it.text())
                        renderCode(it.text()!!)
                    }
                }
            }
            button("two") {
                action {
                    val code = "from datetime import datetime\nprint 'xx'"
                    renderCode(code)
                }
            }
        }


        center = vbox {
            squeezebox {
                fold("Arguments") {
                    listview(mutableListOf("Foo", "Bar", "Baz").observable())
                }
                fold("Result", expanded = true) {
                    this@MainView.resultField = textarea()
                }
            }
            webView = webview {
                val url = MainView::class.java.getResource("/index.html").toExternalForm()
                engine.setOnAlert { println("Alert: $it") }
                engine.loadWorker.stateProperty().addListener { _, _, newValue ->
                    println("New value: $newValue)}");
                    if (newValue == Worker.State.SUCCEEDED) {
                        println("Finished loading");

                        val window: JSObject = engine.executeScript("window") as JSObject
                        window.setMember("java", JavaBridge())
                        engine.executeScript("window.console.log = function(message) { java.log(message) }")
                        engine.executeScript("console.log('xxx')")
                    }
                }
                engine.load(url)

            }
        }

    }
}

class JavaBridge {
    fun log(text: String) {
        println(text)
    }
}


class App : tornadofx.App(MainView::class)

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}