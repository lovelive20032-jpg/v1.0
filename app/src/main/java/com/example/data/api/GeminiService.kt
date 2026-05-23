package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class ExpandedPromptResponse(
    val promptEnglish: String,
    val promptArabicExpanded: String,
    val negativePrompt: String,
    val suggestedAspect: String = "1:1"
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val expandedPromptAdapter = moshi.adapter(ExpandedPromptResponse::class.java)

    /**
     * Call Gemini to expand Arabic/English input into an image/video prompt package.
     */
    suspend fun expandPrompt(
        userInput: String, 
        modelType: String, // "IMAGE" or "VIDEO"
        modelTriggerWords: String = ""
    ): ExpandedPromptResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is not set, using offline creator")
            return@withContext getOfflineExpandedPrompt(userInput, modelType, modelTriggerWords)
        }

        val systemInstruction = """
            أنت مساعد مبدع لتوليد وتوسيع مطالبات نماذج الذكاء الاصطناعي (Stable Diffusion & Veo).
            مهمتك تقتصر على تحويل فكرة المستخدم البسيطة (سواء بالعربية أو الإنجليزية) إلى موصوف صور/فيديو احترافي ومفصل.
            
            سوف تعيد كائن JSON صالح فقط بدون أي وسوم markdown أو علامات اقتباس إضافية.
            البيان التالي هو بنية الكود المطلوبة بالضبط:
            {
               "promptEnglish": "وصف مفصل بالإنجليزية يتضمن الألوان، زوايا الكاميرا، جودة الإضاءة، والأسلوب الفني ومدمج معه الكلمات المفتاحية للموديل: ${modelTriggerWords}",
               "promptArabicExpanded": "ترجمة وتفصيل مبدع ومفسر للفكرة باللغة العربية بجمالية أدبية ممتازة تصف الصورة بجاذبية",
               "negativePrompt": "الميزات غير المرغوبة مثل: blurry, bad anatomy, deformed, worst quality, low resolution",
               "suggestedAspect": "نسبة الأبعاد المقترحة بناء على الفكرة: 16:9 أو 9:16 أو 1:1"
            }
            أجب بهذا الكائن فقط.
        """.trimIndent()

        val prompt = "صنف ورتب هذه الفكرة لتوليد ${if(modelType == "VIDEO") "فيديو سينمائي متحرك" else "صورة فنية ممتازة"}: \"$userInput\""

        try {
            // Build the Retrofit/OkHttp request manually matching custom JSON structure
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseFormat", JSONObject().apply {
                        put("type", "OBJECT")
                        put("responseMimeType", "application/json")
                    })
                    put("temperature", 0.7)
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP code $response")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            val rootObj = JSONObject(responseBody)
            val candidates = rootObj.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val textResult = firstCandidate.getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val cleanedText = cleanJsonString(textResult)
            val parsed = expandedPromptAdapter.fromJson(cleanedText)
            parsed ?: throw IOException("Failed to parse JSON")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content via Gemini: ", e)
            getOfflineExpandedPrompt(userInput, modelType, modelTriggerWords)
        }
    }

    private fun cleanJsonString(input: String): String {
        var str = input.trim()
        if (str.startsWith("```json")) {
            str = str.substring(7)
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3)
        }
        return str.trim()
    }

    /**
     * Local Creative Generator when offline or no API key is set
     */
    private fun getOfflineExpandedPrompt(
        userInput: String,
        modelType: String,
        modelTriggerWords: String
    ): ExpandedPromptResponse {
        val aspect = if (modelType == "VIDEO") "16:9" else "1:1"
        val triggerPart = if (modelTriggerWords.isNotEmpty()) ", $modelTriggerWords" else ""

        val englishExpansion = when {
            modelType == "VIDEO" -> {
                "Cinema cinematic video of '$userInput', dynamic slow panning shot, photorealistic unreal engine render, warm volumetric sunset lighting, award-winning cinematography, ultra-detailed, flowing, high FPS$triggerPart"
            }
            userInput.contains("كرتون") || userInput.contains("رسوم") || userInput.contains("cartoon") || userInput.contains("anime") -> {
                "Cute stunning illustration of '$userInput', cell shaded animation, charming vibrant color palettes, artistic hand-drawn textures, masterpieces design, 8k resolution$triggerPart"
            }
            else -> {
                "Masterpiece detailed photograph, realistic capture of '$userInput', dramatic studio lighting, sharp details, raw texture, 85mm portrait lens, professional focus, high contrast, stunning composition$triggerPart"
            }
        }

        val arabicExpansion = when {
            modelType == "VIDEO" -> {
                "مشهد سينمائي متحرك يعبر عن '$userInput' بلقطة دراماتيكية مذهلة وإضاءة ناعمة وحركات كاميرا سلسة."
            }
            else -> {
                "صورة فنية متكاملة فائقة الجودة تجسد بوضوح '$userInput' مع الاهتمام بخصائص الإضاءة والتحف الفنية المتقاطعة."
            }
        }

        val negative = "blurry, low quality, deformed anatomy, bad proportions, worst quality, realistic if cartoon, weird colors, low resolution"

        return ExpandedPromptResponse(englishExpansion, arabicExpansion, negative, aspect)
    }
}
