package com.pobedie.attackgraph.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pobedie.attackgraph.database.kotlin.DriverFactory
import com.pobedie.attackgraph.settings.UserSettingsDb
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsMigrationTest {
    @Test
    fun migratesUnversionedSettingsAndCanReopen() {
        val file = Files.createTempFile("dm-settings", ".db").toFile()
        try {
            val legacy = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
            legacy.execute(null, "CREATE TABLE user_settings (id INTEGER PRIMARY KEY, llm_url TEXT NOT NULL, llm_api_key TEXT NOT NULL, llm_model TEXT NOT NULL)", 0)
            legacy.execute(null, "INSERT INTO user_settings VALUES (0, 'http://localhost/v1', 'key', 'custom-model')", 0)
            legacy.close()
            repeat(2) {
                val driver = DriverFactory().createPersistentDriver(file.absolutePath)
                try {
                    val settings = UserSettingsDb(driver).user_settingsQueries.getUserSettings().executeAsOne()
                    assertEquals("http://localhost/v1", settings.dm_url)
                    assertEquals("key", settings.dm_api_key)
                    assertEquals("custom-model", settings.dm_model)
                    assertEquals(0.5, settings.dm_confidence_threshold)
                } finally {
                    driver.close()
                }
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun initializesAnEmptyDatabaseFile() {
        val file = Files.createTempFile("dm-settings-empty", ".db").toFile()
        try {
            val driver = DriverFactory().createPersistentDriver(file.absolutePath)
            try {
                val queries = UserSettingsDb(driver).user_settingsQueries
                queries.saveUserSettings("url", "key", "model", 0.8)
                assertEquals(0.8, queries.getUserSettings().executeAsOne().dm_confidence_threshold)
            } finally {
                driver.close()
            }
        } finally {
            file.delete()
        }
    }
}
