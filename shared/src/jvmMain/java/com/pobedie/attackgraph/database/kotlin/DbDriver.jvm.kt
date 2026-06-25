package com.pobedie.attackgraph.database.kotlin

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pobedie.attackgraph.database.Atlas
import java.util.Properties

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(
//            url = "jdbc:sqlite:atlas.db", // for debug purposes
            url = JdbcSqliteDriver.IN_MEMORY,
            properties = Properties(),
            schema = Atlas.Schema,
        )
        return driver
    }
}
