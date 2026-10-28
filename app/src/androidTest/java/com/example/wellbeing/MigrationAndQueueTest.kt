package com.example.wellbeing

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MigrationAndQueueTest {
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),WellbeingDatabase::class.java)
    @Test fun migrationKeepsEncryptedLegacyRecords() {
        val encrypted=Vault().seal("legacy","legacy record contents")
        helper.createDatabase("migration-test",1).apply{execSQL("INSERT INTO records (id,sealed) VALUES (?,?)",arrayOf("legacy",encrypted));close()}
        helper.runMigrationsAndValidate("migration-test",2,true,MIGRATION_1_2).use{db ->
            db.query("SELECT sealed, revision FROM records WHERE id='legacy'").use{cursor->assertTrue(cursor.moveToFirst());assertEquals("legacy record contents",Vault().open("legacy",cursor.getString(0)));assertEquals(0L,cursor.getLong(1))}
            db.query("SELECT COUNT(*) FROM outbox").use{cursor->cursor.moveToFirst();assertEquals(0,cursor.getInt(0))}
        }
    }
    @Test fun queueCompactsEditsAndKeepsOnlyContentFreeTombstone()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="queue-${UUID.randomUUID()}"
        val repository=LocalRepository(context,name)
        try {
            val entry=Entry(id="one",kind=Kind.FOOD,title="Private food",amount=100.0,energy=100.0)
            repository.save(entry);repository.save(entry.copy(energy=200.0))
            assertEquals(1,repository.pending(100).size)
            assertEquals(2L,repository.pending(100).single().localRevision)
            assertEquals(200.0,repository.entries.first().single().energy,0.0)
            repository.delete("one")
            assertTrue(repository.entries.first().isEmpty())
            val deletion=repository.pending(100).single()
            assertEquals("DELETE",deletion.operation)
            assertFalse(deletion.payload.contains("Private food"))
            repository.acknowledge(setOf("unknown"))
            assertEquals(deletion.mutationId,repository.pending(100).single().mutationId)
            repository.clear();assertTrue(repository.pending(100).isEmpty())
        }finally{repository.close();context.deleteDatabase(name)}
    }
}
