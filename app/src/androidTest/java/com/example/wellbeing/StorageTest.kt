package com.example.wellbeing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@RunWith(AndroidJUnit4::class)
class StorageTest {
    @Test fun vaultRoundTripAndTamperRejection() {
        val vault = Vault()
        val cipher = vault.seal("test-record", "private health record")
        assertFalse(cipher.contains("private"))
        assertEquals("private health record", vault.open("test-record", cipher))
        assertTrue(runCatching { vault.open("wrong-record", cipher) }.isFailure)
        assertTrue(runCatching { vault.open("test-record", cipher.dropLast(4) + "AAAA") }.isFailure)
    }
    @Test fun roomUpsertDeduplicatesAndDeleteRemoves() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, WellbeingDatabase::class.java).build()
        try {
            val dao = db.records()
            val vault = Vault()
            val record = Entry(id = "same-mutation", kind = Kind.FOOD, title = "Rice", amount = 150.0, energy = 195.0)
            val stored = StoredRecord(record.id, vault.seal(record.id, record.json().toString()))
            dao.put(stored); dao.put(stored)
            assertEquals(1, dao.observe().first().size)
            val decoded = entryFromJson(org.json.JSONObject(vault.open(stored.id, dao.observe().first().single().sealed)))
            assertEquals(record, decoded)
            dao.delete(record.id)
            assertTrue(dao.observe().first().isEmpty())
        } finally { db.close() }
    }
}
