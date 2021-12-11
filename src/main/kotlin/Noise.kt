import androidx.compose.ui.geometry.Size
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.round

val sizeOne = Size(1f, 1f)

fun generatePixelsScaledWithDepth(
    width: Int,
    height: Int,
    depth: Int,
    scale: Int
): ArrayList<ArrayList<ArrayList<Float>>> {
    val randomPoints = ArrayList<ArrayList<ArrayList<Float>>>()
    for (d in 1..depth) {
        val squareSize = Size(
            d * scale * 1f,
            d * scale * 1f
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
    scale: Int
): Array<Array<Float>> {
    val depth = points.size

    val randomPoints2D = Array(width) { Array(height) { 0.5f } }

    for (d in 0 until depth) {
        val squareSize = Size(
            round((d + 1f) * scale),
            round((d + 1f) * scale)
        )
        for (x in 0 until width) {
            for (y in 0 until height) {
                val xScale = round(x / squareSize.width).toInt()
                val yScale = round(y / squareSize.height).toInt()

//                println("SquareWidth: ${squareSize.width}\nPoint x size $xScale/$x = ${points[d].size}/$width")
//                println("SquareHeight: ${squareSize.height}\nPoint y size $yScale/$y = ${points[d][xScale].size}/$height")

                var point = 0.5f
                var lastPoint = 0.5f
                if (xScale < points[d].size && yScale < points[d][xScale].size) {
                    lastPoint = points[d][xScale + if (xScale > 0) -1 else 0][yScale + if (yScale > 0) -1 else 0]
                    point = points[d][xScale][yScale]
//                    println("lp: $lastPoint // p: $point / c: ${randomPoints2D[x][y]}")
                }

                val firstSign = (point - lastPoint) / abs(point - lastPoint)
                val firstLerp = if (lastPoint != point) clamp(lerp(lastPoint, point, .3f * firstSign)) else point

                val secondSign = (firstLerp - randomPoints2D[x][y]) / abs(firstLerp - randomPoints2D[x][y])
                val secondLerp = if (randomPoints2D[x][y] != firstLerp) clamp(
                    lerp(
                        randomPoints2D[x][y],
                        firstLerp,
                        .3f * secondSign / depth
                    )
                ) else firstLerp

//                println("lerps: $firstLerp / $secondLerp")
//                println("x${x}y$y $secondLerp")

//                if (x == 0 && y == 0) println("Zero: $lastPoint / $point - $firstLerp / $secondLerp")

                randomPoints2D[x][y] = secondLerp
            }
        }
    }
    return randomPoints2D
}