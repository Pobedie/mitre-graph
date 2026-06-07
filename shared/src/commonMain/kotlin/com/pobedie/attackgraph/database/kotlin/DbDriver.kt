package com.pobedie.attackgraph.database.kotlin

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory):  {
    val driver = driverFactory.createDriver()
    val database = Database(driver)

    // Do more work with the database (see below).
}