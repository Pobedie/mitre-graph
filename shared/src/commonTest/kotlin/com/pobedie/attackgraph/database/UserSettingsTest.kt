package com.pobedie.attackgraph.database

import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.core.entity.UserSettings
import com.pobedie.attackgraph.database.kotlin.DriverFactory
import com.pobedie.attackgraph.database.kotlin.createAtlasDatabase
import com.pobedie.attackgraph.database.kotlin.createSettingsDatabase
import kotlinx.coroutines.runBlocking
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

    @Test
    fun testGetTacticsWithTechniquesReturnsAllTechniques() {
        val driverFactory = DriverFactory()
        val database = createAtlasDatabase(driverFactory)
        val settingsDatabase = createSettingsDatabase(driverFactory)
        val repository = MainRepository(database, settingsDatabase)

        database.transaction {
            database.tacticsQueries.insertTactic("T1", "Tactic 1", "Desc 1", "date", "date", 1L)
            database.tacticsQueries.insertTactic("T2", "Tactic 2", "Desc 2", "date", "date", 2L)

            database.techniqueQueries.insertTechnique("Tech1", "Tech 1", "Desc 1", "date", "date", emptyList(), "Realized", "T1")
            database.techniqueQueries.insertTechnique("Tech2", "Tech 2", "Desc 2", "date", "date", emptyList(), "Realized", "T2")
        }

        runBlocking {
            val (techniques, tactics) = repository.getTacticsWithTechniques()
            assertEquals(2, tactics.size)
            assertEquals(2, techniques.size)
            assertEquals("Tech1", techniques[0].id)
            assertEquals("Tech2", techniques[1].id)
        }
    }
}
