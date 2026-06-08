package com.pobedie.attackgraph.database.kotlin

import app.cash.sqldelight.db.SqlDriver
import com.pobedie.attackgraph.database.Atlas
import com.pobedie.attackgraph.database.Mitigation
import com.pobedie.attackgraph.database.Relationship
import com.pobedie.attackgraph.database.Technique

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Atlas {
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
