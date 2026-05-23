package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel(
    application: Application,
    private val repository: TakhayyalRepository
) : AndroidViewModel(application) {

    // Tab selection
    private val _currentTab = MutableStateFlow(0) // 0: Image Gen, 1: Video Gen, 2: Models, 3: History
    val currentTab = _currentTab.asStateFlow()

    // Database flows
    val allModels: StateFlow<List<CivitaiModel>> = repository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedModels: StateFlow<List<CivitaiModel>> = repository.downloadedModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generationHistory: StateFlow<List<GenerationItem>> = repository.generationHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Generation Inputs
    val imagePromptAr = MutableStateFlow("")
    val imagePromptEn = MutableStateFlow("")
    val imageNegative = MutableStateFlow("blurry, low quality, extra limbs, bad eyes, ugly, deformed, worst quality")
    val imageAspect = MutableStateFlow("1:1")
    val imageSelectedModel = MutableStateFlow<CivitaiModel?>(null)

    val videoPromptAr = MutableStateFlow("")
    val videoPromptEn = MutableStateFlow("")
    val videoNegative = MutableStateFlow("low resolutions, bad lighting, shaky camera, glitchy, deformed movement, worst quality")
    val videoAspect = MutableStateFlow("16:9")
    val videoSelectedModel = MutableStateFlow<CivitaiModel?>(null)

    // AI Expander State
    private val _isExpanding = MutableStateFlow(false)
    val isExpanding = _isExpanding.asStateFlow()

    // Generator Worker State
    private val _generationState = MutableStateFlow<GenerationProgressState?>(null)
    val generationState = _generationState.asStateFlow()

    // Model Download Tracker
    private val _downloadingModelIds = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val downloadingModelIds = _downloadingModelIds.asStateFlow()

    // Civitai URL Import Section State
    val civitaiUrlInput = MutableStateFlow("")
    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()
    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage = _importStatusMessage.asStateFlow()

    init {
        // Safe default model assignment once loaded
        viewModelScope.launch {
            allModels.collect { models ->
                if (models.isNotEmpty()) {
                    if (imageSelectedModel.value == null) {
                        imageSelectedModel.value = models.firstOrNull { it.isDownloaded }
                    }
                    if (videoSelectedModel.value == null) {
                        videoSelectedModel.value = models.lastOrNull { !it.type.contains("LORA") }
                    }
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
        _generationState.value = null // reset generation panel on tab switch
    }

    /**
     * Call AI to expand Prompt from simple Arabic into ultra rich English SD prompt!
     */
    fun expandPromptWithAI(type: String) {
        val input = if (type == "IMAGE") imagePromptAr.value else videoPromptAr.value
        if (input.isBlank()) return

        val triggerWords = if (type == "IMAGE") {
            imageSelectedModel.value?.triggerWords ?: ""
        } else {
            videoSelectedModel.value?.triggerWords ?: ""
        }

        viewModelScope.launch {
            _isExpanding.value = true
            try {
                val expanded = repository.getExpandedPrompt(input, type, triggerWords)
                if (type == "IMAGE") {
                    imagePromptEn.value = expanded.promptEnglish
                    imagePromptAr.value = expanded.promptArabicExpanded
                    imageNegative.value = expanded.negativePrompt
                    imageAspect.value = expanded.suggestedAspect
                } else {
                    videoPromptEn.value = expanded.promptEnglish
                    videoPromptAr.value = expanded.promptArabicExpanded
                    videoNegative.value = expanded.negativePrompt
                    videoAspect.value = expanded.suggestedAspect
                }
            } catch (e: Exception) {
                // Handled gracefully in repo
            } finally {
                _isExpanding.value = false
            }
        }
    }

    /**
     * Start producing media (Simulates real-time pipeline calling repository workflow)
     */
    fun startGeneratingMedia(type: String) {
        val promptAr = if (type == "IMAGE") imagePromptAr.value else videoPromptAr.value
        var promptEn = if (type == "IMAGE") imagePromptEn.value else videoPromptEn.value
        val negative = if (type == "IMAGE") imageNegative.value else videoNegative.value
        val aspect = if (type == "IMAGE") imageAspect.value else videoAspect.value
        val model = if (type == "IMAGE") imageSelectedModel.value else videoSelectedModel.value

        if (promptAr.isBlank()) return
        if (promptEn.isBlank()) {
            // Auto complete English prompt from Arabic if user skipped AI expansion
            promptEn = "Dynamic detailed artistic capture of: $promptAr"
        }

        val modelName = model?.name ?: "DreamShaper v8"

        viewModelScope.launch {
            repository.generateMedia(
                type = type,
                promptAr = promptAr,
                promptEn = promptEn,
                negative = negative,
                aspect = aspect,
                modelName = modelName
            ).collect { state ->
                _generationState.value = state
            }
        }
    }

    fun clearGenerationState() {
        _generationState.value = null
    }

    /**
     * Simulated downloading of a Civitai Model version file
     */
    fun downloadCivitaiModel(model: CivitaiModel) {
        if (model.isDownloaded) return
        viewModelScope.launch {
            _downloadingModelIds.update { it + (model.id to 0) }
            repository.simulateModelDownload(model).collect { updatedModel ->
                _downloadingModelIds.update { 
                    if (updatedModel.isDownloaded) {
                        it - model.id
                    } else {
                        it + (model.id to updatedModel.downloadProgress)
                    }
                }
            }
        }
    }

    /**
     * Paste Civitai URL or ID, fetch live details, add to Database
     */
    fun importCivitaiModel() {
        val url = civitaiUrlInput.value
        if (url.isBlank()) {
            _importStatusMessage.value = "يرجى كتابة رابط الموديل من Civitai أولاً."
            return
        }

        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "جاري قراءة الرابط وجلب تفاصيل النموذج..."
            val result = repository.fetchAndAddCivitaiModel(url)
            _isImporting.value = false
            
            if (result.isSuccess) {
                val model = result.getOrNull()
                _importStatusMessage.value = "تم استيراد النموذج بنجاح: ${model?.name}! متوفر الآن في تبويب الموديلات لبدء التحميل."
                civitaiUrlInput.value = ""
            } else {
                _importStatusMessage.value = result.exceptionOrNull()?.message ?: "خطأ أثناء محاولة جلب الموديل."
            }
        }
    }

    fun clearImportMessage() {
        _importStatusMessage.value = null
    }

    fun toggleFavorite(item: GenerationItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun deleteHistoryItem(item: GenerationItem) {
        viewModelScope.launch {
            repository.deleteGeneration(item)
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: TakhayyalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
