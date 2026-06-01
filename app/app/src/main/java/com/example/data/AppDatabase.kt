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

// --- Room Database Entities ---

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mind_maps")
data class MindMapItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val structureJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quizzes")
data class QuizItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val optionsJson: String, // Delimited or JSON string array
    val correctAnswer: String,
    val userSelectedAnswer: String? = null,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "platform_records")
data class PlatformRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "MEDIA_IMAGE", "MEDIA_VIDEO", "PERMISSION_GRANTED", "PERMISSION_DENIED", "ANALYTICS"
    val title: String,
    val details: String,
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// --- Data Access Object ---

@Dao
interface StudyDao {
    // Chat Message Queries
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllChatMessages(): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()

    // Mind Maps Queries
    @Query("SELECT * FROM mind_maps ORDER BY createdAt DESC")
    suspend fun getAllMindMaps(): List<MindMapItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindMap(mindMap: MindMapItem)

    @Query("DELETE FROM mind_maps WHERE id = :id")
    suspend fun deleteMindMapById(id: Int)

    // Quiz / Flashcard Queries
    @Query("SELECT * FROM quizzes ORDER BY timestamp DESC")
    suspend fun getAllQuizzes(): List<QuizItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizItem)

    @Query("UPDATE quizzes SET userSelectedAnswer = :selected WHERE id = :id")
    suspend fun updateUserSelectedAnswer(id: Int, selected: String)

    @Query("DELETE FROM quizzes")
    suspend fun deleteAllQuizzes()

    // Platform Logs Queries
    @Query("SELECT * FROM platform_records ORDER BY timestamp DESC")
    suspend fun getAllPlatformRecords(): List<PlatformRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatformRecord(record: PlatformRecord)

    @Query("DELETE FROM platform_records WHERE id = :id")
    suspend fun deletePlatformRecordById(id: Int)
}

// --- Database Configuration ---

@Database(
    entities = [ChatMessage::class, MindMapItem::class, QuizItem::class, PlatformRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "borei_study_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
