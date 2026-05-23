package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CivitaiModel::class, GenerationItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun civitaiModelDao(): CivitaiModelDao
    abstract fun generationDao(): GenerationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "takhayyal_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.civitaiModelDao())
                }
            }
        }

        suspend fun populateDatabase(dao: CivitaiModelDao) {
            // Check if models already exist
            if (dao.getCount() == 0) {
                val presets = listOf(
                    CivitaiModel(
                        name = "DreamShaper v8",
                        type = "Checkpoint",
                        baseModel = "SD 1.5",
                        triggerWords = "photorealistic, cinematic, digital art, highly detailed, masterpieces",
                        downloadUrl = "https://civitai.com/api/v1/model-versions/128713",
                        description = "مواد رسومي وصور فنية واقعية للغاية وثلاثية الأبعاد بأسلوب فريد.",
                        rating = 4.8f,
                        imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=400",
                        isDownloaded = true, // Preloaded for great offline prototype experience
                        isCustom = false
                    ),
                    CivitaiModel(
                        name = "EpicRealism XL",
                        type = "Checkpoint",
                        baseModel = "SDXL 1.0",
                        triggerWords = "photoreal, 8k skin texture, raw dslr photo, high contrast, warm lighting",
                        downloadUrl = "https://civitai.com/api/v1/model-versions/289073",
                        description = "النموذج الأقوى لالتقاط الوجوه والتفاصيل البشرية والإضاءة السينمائية الحقيقية.",
                        rating = 4.9f,
                        imageUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=400",
                        isDownloaded = true,
                        isCustom = false
                    ),
                    CivitaiModel(
                        name = "Rev Animated v2.0",
                        type = "Checkpoint",
                        baseModel = "SD 1.5",
                        triggerWords = "anime style, 3d fantasy render, vibrant colors, magical environment, complex detail",
                        downloadUrl = "https://civitai.com/api/v1/model-versions/46846",
                        description = "نموذج رائع لتوليد شخصيات الأنمي، الفانتازيا والأجواء الخيالية النابضة بالألوان.",
                        rating = 4.7f,
                        imageUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&q=80&w=400",
                        isDownloaded = false,
                        isCustom = false
                    ),
                    CivitaiModel(
                        name = "ToonYou Anime",
                        type = "Checkpoint",
                        baseModel = "SD 1.5",
                        triggerWords = "cell shaded anime, 2d cartoon, cute, illustration, outline",
                        downloadUrl = "https://civitai.com/api/v1/model-versions/125411",
                        description = "إنتاج رسومات ثنائية الأبعاد كلاسيكية وتصاميم كرتون لطيفة عالية التميز.",
                        rating = 4.6f,
                        imageUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&q=80&w=400",
                        isDownloaded = false,
                        isCustom = false
                    ),
                    CivitaiModel(
                        name = "AnimateDiff Motion V3",
                        type = "Motion Module",
                        baseModel = "SD 1.5",
                        triggerWords = "smooth motion, cinematic movement, high frame rate, cinematic pans",
                        downloadUrl = "https://civitai.com/api/v1/model-versions/244321",
                        description = "موديل مخصص لإنتاج حركات الفيديو الفيزيائية الناعمة والتنقلات الكاميرية الطبيعية.",
                        rating = 4.8f,
                        imageUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&q=80&w=400",
                        isDownloaded = false,
                        isCustom = false
                    )
                )
                for (preset in presets) {
                    dao.insertModel(preset)
                }
            }
        }
    }
}
