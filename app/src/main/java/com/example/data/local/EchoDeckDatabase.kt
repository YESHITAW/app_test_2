package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class EchoTypeConverters {
    @TypeConverter
    fun fromGreetingType(value: GreetingType): String = value.name

    @TypeConverter
    fun toGreetingType(value: String): GreetingType =
        runCatching { GreetingType.valueOf(value) }.getOrDefault(GreetingType.TTS)

    @TypeConverter
    fun fromScreeningAction(value: ScreeningAction): String = value.name

    @TypeConverter
    fun toScreeningAction(value: String): ScreeningAction =
        runCatching { ScreeningAction.valueOf(value) }.getOrDefault(ScreeningAction.ALLOW)
}

@Database(
    entities = [
        VoicemailMessageEntity::class,
        GreetingEntity::class,
        ScreeningRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(EchoTypeConverters::class)
abstract class EchoDeckDatabase : RoomDatabase() {
    abstract fun echoDeckDao(): EchoDeckDao

    companion object {
        @Volatile
        private var INSTANCE: EchoDeckDatabase? = null

        fun getInstance(context: Context): EchoDeckDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EchoDeckDatabase::class.java,
                    "echodeck_database.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
