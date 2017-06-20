package xyz.ohmycs.bjwiber

import okhttp3.*
import tornadofx.*
import java.io.StringReader
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonObjectBuilder
import javax.json.JsonValue
import kotlin.coroutines.experimental.*

import javax.script.ScriptEngineManager
import kotlin.properties.Delegates

/**
 * Created by BaiJiFeiLong@gmail.com on 2017/6/17.
 */

data class Thing(var name: String, val age: Int)

var name: String by Delegates.observable("NuLL") { property, oldValue, newValue ->
    println("$property : $oldValue => $newValue")
}


fun main(args: Array<String>) {
    val client = OkHttpClient()
    val a = Request.Builder().url(HttpUrl.parse("http://www.baidu.com/a/b/c?d=e&f=g"))
    val resp = client.newCall(Request.Builder().url("http://127.0.0.1:22222/v3/users").post(
            MultipartBody.Builder().addFormDataPart("a", "b").addPart(MultipartBody.Part.createFormData("g", "h")).setType(MultipartBody.FORM).build()
    ).build()).execute()
    val json = Json.createReader(StringReader(resp.body()!!.string())).read() as JsonObject
    println(resp)
    print(json)
    `System`.`out`.`println`()
}