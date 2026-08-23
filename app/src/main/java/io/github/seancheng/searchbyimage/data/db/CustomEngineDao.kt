package io.github.seancheng.searchbyimage.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEngineDao {
    @Query("SELECT * FROM custom_engines ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CustomEngineEntity>>

    @Query("SELECT * FROM custom_engines WHERE id = :id LIMIT 1")
    suspend fun find(id: Long): CustomEngineEntity?

    @Upsert
    suspend fun save(entity: CustomEngineEntity): Long

    @Delete
    suspend fun delete(entity: CustomEngineEntity)
}
