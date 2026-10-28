package com.example.wellbeing

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Entity(tableName = "records")
data class StoredRecord(@PrimaryKey val id: String, val sealed: String, @ColumnInfo(defaultValue = "0") val revision: Long = 0)
@Entity(tableName = "outbox", indices = [Index(value = ["mutationId"], unique = true)])
data class OutboxRecord(@PrimaryKey(autoGenerate = true) val sequence: Long = 0, val mutationId: String, val recordId: String, val sealed: String)
@Dao
interface RecordDao {
    @Query("SELECT * FROM records") fun observe(): Flow<List<StoredRecord>>
    @Upsert suspend fun put(record: StoredRecord)
    @Query("DELETE FROM records WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM records") suspend fun clear()
    @Query("SELECT * FROM records WHERE id = :id") suspend fun find(id: String): StoredRecord?
    @Insert suspend fun enqueue(record: OutboxRecord)
    @Query("SELECT * FROM outbox ORDER BY sequence LIMIT :limit") suspend fun pending(limit: Int): List<OutboxRecord>
    @Query("DELETE FROM outbox WHERE mutationId IN (:ids)") suspend fun acknowledge(ids: Set<String>)
    @Query("DELETE FROM outbox") suspend fun clearOutbox()
    @Query("DELETE FROM outbox WHERE recordId = :recordId") suspend fun compact(recordId: String)
}
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE records ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS outbox (sequence INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mutationId TEXT NOT NULL, recordId TEXT NOT NULL, sealed TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_outbox_mutationId ON outbox (mutationId)")
    }
}
@Database(entities = [StoredRecord::class, OutboxRecord::class], version = 2, exportSchema = true)
abstract class WellbeingDatabase : RoomDatabase() { abstract fun records(): RecordDao }

class Vault {
    private val alias = "wellbeing.records.v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun seal(id: String, value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    @Synchronized fun open(id: String, value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }
}
fun Entry.json(): JSONObject = JSONObject().put("id", id).put("kind", kind.name).put("at", at).put("title", title).put("amount", amount).put("energy", energy).put("source", source).put("sourceVersion", sourceVersion).put("note", note).put("extra", extra)
fun entryFromJson(j: JSONObject) = Entry(j.getString("id"), Kind.valueOf(j.getString("kind")), j.getLong("at"), j.getString("title"), j.getDouble("amount"), j.getDouble("energy"), j.getString("source"), j.getString("sourceVersion"), j.getString("note"), j.getString("extra"))
class LocalRepository(context: Context, databaseName: String = "wellbeing.db") : JournalRepository, SyncQueue {
    private val db = Room.databaseBuilder(context, WellbeingDatabase::class.java, databaseName).addMigrations(MIGRATION_1_2).build()
    private val vault = Vault()
    override val entries = db.records().observe().map { rows -> rows.map { entryFromJson(JSONObject(vault.open(it.id, it.sealed))) } }
    private suspend fun enqueue(id: String, operation: String, revision: Long, entry: Entry? = null) {
        val mutationId = UUID.randomUUID().toString()
        val payload = JSONObject().put("mutationId", mutationId).put("recordId", id).put("operation", operation).put("localRevision", revision).put("entry", entry?.json()).toString()
        db.records().compact(id)
        db.records().enqueue(OutboxRecord(mutationId = mutationId, recordId = id, sealed = vault.seal(mutationId, payload)))
    }
    override suspend fun save(entry: Entry) = db.withTransaction {
        require(WellnessMath.valid(entry))
        val revision = (db.records().find(entry.id)?.revision ?: 0) + 1
        db.records().put(StoredRecord(entry.id, vault.seal(entry.id, entry.json().toString()), revision))
        if (entry.kind !in setOf(Kind.TIMER, Kind.DEVICE_SNAPSHOT)) enqueue(entry.id, "UPSERT", revision, entry)
    }
    override suspend fun delete(id: String) = db.withTransaction {
        val previous = db.records().find(id)
        db.records().delete(id)
        if (previous != null && entryFromJson(JSONObject(vault.open(id, previous.sealed))).kind !in setOf(Kind.TIMER, Kind.DEVICE_SNAPSHOT)) enqueue(id, "DELETE", previous.revision + 1)
    }
    override suspend fun finishSession(entry: Entry, timerId: String) = db.withTransaction {
        require(timerId in setOf("timer", "focus-timer", "workout-draft"))
        save(entry); db.records().delete(timerId)
    }
    override suspend fun clear() = db.withTransaction { db.records().clear(); db.records().clearOutbox() }
    override suspend fun pending(limit: Int): List<PendingChange> = db.records().pending(limit.coerceIn(1, 100)).map {
        val payload = vault.open(it.mutationId, it.sealed)
        val json = JSONObject(payload)
        PendingChange(it.mutationId, json.getString("recordId"), json.getString("operation"), json.getLong("localRevision"), payload)
    }
    override suspend fun acknowledge(mutationIds: Set<String>) { if (mutationIds.isNotEmpty()) db.records().acknowledge(mutationIds) }
    fun close() = db.close()
}
