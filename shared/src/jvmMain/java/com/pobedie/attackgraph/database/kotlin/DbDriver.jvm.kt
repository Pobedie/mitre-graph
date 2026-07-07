package com.pobedie.attackgraph.database.kotlin

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pobedie.attackgraph.database.Atlas
import com.pobedie.attackgraph.settings.UserSettingsDb
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

    actual fun createPersistentDriver(fileName: String): SqlDriver {
        val databaseFile = java.io.File(fileName)
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        if (!databaseFile.exists()) {
            UserSettingsDb.Schema.create(driver)
        }
        return driver
    }
}
