package xyz.ohmycs.bjwiber

import kotlin.coroutines.experimental.*

import javax.script.ScriptEngineManager

/**
 * Created by BaiJiFeiLong@gmail.com on 2017/6/17.
 */

data class Thing(var name: String, val age: Int)


fun main(args: Array<String>) {
    val (name, age) = Thing("xx", 1)
    listOf("").map {  }

    println("$name $age")

}