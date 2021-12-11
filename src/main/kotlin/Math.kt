import kotlin.math.min
import kotlin.math.max

fun lerp(begin: Float, end: Float, period: Float) = begin + (end - begin) * period

fun clamp(value: Float, min: Float = 0f, max: Float = 1f) = min(max, max(min, value))