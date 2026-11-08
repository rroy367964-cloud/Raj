package com.example.data.local

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

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "user" or "mahi"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionExecuted: String? = null,
    val language: String = "en"
)

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface MahiDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 100")
    fun getRecentConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversations")
    suspend fun clearConversations()

    @Query("SELECT * FROM user_memories ORDER BY lastUpdated DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity): Long

    @Query("DELETE FROM user_memories WHERE `key` = :key")
    suspend fun deleteMemoryByKey(key: String)

    @Query("DELETE FROM user_memories")
    suspend fun clearMemories()
}

@Database(
    entities = [ConversationEntity::class, UserMemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MahiDatabase : RoomDatabase() {
    abstract fun mahiDao(): MahiDao

    companion object {
        @Volatile
        private var INSTANCE: MahiDatabase? = null

        fun getInstance(context: Context): MahiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahiDatabase::class.java,
                    "mahi_assistant.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
