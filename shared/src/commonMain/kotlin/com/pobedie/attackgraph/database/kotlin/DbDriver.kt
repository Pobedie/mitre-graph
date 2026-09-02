package com.pobedie.attackgraph.database.kotlin

import app.cash.sqldelight.db.SqlDriver
import com.pobedie.attackgraph.database.Atlas
import com.pobedie.attackgraph.database.Mitigation
import com.pobedie.attackgraph.database.Relationship
import com.pobedie.attackgraph.database.Technique
import com.pobedie.attackgraph.settings.UserSettingsDb

expect class DriverFactory {
    fun createDriver(): SqlDriver
    fun createPersistentDriver(fileName: String): SqlDriver
}

fun createAtlasDatabase(driverFactory: DriverFactory): Atlas {
    val driver = driverFactory.createDriver()
    return Atlas(
        driver = driver,
        mitigationAdapter = Mitigation.Adapter(
            lifecycle_phasesAdapter = StringListAdapter,
            categoriesAdapter = StringListAdapter
        ),
        relationshipAdapter = Relationship.Adapter(
            leads_toAdapter = StringListAdapter
        ),
        techniqueAdapter = Technique.Adapter(
            platformsAdapter = StringListAdapter
        )
    )
}

fun createSettingsDatabase(driverFactory: DriverFactory): UserSettingsDb {
    val driver = driverFactory.createPersistentDriver("user_settings.db")
    return UserSettingsDb(driver)
}
