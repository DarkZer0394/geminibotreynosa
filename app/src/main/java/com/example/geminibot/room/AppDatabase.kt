package com.example.geminibot.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.geminibot.model.ChatModel

@Database(entities = [ChatModel::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun chatDao(): ChatDao
}