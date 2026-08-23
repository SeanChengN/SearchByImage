package io.github.seancheng.searchbyimage.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_engines")
data class CustomEngineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val endpoint: String,
    val fileField: String,
    val staticFieldsJson: String,
    val resultUrlJsonField: String,
    val enabled: Boolean,
    val sortOrder: Int,
)
