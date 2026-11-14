package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.TuiCard
import com.example.ui.components.TuiSectionHeader
import com.example.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropAspectPreset(val label: String, val ratio: Float) {
    SQUARE("1:1 Square", 1f),
    WIDE_2_1("2:1 Wide", 2f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    WIDE_16_9("16:9", 16f / 9f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageCropDialog(
    imageUri: Uri,
    title: String,
    isIconCrop: Boolean,
    outputPrefix: String,
    initialAspect: CropAspectPreset = CropAspectPreset.SQUARE,
    onCropped: (savedFilePath: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadError by remember(imageUri) { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var rotationDeg by remember(imageUri) { mutableIntStateOf(0) }
    var aspectPreset by remember(imageUri, initialAspect) { mutableStateOf(initialAspect) }
    var showCircleGuide by remember(imageUri) { mutableStateOf(false) }
    var zoom by remember(imageUri) { mutableFloatStateOf(1f) }
    var panX by remember(imageUri) { mutableFloatStateOf(0f) }
    var panY by remember(imageUri) { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(imageUri) {
        loadError = false
        val loaded = loadDownsampledBitmap(context, imageUri, maxDim = 1600)
        if (loaded == null) {
            loadError = true
        } else {
            sourceBitmap = loaded
        }
    }

    val displayBitmap = remember(sourceBitmap, rotationDeg) {
        val base = sourceBitmap ?: return@remember null
        if (rotationDeg % 360 == 0) base
        else {
            val matrix = Matrix().apply { postRotate(rotationDeg.toFloat()) }
            Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
        }
    }

    val viewW = viewportSize.width.toFloat().coerceAtLeast(1f)
    val viewH = viewportSize.height.toFloat().coerceAtLeast(1f)
    val maxCropW = viewW * 0.86f
    val maxCropH = viewH * 0.86f
    val targetRatio = aspectPreset.ratio
    val cropW: Float
    val cropH: Float
    if (maxCropW / maxCropH > targetRatio) {
        cropH = maxCropH
        cropW = cropH * targetRatio
    } else {
        cropW = maxCropW
        cropH = cropW / targetRatio
    }

    val bmpW = (displayBitmap?.width ?: 1).toFloat()
    val bmpH = (displayBitmap?.height ?: 1).toFloat()
    val baseScale = max(cropW / bmpW, cropH / bmpH)
    val effectiveScale = baseScale * zoom
    val scaledW = bmpW * effectiveScale
    val scaledH = bmpH * effectiveScale
    val maxPanX = ((scaledW - cropW) / 2f).coerceAtLeast(0f)
    val maxPanY = ((scaledH - cropH) / 2f).coerceAtLeast(0f)
    val clampedPanX = panX.coerceIn(-maxPanX, maxPanX)
    val clampedPanY = panY.coerceIn(-maxPanY, maxPanY)

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        TuiCard(
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("image_crop_dialog")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        TuiSectionHeader(text = title)
                        Text(
                            text = "Pinch or drag to position · adjust zoom & rotate",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                rotationDeg = (rotationDeg + 90) % 360
                                panX = 0f
                                panY = 0f
                            },
                            enabled = displayBitmap != null && !isSaving,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate 90 degrees",
                                tint = tokens.accent
                            )
                        }
                        IconButton(
                            onClick = {
                                zoom = 1f
                                panX = 0f
                                panY = 0f
                                rotationDeg = 0
                            },
                            enabled = displayBitmap != null && !isSaving,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset crop",
                                tint = tokens.textPrimary
                            )
                        }
                    }
                }

                if (!isIconCrop) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CropAspectPreset.entries.forEach { preset ->
                            val selected = aspectPreset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(if (selected) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                    .border(
                                        1.dp,
                                        if (selected) tokens.accent else tokens.border,
                                        RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                    .clickable {
                                        aspectPreset = preset
                                        panX = 0f
                                        panY = 0f
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) tokens.pillText else tokens.textPrimary
                                )
                            }
                        }
                    }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(false to "Square guide", true to "Circle guide").forEach { (circle, label) ->
                            val selected = showCircleGuide == circle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                    .background(if (selected) tokens.pillBg else tokens.background.copy(alpha = 0.6f))
                                    .border(
                                        1.dp,
                                        if (selected) tokens.accent else tokens.border,
                                        RoundedCornerShape(tokens.cardCornerRadius)
                                    )
                                    .clickable { showCircleGuide = circle }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) tokens.pillText else tokens.textPrimary
                                )
                            }
                        }
                    }
                }

                // Interactive Crop Canvas Viewport
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(tokens.cardCornerRadius))
                        .background(Color.Black.copy(alpha = 0.88f))
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.cardCornerRadius))
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(displayBitmap, maxPanX, maxPanY, cropW, cropH) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                if (displayBitmap == null) return@detectTransformGestures
                                val nextZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                val nextScale = baseScale * nextZoom
                                val nextMaxX = ((bmpW * nextScale - cropW) / 2f).coerceAtLeast(0f)
                                val nextMaxY = ((bmpH * nextScale - cropH) / 2f).coerceAtLeast(0f)
                                zoom = nextZoom
                                panX = (panX + pan.x).coerceIn(-nextMaxX, nextMaxX)
                                panY = (panY + pan.y).coerceIn(-nextMaxY, nextMaxY)
                            }
                        }
                ) {
                    when {
                        loadError -> {
                            Text(
                                text = "Could not decode selected image",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        displayBitmap == null -> {
                            CircularProgressIndicator(color = tokens.accent, modifier = Modifier.size(28.dp))
                        }
                        else -> {
                            val imageBmp = remember(displayBitmap) { displayBitmap.asImageBitmap() }
                            val accent = tokens.accent
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val imgLeft = size.width / 2f + clampedPanX - scaledW / 2f
                                val imgTop = size.height / 2f + clampedPanY - scaledH / 2f

                                drawImage(
                                    image = imageBmp,
                                    dstOffset = IntOffset(imgLeft.roundToInt(), imgTop.roundToInt()),
                                    dstSize = IntSize(scaledW.roundToInt().coerceAtLeast(1), scaledH.roundToInt().coerceAtLeast(1))
                                )

                                val cropLeft = (size.width - cropW) / 2f
                                val cropTop = (size.height - cropH) / 2f
                                val cropRight = cropLeft + cropW
                                val cropBottom = cropTop + cropH

                                // Darkened outer scrim outside crop frame
                                val scrimPath = Path().apply {
                                    fillType = PathFillType.EvenOdd
                                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                                    addRect(androidx.compose.ui.geometry.Rect(cropLeft, cropTop, cropRight, cropBottom))
                                }
                                drawPath(path = scrimPath, color = Color.Black.copy(alpha = 0.58f))

                                // Rule-of-thirds guides
                                val thirdColor = Color.White.copy(alpha = 0.35f)
                                drawLine(
                                    color = thirdColor,
                                    start = Offset(cropLeft + cropW / 3f, cropTop),
                                    end = Offset(cropLeft + cropW / 3f, cropBottom),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = thirdColor,
                                    start = Offset(cropLeft + cropW * 2f / 3f, cropTop),
                                    end = Offset(cropLeft + cropW * 2f / 3f, cropBottom),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = thirdColor,
                                    start = Offset(cropLeft, cropTop + cropH / 3f),
                                    end = Offset(cropRight, cropTop + cropH / 3f),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = thirdColor,
                                    start = Offset(cropLeft, cropTop + cropH * 2f / 3f),
                                    end = Offset(cropRight, cropTop + cropH * 2f / 3f),
                                    strokeWidth = 1f
                                )

                                // Optional circular icon preview guide
                                if (showCircleGuide) {
                                    drawCircle(
                                        color = accent.copy(alpha = 0.7f),
                                        radius = min(cropW, cropH) / 2f,
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        style = Stroke(width = 2f)
                                    )
                                }

                                // Crop border
                                drawRect(
                                    color = accent,
                                    topLeft = Offset(cropLeft, cropTop),
                                    size = Size(cropW, cropH),
                                    style = Stroke(width = 2.5f)
                                )
                            }
                        }
                    }
                }

                // Fine Zoom Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "zoom",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textMuted
                    )
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = zoom,
                        onValueChange = { nextZoom ->
                            val nextScale = baseScale * nextZoom
                            val nextMaxX = ((bmpW * nextScale - cropW) / 2f).coerceAtLeast(0f)
                            val nextMaxY = ((bmpH * nextScale - cropH) / 2f).coerceAtLeast(0f)
                            zoom = nextZoom
                            panX = panX.coerceIn(-nextMaxX, nextMaxX)
                            panY = panY.coerceIn(-nextMaxY, nextMaxY)
                        },
                        valueRange = 1f..5f,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.accent,
                            activeTrackColor = tokens.accent,
                            inactiveTrackColor = tokens.border
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${String.format("%.1f", zoom)}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = tokens.accent
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text("Cancel", color = tokens.textMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = displayBitmap != null && !isSaving,
                        onClick = {
                            val bmp = displayBitmap ?: return@Button
                            isSaving = true
                            scope.launch {
                                val cropLeft = (viewW - cropW) / 2f
                                val cropTop = (viewH - cropH) / 2f
                                val imgLeft = viewW / 2f + clampedPanX - scaledW / 2f
                                val imgTop = viewH / 2f + clampedPanY - scaledH / 2f

                                val srcLeft = ((cropLeft - imgLeft) / effectiveScale).roundToInt()
                                    .coerceIn(0, (bmp.width - 1).coerceAtLeast(0))
                                val srcTop = ((cropTop - imgTop) / effectiveScale).roundToInt()
                                    .coerceIn(0, (bmp.height - 1).coerceAtLeast(0))
                                val srcW = (cropW / effectiveScale).roundToInt()
                                    .coerceIn(1, (bmp.width - srcLeft).coerceAtLeast(1))
                                val srcH = (cropH / effectiveScale).roundToInt()
                                    .coerceIn(1, (bmp.height - srcTop).coerceAtLeast(1))

                                val saved = saveCroppedBitmap(
                                    context = context,
                                    bitmap = bmp,
                                    srcRect = Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH),
                                    targetMaxSide = if (isIconCrop) 512 else 1080,
                                    prefix = outputPrefix,
                                    preserveAlpha = isIconCrop
                                )
                                isSaving = false
                                if (saved != null) {
                                    onCropped(saved)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.pillBg,
                            contentColor = tokens.pillText
                        ),
                        shape = RoundedCornerShape(tokens.cardCornerRadius),
                        modifier = Modifier.testTag("apply_crop_button")
                    ) {
                        Text(if (isSaving) "Saving..." else "Apply Crop")
                    }
                }
            }
        }
    }
}

private suspend fun loadDownsampledBitmap(
    context: Context,
    uri: Uri,
    maxDim: Int
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = max(w, h)
                if (largest > maxDim) {
                    val scale = maxDim.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (w * scale).roundToInt().coerceAtLeast(1),
                        (h * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            var inSampleSize = 1
            while (max(boundsOpts.outWidth, boundsOpts.outHeight) / inSampleSize > maxDim * 2) {
                inSampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return@runCatching null

            val exifRotation = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            if (exifRotation != 0f) {
                val m = Matrix().apply { postRotate(exifRotation) }
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
            } else {
                raw
            }
        }
    }.getOrNull()
}

private suspend fun saveCroppedBitmap(
    context: Context,
    bitmap: Bitmap,
    srcRect: Rect,
    targetMaxSide: Int,
    prefix: String,
    preserveAlpha: Boolean
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val srcW = srcRect.width().coerceAtLeast(1)
        val srcH = srcRect.height().coerceAtLeast(1)
        val scale = min(1f, targetMaxSide.toFloat() / max(srcW, srcH).toFloat())
        val outW = (srcW * scale).roundToInt().coerceAtLeast(1)
        val outH = (srcH * scale).roundToInt().coerceAtLeast(1)

        val outBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, Rect(0, 0, outW, outH), paint)

        val dir = File(context.filesDir, "user_media").apply { mkdirs() }
        val ext = if (preserveAlpha) "png" else "jpg"
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { out ->
            if (preserveAlpha) {
                outBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                outBitmap.compress(Bitmap.CompressFormat.JPEG, 93, out)
            }
        }
        outBitmap.recycle()
        file.absolutePath
    }.getOrNull()
}
