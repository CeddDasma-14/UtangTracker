package com.cedd.utangtracker.presentation.contract

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

data class DrawnPath(val points: List<Offset>)

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSave: (List<DrawnPath>, Int, Int) -> Unit,
    onClear: () -> Unit = {}
) {
    var paths by remember { mutableStateOf(listOf<DrawnPath>()) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.White)
                .onSizeChanged { canvasSize = it }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPoints = listOf(offset) },
                            onDrag = { change, _ ->
                                currentPoints = currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.size > 1) {
                                    paths = paths + DrawnPath(currentPoints)
                                }
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                // Draw completed paths
                paths.forEach { drawnPath ->
                    if (drawnPath.points.size > 1) {
                        val path = Path().apply {
                            moveTo(drawnPath.points.first().x, drawnPath.points.first().y)
                            drawnPath.points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, Color.Black, style = Stroke(width = 3f))
                    }
                }
                // Draw current stroke
                if (currentPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        currentPoints.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, Color.Black, style = Stroke(width = 3f))
                }
                // Border
                drawRect(Color.LightGray, style = Stroke(width = 1f))
            }

            if (paths.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    "Sign here",
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { paths = emptyList(); currentPoints = emptyList(); onClear() },
                modifier = Modifier.weight(1f)
            ) { Text("Clear") }
            Button(
                onClick = { if (paths.isNotEmpty()) onSave(paths, canvasSize.width, canvasSize.height) },
                modifier = Modifier.weight(1f),
                enabled = paths.isNotEmpty()
            ) { Text("Save Signature") }
        }
    }
}

/** Renders drawn paths to a PNG file and returns the file path. */
fun saveSignatureToPng(
    paths: List<DrawnPath>,
    dir: File,
    filename: String,
    canvasWidthPx: Int,
    canvasHeightPx: Int
): String {
    val bmpW = 800
    val bmpH = 200
    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 14f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    val srcW = canvasWidthPx.coerceAtLeast(1).toFloat()
    val srcH = canvasHeightPx.coerceAtLeast(1).toFloat()
    val scaleX = bmpW / srcW
    val scaleY = bmpH / srcH

    paths.forEach { drawnPath ->
        if (drawnPath.points.size > 1) {
            val androidPath = android.graphics.Path()
            androidPath.moveTo(drawnPath.points.first().x * scaleX, drawnPath.points.first().y * scaleY)
            drawnPath.points.drop(1).forEach {
                androidPath.lineTo(it.x * scaleX, it.y * scaleY)
            }
            canvas.drawPath(androidPath, paint)
        }
    }

    dir.mkdirs()
    val file = File(dir, "$filename.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bmp.recycle()
    return file.absolutePath
}
