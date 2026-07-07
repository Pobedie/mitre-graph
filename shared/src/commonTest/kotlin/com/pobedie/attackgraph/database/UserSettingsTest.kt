package com.pobedie.attackgraph.database

import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.core.entity.UserSettings
import com.pobedie.attackgraph.database.kotlin.DriverFactory
import com.pobedie.attackgraph.database.kotlin.createAtlasDatabase
import com.pobedie.attackgraph.database.kotlin.createSettingsDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserSettingsTest {

    @Test
    fun testSaveAndGetUserSettings() {
        val driverFactory = DriverFactory()
        val database = createAtlasDatabase(driverFactory)
        val settingsDatabase = createSettingsDatabase(driverFactory)
        val repository = MainRepository(database, settingsDatabase)

        val settings = UserSettings(
            llmUrl = "https://api.openai.com/v1",
            llmApiKey = "sk-12345",
            llmModel = "gpt-4"
        )

        repository.saveUserSettings(settings)

        val retrievedSettings = repository.getUserSettings()

        assertNotNull(retrievedSettings)
        assertEquals(settings.llmUrl, retrievedSettings.llmUrl)
        assertEquals(settings.llmApiKey, retrievedSettings.llmApiKey)
        assertEquals(settings.llmModel, retrievedSettings.llmModel)
    }
}
