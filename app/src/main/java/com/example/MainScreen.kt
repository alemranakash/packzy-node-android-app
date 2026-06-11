package com.example

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import java.util.concurrent.Executors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 768

    // Read state from ViewModel
    val baseUrl by viewModel.baseUrl.collectAsState()
    val isAutoOpenMode by viewModel.isAutoOpenMode.collectAsState()
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val scannedId by viewModel.scannedId.collectAsState()
    val isEditingBaseUrl by viewModel.isEditingBaseUrl.collectAsState()
    val torchState by viewModel.torchState.collectAsState()
    val isManualInputExpanded by viewModel.isManualInputExpanded.collectAsState()
    val manualInputText by viewModel.manualInputText.collectAsState()
    val isCameraActive by viewModel.isCameraActive.collectAsState()
    val baseUrlInput by viewModel.baseUrlInput.collectAsState()

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Pulsing glowing animation for interactive components
    val transition = rememberInfiniteTransition(label = "glow")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isTablet) 32.dp else 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. HEADER BAR
            HeaderBar(
                isTablet = isTablet,
                pulseAlpha = pulseAlpha,
                onLoginClick = {
                    viewModel.launchBrowser("https://admin.packzy.com/admin/login", context)
                }
            )

            // 2. PRIMARY SCANNING & MANUAL INPUT ZONE
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isCameraActive) {
                    if (cameraPermissionState.status.isGranted) {
                        // Interactive Camera Viewport with custom Bracket Guide Overlay & Torch & Bottom Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isTablet) 360.dp else 250.dp)
                                .clip(RoundedCornerShape(24.dp)) // rounded-3xl corresponding
                                .background(Slate900)
                                .border(1.dp, Slate800, RoundedCornerShape(24.dp))
                        ) {
                            CameraPreview(
                                isCameraActive = isCameraActive,
                                torchState = torchState,
                                onBarcodeScanned = { code ->
                                    viewModel.handleCodeScanned(code, context)
                                }
                            )

                            // S-Tier Scanning Brackets HUD Overlay
                            CameraFrameOverlay(
                                isTablet = isTablet,
                                pulseAlpha = pulseAlpha
                            )

                            // Floating flashlight toggle in top-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                TorchToggleButton(
                                    torchState = torchState,
                                    onClick = { viewModel.setTorchState(!torchState) }
                                )
                            }

                            // Manual Fallback button overlaid inside the camera box at the bottom (when not expanded)
                            if (!isManualInputExpanded) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setManualInputExpanded(true) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black.copy(alpha = 0.6f),
                                            contentColor = TextWhite
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(0.5.dp, Slate700.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                    ) {
                                        Text(
                                            text = "MANUAL INPUT FALLBACK",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.sp,
                                            color = SoftSlateGray
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Permission Request Panel
                        CameraPermissionFallback(
                            onRequestPermission = {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        )
                    }
                } else {
                    // Camera paused state
                    CameraPausedFallback(
                        pulseAlpha = pulseAlpha,
                        onActivateCamera = {
                            viewModel.setManualInputExpanded(false)
                        }
                    )
                }

                // Interactive Expandable Container for Manual Text Entry
                AnimatedVisibility(
                    visible = isManualInputExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ManualInputFallbackPanel(
                        inputText = manualInputText,
                        onTextChange = { viewModel.setManualInputText(it) },
                        onClose = { viewModel.setManualInputExpanded(false) },
                        onProcessId = {
                            if (manualInputText.isNotEmpty()) {
                                viewModel.handleCodeScanned(manualInputText, context)
                            }
                        }
                    )
                }
            }

            // 3. DETECTION CAPTURED CONTROL CONTAINER
            DetectionCapturedContainer(
                scannedId = scannedId,
                isTablet = isTablet,
                pulseAlpha = pulseAlpha,
                onOpenLink = {
                    val finalUrl = viewModel.compileTargetUrl(scannedId)
                    viewModel.launchBrowser(finalUrl, context)
                }
            )

            // 4. SCANNER CONTROLS & FEEDBACK MODE SWITCHERS
            FeedbackControlsGrid(
                isAutoOpenMode = isAutoOpenMode,
                isHapticEnabled = isHapticEnabled,
                isSoundEnabled = isSoundEnabled,
                onAutoOpenChanged = { viewModel.setAutoOpenMode(it) },
                onHapticChanged = { viewModel.setHapticEnabled(it) },
                onSoundChanged = { viewModel.setSoundEnabled(it) }
            )

            // 5. DYNAMIC TARGET PORTAL SETTING
            DynamicTargetPortalPanel(
                baseUrl = baseUrl,
                isEditing = isEditingBaseUrl,
                baseUrlInput = baseUrlInput,
                scannedId = scannedId,
                onBaseUrlInputChanged = { viewModel.setBaseUrlInput(it) },
                onStartEdit = { viewModel.startEditingBaseUrl() },
                onSave = { viewModel.saveBaseUrl() },
                onCancel = { viewModel.cancelEditingBaseUrl() },
                onReset = { viewModel.resetBaseUrl() },
                onTestCompile = { id -> viewModel.compileTargetUrl(id) }
            )
            
            // System Terminal Telemetry Status Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PACKZY NODE ENGINE V1.4 // STATUS: SECURE_ACTIVE",
                    color = ElectricGreen.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SECURE PORTAL REDIRECTION ENCRYPTED SHA-256",
                    color = SoftSlateGray.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// COMPOSABLE VISUAL MODULE PARTNERS
// ------------------------------------------------------------------

@Composable
fun HeaderBar(
    isTablet: Boolean,
    pulseAlpha: Float,
    onLoginClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Bottom divider line with beautiful 20% opacity emerald tint
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 4.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pulsing live logo indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .drawBehind {
                        drawCircle(
                            color = ElectricGreen.copy(alpha = 0.15f * pulseAlpha),
                            radius = size.minDimension * (0.8f + 0.3f * pulseAlpha)
                        )
                        drawCircle(
                            color = ElectricGreen,
                            radius = 6.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = ElectricGreen.copy(alpha = pulseAlpha),
                            radius = 3.dp.toPx()
                        )
                    }
            )
            
            Text(
                text = "PACKZY NODE",
                color = ElectricGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = if (isTablet) 16.sp else 14.sp,
                letterSpacing = 2.sp
            )
        }

        // Sleek compact button with 50% opacity slate and slate-700 outline
        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Slate800.copy(alpha = 0.5f),
                contentColor = SoftSlateGray
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Slate700),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
                .testTag("login_portal_button")
                .height(34.dp)
        ) {
            Text(
                text = "PORTAL",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = SoftSlateGray
            )
        }
    }
}

@Composable
fun CameraPreview(
    isCameraActive: Boolean,
    torchState: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(torchState, camera) {
        camera?.cameraControl?.enableTorch(torchState)
    }

    if (isCameraActive) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val analyzer = BarcodeAnalyzer { code ->
                        onBarcodeScanned(code)
                    }

                    imageAnalysis.setAnalyzer(executor, analyzer)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()

                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CameraFrameOverlay(
    isTablet: Boolean,
    pulseAlpha: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val selectionWidthRatio = if (isTablet) 0.65f else 0.80f
        val scanFrameWidth = width * selectionWidthRatio
        val scanFrameHeight = scanFrameWidth * 0.65f

        val left = (width - scanFrameWidth) / 2f
        val top = (height - scanFrameHeight) / 2f
        val right = left + scanFrameWidth
        val bottom = top + scanFrameHeight

        // Dark scanner surroundings overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.50f),
            size = Size(width, top)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.50f),
            topLeft = Offset(0f, bottom),
            size = Size(width, height - bottom)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.50f),
            topLeft = Offset(0f, top),
            size = Size(left, scanFrameHeight)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.50f),
            topLeft = Offset(right, top),
            size = Size(width - right, scanFrameHeight)
        )

        // S-Tier Corner guide brackets with drop glows matching mockup specs
        val bracketColor = ElectricGreen.copy(alpha = pulseAlpha)
        val lineLength = 28.dp.toPx()
        val thickness = 3.dp.toPx()

        // Top-Left corner guide
        drawRect(
            color = bracketColor,
            topLeft = Offset(left - thickness / 2, top - thickness / 2),
            size = Size(lineLength, thickness)
        )
        drawRect(
            color = bracketColor,
            topLeft = Offset(left - thickness / 2, top - thickness / 2),
            size = Size(thickness, lineLength)
        )

        // Top-Right corner guide
        drawRect(
            color = bracketColor,
            topLeft = Offset(right - lineLength + thickness / 2, top - thickness / 2),
            size = Size(lineLength, thickness)
        )
        drawRect(
            color = bracketColor,
            topLeft = Offset(right - thickness / 2, top - thickness / 2),
            size = Size(thickness, lineLength)
        )

        // Bottom-Left corner guide
        drawRect(
            color = bracketColor,
            topLeft = Offset(left - thickness / 2, bottom - thickness / 2),
            size = Size(lineLength, thickness)
        )
        drawRect(
            color = bracketColor,
            topLeft = Offset(left - thickness / 2, bottom - lineLength + thickness / 2),
            size = Size(thickness, lineLength)
        )

        // Bottom-Right corner guide
        drawRect(
            color = bracketColor,
            topLeft = Offset(right - lineLength + thickness / 2, bottom - thickness / 2),
            size = Size(lineLength, thickness)
        )
        drawRect(
            color = bracketColor,
            topLeft = Offset(right - thickness / 2, bottom - lineLength + thickness / 2),
            size = Size(thickness, lineLength)
        )

        // Real-time animated scanning beam line with glow
        val scanLineY = top + (scanFrameHeight * (0.5f + 0.48f * kotlin.math.sin(System.currentTimeMillis() / 320.0).toFloat()))
        drawLine(
            color = ElectricGreen.copy(alpha = 0.6f),
            start = Offset(left + 6.dp.toPx(), scanLineY),
            end = Offset(right - 6.dp.toPx(), scanLineY),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

@Composable
fun TorchToggleButton(
    torchState: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .background(
                color = if (torchState) ElectricGreen else Color.Black.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (torchState) ElectricGreen else Slate600,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = if (torchState) Icons.Default.FlashOn else Icons.Default.FlashOff,
            contentDescription = "Toggle Torch Flashlight",
            tint = if (torchState) Color.Black else TextWhite,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CameraPermissionFallback(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = ElectricGreen.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "CAMERA AUTHORIZATION REQUIRED",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Trigger real-time optical scanning by authorizing standard on-device camera feeds.",
            color = SoftSlateGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "AUTHORIZE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun CameraPausedFallback(
    pulseAlpha: Float,
    onActivateCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .drawBehind {
                    drawCircle(
                        color = ElectricGreen.copy(alpha = 0.08f),
                        radius = size.minDimension / 1.7f
                    )
                    drawCircle(
                        color = ElectricGreen.copy(alpha = pulseAlpha * 0.2f),
                        radius = size.minDimension / 2.1f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = ElectricGreen,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "SCANNER STREAM SUSPENDED",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Optical stream paused while custom input modes are in focus. Tap below to reactivate scanner.",
            color = SoftSlateGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onActivateCamera,
            colors = ButtonDefaults.textButtonColors(
                contentColor = ElectricGreen
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "RESUME OPTICAL CAPTURE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ManualInputFallbackPanel(
    inputText: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onProcessId: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = ElectricGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "MANUAL RESOLUTION NODE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = SoftSlateGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = "Enter raw node identifier sequence key below when barcode stickers are degraded:",
            color = SoftSlateGray,
            fontSize = 11.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_id_input_field"),
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                placeholder = {
                    Text(
                        text = "e.g., PKZ-9928-AX7",
                        color = Slate600,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricGreen,
                    unfocusedBorderColor = Slate800,
                    cursorColor = ElectricGreen,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (inputText.isNotEmpty()) onProcessId() }
                )
            )

            Button(
                onClick = onProcessId,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = inputText.isNotEmpty(),
                modifier = Modifier
                    .testTag("process_id_button")
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun DetectionCapturedContainer(
    scannedId: String,
    isTablet: Boolean,
    pulseAlpha: Float,
    onOpenLink: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val hasContent = scannedId.isNotEmpty()
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val openButtonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)) // rounded-2xl
            .background(Slate900.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                color = if (hasContent) Emerald500.copy(alpha = 0.4f) else Slate800,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // Badge Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // High fidelity indicator capsule
                Box(
                    modifier = Modifier
                        .background(
                            color = if (hasContent) Emerald500.copy(alpha = 0.12f) else Slate800.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (hasContent) Emerald500.copy(alpha = 0.4f) else Slate700.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DETECTION CAPTURED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = if (hasContent) ElectricGreen else SoftSlateGray,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Status label
            Text(
                text = if (hasContent) "DECODED" else "READY / AWAITING",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (hasContent) ElectricGreen.copy(alpha = 0.7f) else Slate600
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (hasContent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    SelectionContainer {
                        Text(
                            text = scannedId,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "STANDARD PARCEL ID",
                        color = SoftSlateGray,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // S-Tier Glowing Link Trigger Button
                IconButton(
                    onClick = onOpenLink,
                    modifier = Modifier
                        .size(46.dp)
                        .scale(openButtonScale)
                        .background(ElectricGreen, RoundedCornerShape(12.dp))
                        .testTag("open_link_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowOutward,
                        contentDescription = "Process Portal Target Link",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // High fidelity fallback matching "Sleek Interface" mockup beautifully
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .border(BorderStroke(0.5.dp, Slate800), RoundedCornerShape(10.dp))
                    .padding(vertical = 20.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = SoftSlateGray.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AWAITING NODE IDENTIFIER",
                        color = SoftSlateGray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackControlsGrid(
    isAutoOpenMode: Boolean,
    isHapticEnabled: Boolean,
    isSoundEnabled: Boolean,
    onAutoOpenChanged: (Boolean) -> Unit,
    onHapticChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Auto Open
        FeedbackControlCard(
            modifier = Modifier.weight(1f),
            label = "AUTO OPEN",
            statusLabel = if (isAutoOpenMode) "ENABLED" else "HOLD",
            statusColor = if (isAutoOpenMode) ElectricGreen else SoftSlateGray,
            checked = isAutoOpenMode,
            onCheckedChange = onAutoOpenChanged
        )

        // Card 2: Haptic
        FeedbackControlCard(
            modifier = Modifier.weight(1f),
            label = "HAPTIC PUNCH",
            statusLabel = if (isHapticEnabled) "ACTIVE" else "MUTED",
            statusColor = if (isHapticEnabled) ElectricGreen else SoftSlateGray,
            checked = isHapticEnabled,
            onCheckedChange = onHapticChanged
        )

        // Card 3: Audio Beep
        FeedbackControlCard(
            modifier = Modifier.weight(1f),
            label = "AUDIO BEEP",
            statusLabel = if (isSoundEnabled) "ACTIVE" else "MUTED",
            statusColor = if (isSoundEnabled) ElectricGreen else SoftSlateGray,
            checked = isSoundEnabled,
            onCheckedChange = onSoundChanged
        )
    }
}

@Composable
fun FeedbackControlCard(
    modifier: Modifier = Modifier,
    label: String,
    statusLabel: String,
    statusColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = Slate600,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusLabel,
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            // Custom futuristic sliding/pill switch element matching exact mockup specs
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 18.dp)
                    .clip(CircleShape)
                    .background(if (checked) Emerald500.copy(alpha = 0.15f) else Slate800)
                    .border(
                        width = 1.dp,
                        color = if (checked) Emerald500.copy(alpha = 0.4f) else Slate700,
                        shape = CircleShape
                    )
                    .clickable { onCheckedChange(!checked) }
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                        .background(if (checked) ElectricGreen else Slate600, CircleShape)
                )
            }
        }
    }
}

@Composable
fun DynamicTargetPortalPanel(
    baseUrl: String,
    isEditing: Boolean,
    baseUrlInput: String,
    scannedId: String,
    onBaseUrlInputChanged: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onTestCompile: (String) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Slate900.copy(alpha = 0.5f))
            .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isEditing) {
            // Highly polished compact footer bar representing redirection target URL specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "REDIRECTION TARGET",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = Slate600,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = baseUrl,
                        color = Color(0xFFD1D5DB),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                TextButton(
                    onClick = onStartEdit,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ElectricGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .testTag("edit_link_button")
                        .height(28.dp)
                ) {
                    Text(
                        text = "EDIT LINK",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
            }
            
            // Helpful telemetry preview helper
            val currentTest = if (scannedId.isEmpty()) "PKZ-9928-AX7" else scannedId
            Text(
                text = "RESOLVED PREVIEW: ${onTestCompile(currentTest)}",
                color = Emerald400.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            // Form display to change host
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "REDIRECTION ROUTE BASE URL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White
                )

                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = onBaseUrlInputChanged,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base_url_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricGreen,
                        unfocusedBorderColor = Slate800,
                        cursorColor = ElectricGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text(
                            text = "SAVE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = SoftSlateGray
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "CANCEL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onReset,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "RESET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
