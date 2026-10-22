package com.pobedie.attackgraph.database.kotlin

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
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
        val hasSettings = driver.executeQuery(null,
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'user_settings'",
            { cursor -> QueryResult.Value(cursor.next().value) }, 0
        ).value
        if (!hasSettings) {
            UserSettingsDb.Schema.create(driver)
        } else {
            val version = driver.executeQuery(null, "PRAGMA user_version",
                { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getLong(0) ?: 0L)
                }, 0
            ).value
            // Older releases created the table without recording a schema version.
            if (version < UserSettingsDb.Schema.version) {
                UserSettingsDb.Schema.migrate(driver, version.coerceAtLeast(1L), UserSettingsDb.Schema.version)
            }
        }
        driver.execute(null, "PRAGMA user_version = ${UserSettingsDb.Schema.version}", 0)
        return driver
    }
}
