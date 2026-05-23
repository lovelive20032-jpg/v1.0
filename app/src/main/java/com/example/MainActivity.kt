package com.example

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local Room Database instantiation
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = TakhayyalRepository(database.civitaiModelDao(), database.generationDao())
        
        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        topBar = { TopNavigationBar() },
                        bottomBar = { BottomTabSwitcher(viewModel) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            ActiveScreenContent(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigationBar() {
    val apiKey = BuildConfig.GEMINI_API_KEY
    val isGeminiActive = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "GEMINI_API_KEY"

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DeepBackground,
            titleContentColor = LightText
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "تخيل ",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        brush = Brush.horizontalGradient(listOf(PrimaryNeon, SecondaryNeon))
                    )
                )
                Text(
                    text = "AI",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LightText
                )
            }
        },
        actions = {
            // Gemini Connection Status Indicator Badge
            Surface(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = if (isGeminiActive) BrightGreen.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                border = BorderStroke(
                    1.dp,
                    if (isGeminiActive) BrightGreen.copy(alpha = 0.6f) else GoldAccent.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isGeminiActive) BrightGreen else GoldAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGeminiActive) "الذكاء نشط" else "محلي مبدع",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGeminiActive) BrightGreen else GoldAccent
                    )
                }
            }
        }
    )
}

@Composable
fun BottomTabSwitcher(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    NavigationBar(
        containerColor = DeepBackground,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { viewModel.selectTab(0) },
            label = { Text("صور", fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 0) Icons.Filled.Image else Icons.Outlined.Image,
                    contentDescription = "توليد صورة"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LightText,
                selectedTextColor = PrimaryNeon,
                unselectedIconColor = DimText,
                unselectedTextColor = DimText,
                indicatorColor = PrimaryNeon.copy(alpha = 0.4f)
            ),
            modifier = Modifier.testTag("tab_image_gen")
        )
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { viewModel.selectTab(1) },
            label = { Text("فيديو", fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 1) Icons.Filled.MovieCreation else Icons.Outlined.MovieCreation,
                    contentDescription = "توليد فيديو"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LightText,
                selectedTextColor = PrimaryNeon,
                unselectedIconColor = DimText,
                unselectedTextColor = DimText,
                indicatorColor = PrimaryNeon.copy(alpha = 0.4f)
            ),
            modifier = Modifier.testTag("tab_video_gen")
        )
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { viewModel.selectTab(2) },
            label = { Text("الموديلات", fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 2) Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload,
                    contentDescription = "موديلات Civitai"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LightText,
                selectedTextColor = PrimaryNeon,
                unselectedIconColor = DimText,
                unselectedTextColor = DimText,
                indicatorColor = PrimaryNeon.copy(alpha = 0.4f)
            ),
            modifier = Modifier.testTag("tab_models")
        )
        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { viewModel.selectTab(3) },
            label = { Text("الأرشيف", fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 3) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                    contentDescription = "أرشيف الأجيال"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LightText,
                selectedTextColor = PrimaryNeon,
                unselectedIconColor = DimText,
                unselectedTextColor = DimText,
                indicatorColor = PrimaryNeon.copy(alpha = 0.4f)
            ),
            modifier = Modifier.testTag("tab_archive")
        )
    }
}

@Composable
fun ActiveScreenContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "TabTransition"
    ) { targetTab ->
        when (targetTab) {
            0 -> GenerationImageScreen(viewModel)
            1 -> GenerationVideoScreen(viewModel)
            2 -> ModelsManagerScreen(viewModel)
            3 -> GalleryHistoryScreen(viewModel)
        }
    }
}

// ======================== IMAGE GENERATOR SCREEN ========================
@Composable
fun GenerationImageScreen(viewModel: MainViewModel) {
    val promptAr by viewModel.imagePromptAr.collectAsStateWithLifecycle()
    val promptEn by viewModel.imagePromptEn.collectAsStateWithLifecycle()
    val negativePrompt by viewModel.imageNegative.collectAsStateWithLifecycle()
    val aspect by viewModel.imageAspect.collectAsStateWithLifecycle()
    val selectedModel by viewModel.imageSelectedModel.collectAsStateWithLifecycle()
    val isExpanding by viewModel.isExpanding.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val downloadedModels by viewModel.downloadedModels.collectAsStateWithLifecycle()

    var showAdvanced by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryNeon.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "توليد صور فائقة الخيال ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                            Text(
                                "اكتب فكرتك البسيطة بالعربية وسيقوم الذكاء بتفصيلها وتوليدها كلوحة مذهلة.",
                                fontSize = 12.sp,
                                color = DimText
                            )
                        }
                    }
                }
            }

            // Arabic Prompt Input
            item {
                Column {
                    Text(
                        "فكرة الصورة (باللغة العربية)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = promptAr,
                        onValueChange = { viewModel.imagePromptAr.value = it },
                        placeholder = { Text("مثال: قط رائد فضاء يستكشف المريخ تحت النجوم الملونه...", color = DimText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = VariantSurface,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText,
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface
                        ),
                        maxLines = 4,
                        minLines = 3
                    )
                }
            }

            // Expand prompt AI Button
            item {
                Button(
                    onClick = { viewModel.expandPromptWithAI("IMAGE") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ai_expand_image_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(14.dp),
                    enabled = promptAr.isNotBlank() && !isExpanding
                ) {
                    if (isExpanding) {
                        CircularProgressIndicator(color = LightText, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جاري تحسين الفكرة الذكاء يكتب...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Filled.CloudQueue, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("تحسين المطالبة الذكي بـ Gemini ✨", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Prompt Suggestions
            item {
                Column {
                    Text(
                        "أفكار سريعة مقترحة:",
                        fontSize = 12.sp,
                        color = DimText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestions = listOf(
                            "حصان عربي مطلي بالذهب يركض في شوارع دبي سريالي",
                            "بوابة سحرية مشعة بالأزرق في غابة غامضة خريفية",
                            "قلعة بأسلوب السايبربانك فوق السحاب في إضاءة الغسق",
                            "فنجان قهوة عربية يتطاير منه مجرات وأكواد برمجية"
                        )
                        suggestions.forEach { text ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.imagePromptAr.value = text },
                                color = VariantSurface,
                                border = BorderStroke(1.dp, VariantSurface)
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 12.sp,
                                    color = LightText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Model Picker Selector
            item {
                Column {
                    Text(
                        "موديل التوليد الفني (مستورد من Civitai)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelPicker = true },
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, VariantSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = selectedModel?.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedModel?.name ?: "تسمية الموديل",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                                Text(
                                    text = "${selectedModel?.type ?: "Checkpoint"} • ${selectedModel?.baseModel ?: "SD 1.5"}",
                                    fontSize = 12.sp,
                                    color = DimText
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "تغيير الموديل",
                                tint = DimText
                            )
                        }
                    }
                }
            }

            // Collapse Advanced Parameters Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = SecondaryNeon
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إعدادات فنية متقدمة (الأبعاد، نفي العناصر)",
                        color = SecondaryNeon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Collapsible Advanced section
            if (showAdvanced) {
                // Aspects Card Selector
                item {
                    Column {
                        Text(
                            "نسبة أبعاد الصورة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val aspects = listOf("1:1", "16:9", "9:16", "4:3")
                            aspects.forEach { itemAspect ->
                                val isSelected = aspect == itemAspect
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PrimaryNeon.copy(alpha = 0.25f) else CardSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) PrimaryNeon else VariantSurface,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.imageAspect.value = itemAspect }
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val iconRatio = when (itemAspect) {
                                        "1:1" -> Icons.Filled.CropSquare
                                        "16:9" -> Icons.Filled.CropLandscape
                                        "9:16" -> Icons.Filled.CropPortrait
                                        else -> Icons.Filled.Crop
                                    }
                                    Icon(
                                        imageVector = iconRatio,
                                        contentDescription = null,
                                        tint = if (isSelected) PrimaryNeon else DimText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = itemAspect,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) LightText else DimText
                                    )
                                }
                            }
                        }
                    }
                }

                // English Prompt (AI Expanded Preview)
                item {
                    Column {
                        Text(
                            "المطالبة الإنجليزية الموسعة والموصى بها",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = promptEn,
                            onValueChange = { viewModel.imagePromptEn.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SecondaryNeon,
                                unfocusedBorderColor = VariantSurface,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            ),
                            maxLines = 6
                        )
                    }
                }

                // Negative Prompt Input
                item {
                    Column {
                        Text(
                            "العناصر المستبعدة والممنوعة (Negative Prompt)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = negativePrompt,
                            onValueChange = { viewModel.imageNegative.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SecondaryNeon,
                                unfocusedBorderColor = VariantSurface,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            ),
                            maxLines = 4
                        )
                    }
                }
            }

            // Main Generate Action Button
            item {
                Button(
                    onClick = { viewModel.startGeneratingMedia("IMAGE") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("action_generate_image"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryNeon,
                        contentColor = DeepBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = promptAr.isNotBlank() && (selectedModel?.isDownloaded == true)
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "تخيل وصنّع الصورة الآن 🎨",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                if (selectedModel?.isDownloaded == false) {
                    Text(
                        "⚠️ الموديل المحدد غير محمل حالياً. يرجى زيارة قسم الموديلات وتنزيله للبدء.",
                        fontSize = 11.sp,
                        color = ErrorRose,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Live Generation Worker Progress Pane
        if (generationState != null) {
            GenerationProgressPanel(
                state = generationState!!,
                onDismiss = { viewModel.clearGenerationState() }
            )
        }
    }

    // Model Selection Bottom Dialog Sheet
    if (showModelPicker) {
        Dialog(onDismissRequest = { showModelPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "اختر نموذج التوليد المستورد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "نماذج Checkpoint تمنح الصورة طابعها الفني (أنمي، واقعي، ثلاثي الأبعاد).",
                        fontSize = 12.sp,
                        color = DimText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        val downloadedOnly = downloadedModels
                        if (downloadedOnly.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "لا توجد نماذج محملة حالياً. حمّل موديل من قسم الموديلات أولاً.",
                                        fontSize = 13.sp,
                                        color = ErrorRose,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(downloadedOnly) { model ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedModel?.id == model.id) PrimaryNeon.copy(alpha = 0.2f) else VariantSurface)
                                        .border(
                                            1.dp,
                                            if (selectedModel?.id == model.id) PrimaryNeon else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.imageSelectedModel.value = model
                                            showModelPicker = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = model.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LightText
                                        )
                                        Text(
                                            "${model.type} • ${model.baseModel}",
                                            fontSize = 11.sp,
                                            color = DimText
                                        )
                                    }
                                    if (selectedModel?.id == model.id) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = PrimaryNeon
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== VIDEO GENERATOR SCREEN ========================
@Composable
fun GenerationVideoScreen(viewModel: MainViewModel) {
    val promptAr by viewModel.videoPromptAr.collectAsStateWithLifecycle()
    val promptEn by viewModel.videoPromptEn.collectAsStateWithLifecycle()
    val negativePrompt by viewModel.videoNegative.collectAsStateWithLifecycle()
    val aspect by viewModel.videoAspect.collectAsStateWithLifecycle()
    val selectedModel by viewModel.videoSelectedModel.collectAsStateWithLifecycle()
    val isExpanding by viewModel.isExpanding.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val allModels by viewModel.allModels.collectAsStateWithLifecycle()

    var showAdvanced by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SecondaryNeon.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.MovieCreation,
                                    contentDescription = null,
                                    tint = SecondaryNeon,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "توليد فيديوهات سينمائية (Veo)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                            Text(
                                "صمم حركات وزوايا سينمائية مذهلة بإضافة لمسات الذكاء الاصطناعي التوليدي الحصري.",
                                fontSize = 12.sp,
                                color = DimText
                            )
                        }
                    }
                }
            }

            // Arabic Prompt Input
            item {
                Column {
                    Text(
                        "فكرة وتأثيرات الفيديو (باللغة العربية)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = promptAr,
                        onValueChange = { viewModel.videoPromptAr.value = it },
                        placeholder = { Text("مثال: كاميرا مائلة تدور حول مركبة فضائية تائهة في مجرة سديمية...", color = DimText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryNeon,
                            unfocusedBorderColor = VariantSurface,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText,
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface
                        ),
                        maxLines = 4,
                        minLines = 3
                    )
                }
            }

            // Expand prompt AI Button
            item {
                Button(
                    onClick = { viewModel.expandPromptWithAI("VIDEO") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ai_expand_video_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(14.dp),
                    enabled = promptAr.isNotBlank() && !isExpanding
                ) {
                    if (isExpanding) {
                        CircularProgressIndicator(color = LightText, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جاري تحرير الفكرة وتحويلها بالذكاء...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("صياغة المطالبة السينمائية بـ Gemini ✨", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Video Motion Guide Model Picker
            item {
                Column {
                    Text(
                        "موديل توجيه الحركة (Motion Module)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelPicker = true },
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = BorderStroke(1.dp, VariantSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = selectedModel?.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedModel?.name ?: "DreamShaper v8",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                                Text(
                                    text = "${selectedModel?.type ?: "Motion Module"} • ${selectedModel?.baseModel ?: "SD 1.5"}",
                                    fontSize = 12.sp,
                                    color = DimText
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "تغيير الموديل",
                                tint = DimText
                            )
                        }
                    }
                }
            }

            // Collapse Advanced Parameters Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = SecondaryNeon
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إعدادات فيديو متقدمة (الكاميرا الحركية)",
                        color = SecondaryNeon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showAdvanced) {
                // Aspects Card Selector
                item {
                    Column {
                        Text(
                            "النسبة البصرية والاتجاه",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val aspects = listOf("16:9", "9:16", "1:1")
                            aspects.forEach { itemAspect ->
                                val isSelected = aspect == itemAspect
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) SecondaryNeon.copy(alpha = 0.25f) else CardSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) SecondaryNeon else VariantSurface,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.videoAspect.value = itemAspect }
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val iconRatio = when (itemAspect) {
                                        "16:9" -> Icons.Filled.CropLandscape
                                        "9:16" -> Icons.Filled.CropPortrait
                                        else -> Icons.Filled.CropSquare
                                    }
                                    Icon(
                                        imageVector = iconRatio,
                                        contentDescription = null,
                                        tint = if (isSelected) SecondaryNeon else DimText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = itemAspect,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) LightText else DimText
                                    )
                                }
                            }
                        }
                    }
                }

                // Expanded English Prompt
                item {
                    Column {
                        Text(
                            "المطالبة الإنجليزية السينمائية التوليدية",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = promptEn,
                            onValueChange = { viewModel.videoPromptEn.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SecondaryNeon,
                                unfocusedBorderColor = VariantSurface,
                                focusedContainerColor = CardSurface,
                                unfocusedContainerColor = CardSurface
                            ),
                            maxLines = 6
                        )
                    }
                }
            }

            // Main Generate Action Button
            item {
                Button(
                    onClick = { viewModel.startGeneratingMedia("VIDEO") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("action_generate_video"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = DeepBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = promptAr.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "توليد وبناء الفيديو الآن 🎞️",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Live Generation Worker Progress Pane
        if (generationState != null) {
            GenerationProgressPanel(
                state = generationState!!,
                onDismiss = { viewModel.clearGenerationState() }
            )
        }
    }

    // Model selection Dialog Sheet
    if (showModelPicker) {
        Dialog(onDismissRequest = { showModelPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "اختر موديل توجيه الحركة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allModels) { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedModel?.id == model.id) SecondaryNeon.copy(alpha = 0.2f) else VariantSurface)
                                    .border(
                                        1.dp,
                                        if (selectedModel?.id == model.id) SecondaryNeon else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.videoSelectedModel.value = model
                                        showModelPicker = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = model.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        model.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightText
                                    )
                                    Text(
                                        "${model.type} • ${model.baseModel}",
                                        fontSize = 11.sp,
                                        color = DimText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== GENERATION PROGRESS PANEL ========================
@Composable
fun GenerationProgressPanel(
    state: GenerationProgressState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (state is GenerationProgressState.Success) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, VariantSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (state) {
                    is GenerationProgressState.Pending -> {
                        CircularProgressIndicator(color = SecondaryNeon, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "جاري الاتصال وتحصيل المولد...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            state.message,
                            fontSize = 13.sp,
                            color = DimText,
                            textAlign = TextAlign.Center
                        )
                    }
                    is GenerationProgressState.Progress -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = state.percent / 100f,
                                color = PrimaryNeon,
                                modifier = Modifier.size(72.dp),
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = "${state.percent}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "جاري المعالجة السحابية المعجّلة ⚡",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            state.message,
                            fontSize = 13.sp,
                            color = DimText,
                            textAlign = TextAlign.Center
                        )
                    }
                    is GenerationProgressState.Success -> {
                        Surface(
                            shape = CircleShape,
                            color = BrightGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = BrightGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "اكتمل توليد لوحتك الإبداعية!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Small preview
                        AsyncImage(
                            model = state.item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_close_generation"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("أدخل المعرض للاستعراض 🔍", fontWeight = FontWeight.Bold)
                        }
                    }
                    is GenerationProgressState.Error -> {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRose,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "خطأ أثناء توليف الصورة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            state.error,
                            fontSize = 13.sp,
                            color = DimText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRose)
                        ) {
                            Text("إغلاق", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ======================== MODELS SCREEN ========================
@Composable
fun ModelsManagerScreen(viewModel: MainViewModel) {
    val allModels by viewModel.allModels.collectAsStateWithLifecycle()
    val downloadingIds by viewModel.downloadingModelIds.collectAsStateWithLifecycle()
    val civitaiUrl by viewModel.civitaiUrlInput.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Civitai Link Downloader Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = PrimaryNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "استيراد موديل من موقع Civitai.com",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ألصق رابط النموذج أو معرّف الموديل للتواصل مع Civitai وقراءة بيانات التدريب كلياً.",
                        fontSize = 12.sp,
                        color = DimText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = civitaiUrl,
                        onValueChange = { viewModel.civitaiUrlInput.value = it },
                        placeholder = { Text("https://civitai.com/models/128713...", color = DimText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("civitai_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = VariantSurface,
                            focusedTextColor = LightText,
                            unfocusedTextColor = LightText,
                            focusedContainerColor = VariantSurface,
                            unfocusedContainerColor = VariantSurface
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { viewModel.importCivitaiModel() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("civitai_import_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(12.dp),
                        enabled = civitaiUrl.isNotBlank() && !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(color = LightText, modifier = Modifier.size(20.dp))
                        } else {
                            Text("استدعاء وقراءة الموديل 🌐", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (importMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = VariantSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = importMessage!!,
                                    fontSize = 12.sp,
                                    color = LightText,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearImportMessage() }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = DimText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                "النماذج والموديلات النشطة",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
        }

        // LazyList of civitai models
        items(allModels) { model ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (model.isDownloaded) PrimaryNeon.copy(alpha = 0.4f) else VariantSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        AsyncImage(
                            model = model.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    model.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                                if (model.isCustom) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = SecondaryNeon.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "مستورد",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryNeon,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "${model.type} • ${model.baseModel}",
                                fontSize = 12.sp,
                                color = DimText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rating",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", model.rating),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        model.description,
                        fontSize = 12.sp,
                        color = LightText.copy(alpha = 0.85f)
                    )

                    // Trained / Trigger Words copyable button
                    if (model.triggerWords.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VariantSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboard.setText(AnnotatedString(model.triggerWords))
                                    Toast
                                        .makeText(
                                            context,
                                            "تم نسخ الكلمات المفتاحية الذكية للموديل 📋",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy triggers",
                                    tint = SecondaryNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الكلمات المفتاحية: ${model.triggerWords}",
                                    fontSize = 11.sp,
                                    color = DimText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Download Sim action OR Status Indicator
                    val isDownloading = downloadingIds.containsKey(model.id)
                    val downloadProgress = downloadingIds[model.id] ?: 0

                    if (model.isDownloaded) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BrightGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BrightGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = BrightGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("النموذج جاهز ونشط للتوليد الفني ⚡", fontSize = 12.sp, color = BrightGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (isDownloading) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("جاري تحميل ملقم Civitai...", fontSize = 11.sp, color = DimText)
                                Text("$downloadProgress%", fontSize = 11.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = downloadProgress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PrimaryNeon,
                                trackColor = VariantSurface
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.downloadCivitaiModel(model) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VariantSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null, tint = SecondaryNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحميل ملف النموذج واستيراده (مجاني)", fontSize = 12.sp, color = LightText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ======================== GALLERY / HISTORY SCREEN ========================
@Composable
fun GalleryHistoryScreen(viewModel: MainViewModel) {
    val history by viewModel.generationHistory.collectAsStateWithLifecycle()
    var activeItemForViewer by remember { mutableStateOf<GenerationItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "معرض أفكارك وإبداعاتك ✨",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Surface(
                shape = CircleShape,
                color = VariantSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "${history.size} عنصر",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryNeon,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Empty gallery",
                        tint = DimText,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "لست تملك إبداعات سابقة حالياً.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "توجه لتبويبي توليد الصور أو الفيديوهات لإنشاء اللوحة الأولى لمخيلتك!",
                        fontSize = 12.sp,
                        color = DimText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeItemForViewer = item },
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Coil Image Loader
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.promptArabic,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Badges Overlay at Top Corners
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Media badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.type == "VIDEO") GoldAccent else PrimaryNeon,
                                    modifier = Modifier.graphicsLayer(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = if (item.type == "VIDEO") "فيديو 🎞️" else "صورة 🎨",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepBackground,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                
                                // Fav heart
                                Surface(
                                    shape = CircleShape,
                                    color = CardSurface.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { viewModel.toggleFavorite(item) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (item.isFavorite) ErrorRose else LightText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                item.promptArabic,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = LightText
                            )
                            Text(
                                item.modelName,
                                fontSize = 10.sp,
                                color = DimText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // High fidelity Detailed Full Screen Media Viewer
    if (activeItemForViewer != null) {
        val detail = activeItemForViewer!!
        
        // Custom animation parameter for panning simulated camera zoom (Ken-Burns)
        val infiniteTransition = rememberInfiniteTransition(label = "KenBurns")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (detail.type == "VIDEO") 1.15f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val translationX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (detail.type == "VIDEO") 15f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "transX"
        )

        Dialog(
            onDismissRequest = { activeItemForViewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DeepBackground
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Title Bar / Back Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { activeItemForViewer = null }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LightText)
                        }
                        Text(
                            text = if (detail.type == "VIDEO") "تفاصيل المعالجة السينمائية" else "استعراض لوحتك الفنية",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        IconButton(onClick = { viewModel.toggleFavorite(detail) }) {
                            Icon(
                                imageVector = if (detail.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (detail.isFavorite) ErrorRose else LightText
                            )
                        }
                    }

                    // Large Media Display Layout
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = detail.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = translationX
                                ),
                            contentScale = ContentScale.Fit
                        )
                        
                        // If type is VIDEO, show pulsing cinemagraph tag
                        if (detail.type == "VIDEO") {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = DeepBackground.copy(alpha = 0.8f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = SecondaryNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "محاكاة التدفق السينمائي النشط 🎞️",
                                        color = SecondaryNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Metadata detail card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "الوصف المكتوب (عربي):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNeon
                                )
                                Text(
                                    detail.promptArabic,
                                    fontSize = 14.sp,
                                    color = LightText
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "المطالبة الإنجليزية التوليدية (English Mode):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryNeon
                                )
                                Text(
                                    detail.promptEnglish,
                                    fontSize = 12.sp,
                                    color = DimText,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("نموذج التدريب", fontSize = 11.sp, color = DimText)
                                        Text(detail.modelName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LightText)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("رقم البذرة (Seed)", fontSize = 11.sp, color = DimText)
                                        Text(detail.seed.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LightText)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("الأبعاد", fontSize = 11.sp, color = DimText)
                                        Text(detail.aspect, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LightText)
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.deleteHistoryItem(detail)
                                        activeItemForViewer = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose)
                                ) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("حذف هذا التوليد نهائياً من الأرشيف", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
