package xyz.ohmycs.bjwiber

import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import okhttp3.*
import org.controlsfx.control.StatusBar
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
        api.baseURI = "http://127.0.0.1:22222/v3"
    }
}

class MainView : View() {
    val mainController: MainController by inject()
    var urlFirstField: TextField? by singleAssign()
    var urlSecondField: TextField? by singleAssign()
    var methodComboBox: ComboBox<String>? by singleAssign()
    var webView: WebView? by singleAssign()
    var statusBar: StatusBar? by singleAssign()
    val arguments = mutableListOf(
            Argument(key = "header", value = "HEADER", position = "header"),
            Argument(key = "url", value = "URL", position = "url"),
            Argument(key = "body", value = "BODY", position = "body")
    ).observable()
    var argumentsListView: ListView<Argument> by singleAssign()
    var bodyTypeComboBox: ComboBox<String>? by singleAssign()
    var client: OkHttpClient? by singleAssign()
    var toggleArgumentsVisibleButton: Button? by singleAssign()

    init {
        client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body: RequestBody? = request.body()
            var requestLog = "Request: ${request} "
            if (body is FormBody) {
                requestLog += "Body: " + (0 until body.size()).map { "${body.name(it)}=${body.value(it)}" }.joinToString(",")
            } else {
                requestLog += "Body: " + body
            }
            println(requestLog)
            runLater {
                statusBar!!.text = requestLog
            }
            val response = chain.proceed(request)
            val responseLog = "Response: ${response}"
            println(responseLog)
            runLater {
                statusBar!!.text = responseLog
            }
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
                    statusBar!!.text = arguments.toString()
                }
            }
            toggleArgumentsVisibleButton = button {
                action {
                    argumentsListView.isVisible = !argumentsListView.isVisible
                }
            }
            bodyTypeComboBox = combobox<String> {
                items = listOf("form", "multipart", "json").observable()
                value = "form"
            }
            methodComboBox = combobox<String> {
                items = listOf("get", "post", "patch", "delete", "put").observable()
                value = "get"
            }
            urlFirstField = textfield("http://127.0.0.1:22222/v3") { hgrow = Priority.ALWAYS }
            urlSecondField = textfield("/test/echo") { hgrow = Priority.ALWAYS }
            button {
                graphic = Glyph.create("FontAwesome|" + FontAwesome.Glyph.SEND).color(Color.BLUEVIOLET)
                action {
                    runAsync {
                        val availableArguments = arguments.filter { it.key != null && it.value != null && it.enabled }
                        val urlArguments = availableArguments.filter { it.position == "url" }
                        val headerArguments = availableArguments.filter { it.position == "header" }
                        val bodyArguments = availableArguments.filter { it.position == "body" }
                        val url = HttpUrl.parse(urlFirstField!!.text + urlSecondField!!.text)!!.newBuilder().apply {
                            urlArguments.forEach {
                                this.addQueryParameter(it.key, it.value)
                            }
                        }.build()
                        val body = when (bodyTypeComboBox!!.selectedItem!!) {
                            "form" -> FormBody.Builder().apply {
                                bodyArguments.forEach { this.add(it.key, it.value) }
                            }.build()
                            "multipart" -> if (bodyArguments.isNotEmpty()) MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                                bodyArguments.forEach { this.addFormDataPart(it.key, it.value) }
                            }.build() else FormBody.Builder().build()
                            "json" -> RequestBody.create(MediaType.parse("application/json"), Json.createObjectBuilder().apply {
                                bodyArguments.forEach { this.add(it.key, it.value) }
                            }.build().toString())
                            else -> throw IllegalStateException()
                        }
                        client!!.newCall(Request.Builder().url(url).apply {
                            headerArguments.forEach { this.addHeader(it.key, it.value) }
                            when (methodComboBox!!.selectedItem!!) {
                                "get" -> this.get()
                                "post" -> this.post(body)
                                "patch" -> this.patch(body)
                                "delete" -> this.delete(body)
                                "put" -> this.put(body)
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
                toggleArgumentsVisibleButton!!.graphicProperty().bind(Bindings.`when`(this.visibleProperty())
                        .then(Glyph.create("FontAwesome|" + FontAwesome.Glyph.TOGGLE_UP).color(Color.DEEPSKYBLUE))
                        .otherwise(Glyph.create("FontAwesome|" + FontAwesome.Glyph.TOGGLE_DOWN).color(Color.DEEPSKYBLUE)))
                prefHeightProperty().bind(Bindings.size(items).multiply(50))
                cellFormat {
                    val currentArgument = it
                    graphic = hbox {
                        fun updateTextColor() {
                            this.children.forEach {
                                it.style = "-fx-text-inner-color: ${when (currentArgument.position) {
                                    "header" -> "red"
                                    "url" -> "green"
                                    else -> "blue"
                                }};"
                            }
                        }
                        spacing = 3.0
                        combobox<String> {
                            items = listOf("header", "url", "body").observable()
                            bind(it.positionProperty)
                            setOnAction { updateTextColor() }
                        }
                        textfield {
                            hgrow = Priority.SOMETIMES
                            bind(it.keyProperty)
                        }
                        textfield {
                            hgrow = Priority.ALWAYS
                            bind(it.valueProperty)
                        }
                        togglebutton {
                            selectedProperty().bindBidirectional(it.enabledProperty)
                            graphicProperty().bind(Bindings.`when`(it.enabledProperty)
                                    .then(Glyph.create("FontAwesome|" + FontAwesome.Glyph.TOGGLE_ON).color(Color.GREEN))
                                    .otherwise(Glyph.create("FontAwesome|" + FontAwesome.Glyph.TOGGLE_OFF).color(Color.GREEN)))
                            action {
                                this.parent.getChildList()!!.filter { it != this }.forEach { it.isDisable = !it.isDisable }
                            }
                        }

                        button(graphic = Glyph.create("FontAwesome|" + FontAwesome.Glyph.REMOVE).color(Color.ORANGERED)) {
                            action {
                                arguments.remove(it)
                            }
                        }

                        updateTextColor()
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
                        engine.executeScript("console.log('I am a log from javascript console :)')")
                    }
                }
                val url = MainView::class.java.getResource("/index.html").toExternalForm()
                engine.load(url)
            }
        }

        bottom = StatusBar().apply {
            text = "Ready."
        }
        statusBar = bottom as StatusBar
    }
}

class JavaBridge {
    fun log(text: String) {
        println(text)
    }
}

class MainStyle : Stylesheet() {
    init {
        textField {
            //            backgroundColor = MultiValue(arrayOf(Color(1.0, 1.0, 1.0, 0.8)))
        }
    }
}

class App : tornadofx.App(MainView::class, MainStyle::class) {
    init {
    }
}

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}