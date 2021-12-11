// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Checkbox
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.util.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Exception
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

val coroutineScope = CoroutineScope(Dispatchers.Main)
val mutex = Mutex()

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Preview
fun App() {
    var depth: Float by remember { mutableStateOf(1f) }
    var scale: Float by remember { mutableStateOf(1f) }
    var isClamped: Boolean by remember { mutableStateOf(false) }

    val generationFlow = MutableStateFlow(Pair(false, "Init..."))
    var generationState by remember { mutableStateOf(generationFlow.value.second) }
    var generationFinished by remember { mutableStateOf(generationFlow.value.first) }

    generationFlow
        .onEach {
            generationState = it.second
            generationFinished = it.first
        }
        .launchIn(coroutineScope)

    Row(
        Modifier
            .fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(.3f)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Text("Depth - $depth")
                Slider(
                    value = depth,
                    onValueChange = {
                        depth = it
                    },
                    steps = 0,
                    valueRange = 1f.rangeTo(100f)
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Text("Scale - $scale")
                Slider(
                    value = scale,
                    onValueChange = {
                        scale = it
                    },
                    steps = 0,
                    valueRange = 1f.rangeTo(20f)
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Text("Clamped")
                Checkbox(
                    isClamped,
                    onCheckedChange = {
                        isClamped = it
                    }
                )
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            noise(
                depth = depth.toInt(),
                scale = scale,
                isClamped = isClamped,
                onGeneration = {
                    generationFlow.value = Pair(false, it)
                },
                onGenerated = {
                    generationFlow.value = Pair(true, "Generated")
                })
        }
    }

    AnimatedVisibility(!generationFinished) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .6f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(generationState, style = TextStyle(color = Color.White))
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

fun DrawScope.noise(
    depth: Int = 1,
    scale: Float = 1f,
    isClamped: Boolean = false,
    onGeneration: (String) -> Unit,
    onGenerated: () -> Unit
) {
    val size = size
    val width = size.width.toInt()
    val height = size.height.toInt()

    onGeneration(
        """
        Generate the first map of points
        0%
        """.trimIndent()
    )

    val randomPoints = generatePixelsScaledWithDepth(width, height, depth, scale)
    val compressedPoints = compressPoints(randomPoints, width, height, scale)

    onGeneration(
        """
        Drawing the points
        """.trimIndent()
    )

        for (x in 0 until width) {
            for (y in 0 until height) {
                val colorValue = compressedPoints[x][y].let {
                    if (isClamped)
                        if (it >= .5f) 1f else 0f
                    else it
                }
                val color = Color(colorValue, colorValue, colorValue)
                val squareSize = sizeOne * scale
                drawRect(
                    color = color,
                    topLeft = Offset(floor(x * squareSize.width), floor(y * squareSize.height)),
                    size = squareSize,
                    style = Fill
                )
            }
        }

    onGenerated()
}

fun generatePixelsScaledWithDepth(
    width: Int,
    height: Int,
    depth: Int,
    scale: Float
): ArrayList<ArrayList<ArrayList<Float>>> {
    val randomPoints = ArrayList<ArrayList<ArrayList<Float>>>()
    for (d in 1..depth) {
        val squareSize = Size(
            d * scale,
            d * scale
        )
        val scaleWidth = (width / squareSize.width).toInt()
        val scaleHeight = (height / squareSize.height).toInt()

        val widthArray = ArrayList<ArrayList<Float>>()

        for (x in 0 until scaleWidth) {
            val heightArray = ArrayList<Float>()
            for (y in 0 until scaleHeight) {
                heightArray.add(Math.random().toFloat())
            }
            widthArray.add(heightArray)
        }
        randomPoints.add(widthArray)
    }
    return randomPoints
}

fun compressPoints(
    points: ArrayList<ArrayList<ArrayList<Float>>>,
    width: Int,
    height: Int,
    scale: Float
): Array<Array<Float>> {
    val depth = points.size

    val randomPoints2D = Array(width) { Array(height) { 0f } }

    for (d in 0 until depth) {
        val squareSize = Size(
            round((d + 1) * scale),
            round((d + 1) * scale)
        )
        for (x in 0 until width) {
            for (y in 0 until height) {
                val xScale = round(x / squareSize.width).toInt()
                val yScale = round(y / squareSize.height).toInt()

//                println("SquareWidth: ${squareSize.width}\nPoint x size $xScale/$x = ${points[d].size}/$width")
//                println("SquareHeight: ${squareSize.height}\nPoint y size $yScale/$y = ${points[d][xScale].size}/$height")

                var point = points[d][points[d].size - 1][points[d][0].size - 1]
                if (xScale < points[d].size && yScale < points[d][xScale].size) {
                    point = points[d][xScale][yScale]

                }
                randomPoints2D[x][y] += point / depth
            }
        }
    }
    return randomPoints2D
}

val sizeOne = Size(1f, 1f)
