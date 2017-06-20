package xyz.ohmycs.bjwiber

import com.sun.deploy.net.HttpRequest
import javafx.application.Application
import javafx.beans.binding.Binding
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.Background
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import okhttp3.*
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import java.io.StringReader
import javax.json.Json
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


class Argument(key: String? = null, value: String? = null, enabled: Boolean = true, position: String = "url") {
    val keyProperty = SimpleStringProperty(this, "key", key)
    val valueProperty = SimpleStringProperty(this, "value", value)
    val enabledProperty = SimpleBooleanProperty(this, "enabled", enabled)
    val positionProperty = SimpleStringProperty(this, "position", position)

    var key by keyProperty
    var value by valueProperty
    var enabled by enabledProperty
    var position by positionProperty

    override fun toString(): String {
        return "Argument {key:$key, value:$value, enabled:$enabled, position:$position}"
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
    var urlFirstField: TextField? by singleAssign()
    var urlSecondField: TextField? by singleAssign()
    var methodComboBox: ComboBox<String>? by singleAssign()
    var webView: WebView? by singleAssign()
    var statusLabel: Label? by singleAssign()
    val arguments = (0 until 4).toMutableList().map { Argument() }.observable()
    var argumentsListView: ListView<Argument> by singleAssign()
    var client: OkHttpClient? by singleAssign()

    init {
        client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val body: RequestBody? = request.body()
                    print("Request: ${request} ")
                    if (body is FormBody) {
                        print("Body: " + (0 until body.size()).map { "${body.name(it)}=${body.value(it)}" }.joinToString(","))
                    } else {
                        print("Body: " + body)
                    }
                    println()
                    val response = chain.proceed(request)
                    println("Response: ${response}")
                    return@addInterceptor response
                }.build()
    }

    fun renderCode(code: String) {
        val window = webView!!.engine.executeScript("window") as JSObject
        window.call("renderCode", code)
    }

    override val root = borderpane {
        top = hbox {
            padding = Insets(5.0)
            spacing = 3.0
            button {
                graphic = Glyph.create("FontAwesome|" + FontAwesome.Glyph.PLUS).color(Color.FORESTGREEN)
                action {
                    if (argumentsListView.isVisible) arguments.add(Argument()) else argumentsListView.isVisible = true
                }
            }
            button {
                graphic = Glyph.create("FontAwesome|" + FontAwesome.Glyph.AMBULANCE).color(Color.RED)
                action {
                    val code = "from datetime import datetime\nprint 'xx'"
                    renderCode(code)
                    statusLabel!!.text = arguments.toString()
                }
            }
            button {
                graphic = Glyph.create("FontAwesome|" + FontAwesome.Glyph.TOGGLE_DOWN).color(Color.DEEPSKYBLUE)
                action {
                    argumentsListView.isVisible = !argumentsListView.isVisible
                }
            }
            combobox<String> {
                items = listOf("form", "multipart", "json").observable()
                value = "form"
            }
            methodComboBox = combobox<String> {
                items = listOf("get", "post", "patch", "delete", "put", "option").observable()
                value = "get"
            }
            urlFirstField = textfield("http://127.0.0.1:22222/v3") { hgrow = Priority.ALWAYS }
            urlSecondField = textfield("/users") { hgrow = Priority.ALWAYS }
            button {
                graphic = Glyph.create("FontAwesome|" + FontAwesome.Glyph.SEND).color(Color.BLUEVIOLET)
                action {
                    runAsync {
                        val availableArguments = arguments.filter { it.key != null && it.value != null && it.enabled }
                        val urlArguments = availableArguments.filter { it.position == "url" }
                        val url = HttpUrl.parse(urlFirstField!!.text + urlSecondField!!.text)!!.newBuilder().apply {
                            urlArguments.forEach {
                                this.addQueryParameter(it.key, it.value)
                            }
                        }.build()
                        val headerArguments = availableArguments.filter { it.position == "header" }
                        val bodyArguments = availableArguments.filter { it.position == "body" }
                        val jsonArguments = availableArguments.filter { it.position == "json" }
                        println("Request: $url")
                        val api = mainController.api
                        client!!.newCall(Request.Builder().url(url).apply {
                            when (methodComboBox!!.selectedItem!!) {
                                "get" -> this.get()
                                "post" -> this.post(FormBody.Builder().apply {
                                    bodyArguments.forEach {
                                        this.add(it.key, it.value)
                                    }
                                }.build())
                            }
                        }.build()).execute()
                    } ui {
                        if (it.body()?.contentType()?.subtype() == "json") {

                            val txt = it.body()?.string()
                            val code = Json.createReader(StringReader(txt)).read().toPrettyString()
                            renderCode(code)
                        } else
                            renderCode(it.body()!!.string())
                    }
                }
            }
        }


        center = vbox {
            argumentsListView = listview(arguments) {
                managedProperty().bind(this.visibleProperty())
                prefHeightProperty().bind(Bindings.size(items).multiply(50))
                cellFormat {
                    graphic = hbox {
                        spacing = 3.0
                        combobox<String> {
                            items = listOf("header", "url", "body").observable()
                            bind(it.positionProperty)
                        }
                        val a = textfield {
                            hgrow = Priority.SOMETIMES
                            bind(it.keyProperty)
                        }
                        textfield {
                            hgrow = Priority.ALWAYS
                            bind(it.valueProperty)
                        }
                        togglebutton {
                            selectedProperty().bindBidirectional(it.enabledProperty)
                            textProperty().bind(selectedProperty().stringBinding { if (it == true) "Enabled" else "Disabled" })
                            action {
                                println(it)
                                a.isDisable = true
                            }
                        }

                        button(graphic = Glyph.create("FontAwesome|" + FontAwesome.Glyph.REMOVE).color(Color.ORANGERED)) {
                            action {
                                arguments.remove(it)
                            }
                        }
                    }
                }
            }
            webView = webview {
                vgrow = Priority.ALWAYS
                engine.setOnAlert { println("Alert: $it") }
                engine.loadWorker.stateProperty().addListener { _, _, newValue ->
                    println("New value: $newValue");
                    if (newValue == Worker.State.SUCCEEDED) {
                        println("Finished loading");

                        val window: JSObject = engine.executeScript("window") as JSObject
                        window.setMember("java", JavaBridge())
                        engine.executeScript("window.console.log = function(message) { java.log(message) }")
                        engine.executeScript("console.log('xxx')")
                    }
                }
                val url = MainView::class.java.getResource("/index.html").toExternalForm()
                engine.load(url)
            }
        }

        statusLabel = label("Ready.")
        bottom = statusLabel
    }
}

class JavaBridge {
    fun log(text: String) {
        println(text)
    }
}


class App : tornadofx.App(MainView::class) {
    init {
    }
}

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}