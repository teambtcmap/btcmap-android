package db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

fun testDb(): Database {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Database.Schema.create(driver)
    return database(driver)
}