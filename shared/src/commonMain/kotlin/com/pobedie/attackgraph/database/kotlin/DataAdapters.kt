package com.pobedie.attackgraph.database.kotlin

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object StringListAdapter : ColumnAdapter<List<String>, String> {
    private val json = Json { ignoreUnknownKeys = true }

    override fun encode(value: List<String>): String = json.encodeToString(value)
    override fun decode(databaseValue: String): List<String> = json.decodeFromString(databaseValue)
}