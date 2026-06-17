package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A single chat message. Every message belongs to exactly one conversation
 * (a 1:1 chat or the family group chat) via [conversationId].
 *
 * The primary key is a stable client-generated string id so the same message
 * synced from another device is de-duplicated (REPLACE) instead of duplicated.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Dao
interface FamilyDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Database(
    entities = [MessageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class FamilyDatabase : RoomDatabase() {
    abstract val dao: FamilyDao

    companion object {
        @Volatile
        private var INSTANCE: FamilyDatabase? = null

        fun getDatabase(context: Context): FamilyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FamilyDatabase::class.java,
                    "family_messenger_database",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
