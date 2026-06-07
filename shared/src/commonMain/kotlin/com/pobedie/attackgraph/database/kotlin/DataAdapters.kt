package com.pobedie.attackgraph.database.kotlin

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object StringListAdapter {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(value: List<String>): String = json.encodeToString(value)
    fun decode(databaseValue: String): List<String> = json.decodeFromString(databaseValue)
}