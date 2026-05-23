package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CivitaiModelDao {
    @Query("SELECT * FROM models ORDER BY isCustom ASC, id DESC")
    fun getAllModels(): Flow<List<CivitaiModel>>

    @Query("SELECT * FROM models WHERE isDownloaded = 1")
    fun getDownloadedModels(): Flow<List<CivitaiModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: CivitaiModel)

    @Update
    suspend fun updateModel(model: CivitaiModel)

    @Delete
    suspend fun deleteModel(model: CivitaiModel)

    @Query("SELECT COUNT(*) FROM models")
    suspend fun getCount(): Int
}

@Dao
interface GenerationDao {
    @Query("SELECT * FROM generations ORDER BY timestamp DESC")
    fun getAllGenerations(): Flow<List<GenerationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneration(generation: GenerationItem): Long

    @Update
    suspend fun updateGeneration(generation: GenerationItem)

    @Delete
    suspend fun deleteGeneration(generation: GenerationItem)

    @Query("UPDATE generations SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)
}
