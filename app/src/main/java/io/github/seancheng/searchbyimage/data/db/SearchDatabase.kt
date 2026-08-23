package io.github.seancheng.searchbyimage.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CustomEngineEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SearchDatabase : RoomDatabase() {
    abstract fun customEngineDao(): CustomEngineDao
}
