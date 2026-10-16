package com.example.aiinterviewer.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val salt: String,
    val passwordHash: String
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val timestamp: Long,
    val category: String,
    val questionCount: Int,
    val overall: Int,            // percentage 0..100
    val topicScores: String      // "Android:7.5;Database:4.0"
)

@Dao
interface UserDao {
    @Insert
    suspend fun insert(u: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE userId = :uid ORDER BY timestamp DESC")
    fun forUser(uid: Long): Flow<List<SessionEntity>>

    @Insert
    suspend fun insert(s: SessionEntity)

    @Query("DELETE FROM sessions WHERE userId = :uid")
    suspend fun clear(uid: Long)
}

@Database(entities = [UserEntity::class, SessionEntity::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun dao(): SessionDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "interviewer.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
