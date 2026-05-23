package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

@JsonClass(generateAdapter = true)
data class CivitaiModelResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val type: String,
    val modelVersions: List<CivitaiVersion>
)

@JsonClass(generateAdapter = true)
data class CivitaiVersion(
    val id: Long,
    val name: String,
    val downloadUrl: String,
    val trainedWords: List<String>?,
    val images: List<CivitaiImage>?
)

@JsonClass(generateAdapter = true)
data class CivitaiImage(
    val url: String
)

interface CivitaiApiService {
    @GET("api/v1/models/{id}")
    suspend fun getModelDetails(
        @Path("id") id: Long
    ): CivitaiModelResponse
}

object CivitaiClient {
    private const val BASE_URL = "https://civitai.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: CivitaiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CivitaiApiService::class.java)
    }

    /**
     * Extracts model ID from various Civitai URLs:
     * e.g., https://civitai.com/models/12345
     * e.g., https://civitai.com/models/12345/some-slug
     * e.g., or just "12345"
     */
    fun extractModelId(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.all { it.isDigit() }) return trimmed.toLongOrNull()
        
        val pattern = Regex(""".*civitai\.com/models/(\d+).*""")
        val match = pattern.matchEntire(trimmed) ?: Regex(".*/models/(\\d+).*").find(trimmed)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }
}
