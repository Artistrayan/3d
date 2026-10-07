package com.example.engine

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.model.PbrMaterial
import com.example.model.TextureType
import com.example.model.UvMappingMode
import com.example.model.Vec3
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Procedural PBR Texture Generator, 3D UV Mapper, 2D Custom Texture Painter, and PNG Texture Baker.
 */
object TextureEngine {

    fun computeUV(
        localPos: Vec3,
        normal: Vec3,
        mode: UvMappingMode
    ): Pair<Float, Float> {
        val rawU: Float
        val rawV: Float

        when (mode) {
            UvMappingMode.TRIPLANAR_BOX -> {
                val ax = abs(normal.x)
                val ay = abs(normal.y)
                val az = abs(normal.z)
                if (ay >= ax && ay >= az) {
                    rawU = localPos.x * 0.5f + 0.5f
                    rawV = localPos.z * 0.5f + 0.5f
                } else if (ax >= ay && ax >= az) {
                    rawU = localPos.z * 0.5f + 0.5f
                    rawV = localPos.y * 0.5f + 0.5f
                } else {
                    rawU = localPos.x * 0.5f + 0.5f
                    rawV = localPos.y * 0.5f + 0.5f
                }
            }
            UvMappingMode.SPHERICAL -> {
                val n = localPos.normalized()
                rawU = (0.5f + atan2(n.z, n.x) / (2f * PI.toFloat()))
                rawV = (0.5f - asinSafe(n.y) / PI.toFloat())
            }
            UvMappingMode.CYLINDRICAL -> {
                rawU = (0.5f + atan2(localPos.z, localPos.x) / (2f * PI.toFloat()))
                rawV = localPos.y * 0.45f + 0.5f
            }
            UvMappingMode.PLANAR_XY -> {
                rawU = localPos.x * 0.45f + 0.5f
                rawV = localPos.y * 0.45f + 0.5f
            }
        }

        val u = rawU - floor(rawU)
        val v = rawV - floor(rawV)
        return u.coerceIn(0f, 1f) to v.coerceIn(0f, 1f)
    }

    private fun asinSafe(v: Float): Float {
        val clamped = v.coerceIn(-1f, 1f)
        return (PI * 0.5 - acos(clamped.toDouble())).toFloat()
    }

    fun sampleTextureAndRoughness(
        u: Float,
        v: Float,
        material: PbrMaterial
    ): Pair<Color, Float> {
        val primary = Color(material.baseColorHex)
        val secondary = Color(material.textureSecondaryHex)
        val scale = material.textureScale.coerceIn(1f, 16f)
        val blend = material.textureBlend.coerceIn(0f, 1f)

        val su = u * scale
        val sv = v * scale

        var patternFactor = 0f
        var roughnessDelta = 0f

        when (material.textureType) {
            TextureType.NONE -> {
                patternFactor = 0f
            }
            TextureType.CHECKER_UV -> {
                val cx = floor(su * 2f).toInt()
                val cy = floor(sv * 2f).toInt()
                val isOdd = ((cx + cy) and 1) != 0
                patternFactor = if (isOdd) 1f else 0f
                roughnessDelta = if (isOdd) 0.08f else -0.04f
            }
            TextureType.CARBON_WEAVE -> {
                val fx = (su * 4f) - floor(su * 4f)
                val fy = (sv * 4f) - floor(sv * 4f)
                val cellX = floor(su * 4f).toInt()
                val cellY = floor(sv * 4f).toInt()
                val diag = ((cellX + cellY) and 1) == 0
                val ridge = if (diag) sin(fx * PI.toFloat()) else sin(fy * PI.toFloat())
                patternFactor = (1f - ridge * 0.85f).coerceIn(0f, 1f)
                roughnessDelta = (patternFactor - 0.4f) * 0.25f
            }
            TextureType.HEX_ARMOR -> {
                val hx = su * 2.2f
                val hy = sv * 2.2f * 1.1547f
                val row = floor(hy).toInt()
                val offsetHx = if (row % 2 == 0) hx else hx + 0.5f
                val localX = (offsetHx - floor(offsetHx)) - 0.5f
                val localY = (hy - floor(hy)) - 0.5f
                val dist = max(abs(localX), abs(localX) * 0.5f + abs(localY) * 0.866f)
                patternFactor = if (dist > 0.41f) 1f else (dist * 0.45f)
                roughnessDelta = if (dist > 0.41f) 0.25f else -0.08f
            }
            TextureType.MARBLE_VEINS -> {
                val n = trigTurbulence(su * 1.4f, sv * 1.4f)
                val vein = abs(sin((su + sv + n * 1.8f) * PI.toFloat() * 1.5f))
                val sharpVein = (1f - vein).let { it * it * it }
                patternFactor = sharpVein.coerceIn(0f, 1f)
                roughnessDelta = sharpVein * 0.12f - 0.06f
            }
            TextureType.WOOD_GRAIN -> {
                val cx = su - scale * 0.5f
                val cy = sv - scale * 0.5f
                val radial = sqrt(cx * cx + cy * cy * 0.25f) * 3.5f
                val wobble = sin(su * 3.2f) * 0.35f + cos(sv * 2.4f) * 0.25f
                val rings = (sin((radial + wobble) * PI.toFloat() * 2f) * 0.5f + 0.5f)
                patternFactor = rings.coerceIn(0f, 1f)
                roughnessDelta = (rings - 0.5f) * 0.16f
            }
            TextureType.CYBER_CIRCUIT -> {
                val gx = floor(su * 3f).toInt()
                val gy = floor(sv * 3f).toInt()
                val lx = (su * 3f) - floor(su * 3f)
                val ly = (sv * 3f) - floor(sv * 3f)
                val hash = ((gx * 73856093) xor (gy * 19349663)) and 0xFF
                val isTraceH = abs(ly - 0.5f) < 0.09f && (hash % 3 != 0)
                val isTraceV = abs(lx - 0.5f) < 0.09f && (hash % 2 == 0)
                val viaDist = sqrt((lx - 0.5f) * (lx - 0.5f) + (ly - 0.5f) * (ly - 0.5f))
                val isPad = viaDist < 0.22f && viaDist > 0.10f && (hash % 4 == 0)
                patternFactor = if (isTraceH || isTraceV || isPad) 0f else 1f
                roughnessDelta = if (patternFactor < 0.5f) -0.15f else 0.15f
            }
            TextureType.BRUSHED_METAL -> {
                val streak = sin(sv * 42f + sin(su * 8f) * 2.5f) * 0.5f + 0.5f
                val fine = cos(sv * 95f) * 0.25f + 0.25f
                patternFactor = ((streak * 0.7f + fine * 0.3f) * 0.55f).coerceIn(0f, 1f)
                roughnessDelta = (patternFactor - 0.25f) * 0.2f
            }
            TextureType.RUST_WEATHERED -> {
                val n1 = trigTurbulence(su * 2.1f, sv * 2.1f)
                val n2 = trigTurbulence(su * 5.3f + 3.1f, sv * 5.3f + 1.7f)
                val rustMask = ((n1 * 0.65f + n2 * 0.35f) * 1.25f - 0.2f).coerceIn(0f, 1f)
                patternFactor = rustMask
                roughnessDelta = rustMask * 0.38f
            }
            TextureType.BRICK_TILES -> {
                val row = floor(sv * 2.5f).toInt()
                val brickU = su * 1.25f + if (row % 2 != 0) 0.5f else 0f
                val lx = brickU - floor(brickU)
                val ly = (sv * 2.5f) - floor(sv * 2.5f)
                val isMortar = lx < 0.08f || ly < 0.11f
                patternFactor = if (isMortar) 1f else 0f
                roughnessDelta = if (isMortar) 0.28f else 0.04f
            }
            TextureType.CAMO_TACTICAL -> {
                val n1 = sin(su * 3.2f + cos(sv * 2.8f) * 1.9f)
                val n2 = cos(sv * 3.6f - sin(su * 2.5f) * 2.1f)
                patternFactor = when {
                    n1 + n2 > 0.55f -> 1f
                    n1 - n2 < -0.45f -> 0.55f
                    else -> 0f
                }
            }
            TextureType.CUSTOM_PAINT -> {
                patternFactor = 0f
            }
        }

        val effectiveFactor = (patternFactor * blend).coerceIn(0f, 1f)
        var outR = primary.red * (1f - effectiveFactor) + secondary.red * effectiveFactor
        var outG = primary.green * (1f - effectiveFactor) + secondary.green * effectiveFactor
        var outB = primary.blue * (1f - effectiveFactor) + secondary.blue * effectiveFactor

        if (material.paintStrokes.isNotEmpty()) {
            for (stroke in material.paintStrokes) {
                val distSq = distanceToSegmentSq(u, v, stroke.u0, stroke.v0, stroke.u1, stroke.v1)
                val rSq = stroke.radiusUv * stroke.radiusUv
                if (distSq <= rSq) {
                    val strokeCol = Color(stroke.colorHex)
                    val edgeSoftness = (1f - (distSq / rSq) * 0.35f).coerceIn(0.4f, 1f)
                    outR = outR * (1f - edgeSoftness) + strokeCol.red * edgeSoftness
                    outG = outG * (1f - edgeSoftness) + strokeCol.green * edgeSoftness
                    outB = outB * (1f - edgeSoftness) + strokeCol.blue * edgeSoftness
                }
            }
        }

        return Color(outR.coerceIn(0f, 1f), outG.coerceIn(0f, 1f), outB.coerceIn(0f, 1f), material.opacity) to roughnessDelta
    }

    private fun trigTurbulence(x: Float, y: Float): Float {
        val v1 = sin(x * 2.3f + cos(y * 2.1f))
        val v2 = cos(x * 4.7f - sin(y * 4.3f)) * 0.5f
        val v3 = sin((x + y) * 7.1f) * 0.25f
        return ((v1 + v2 + v3) / 1.75f) * 0.5f + 0.5f
    }

    private fun distanceToSegmentSq(
        px: Float, py: Float,
        x0: Float, y0: Float,
        x1: Float, y1: Float
    ): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-6f) {
            val ex = px - x0
            val ey = py - y0
            return ex * ex + ey * ey
        }
        val t = (((px - x0) * dx + (py - y0) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = x0 + t * dx
        val projY = y0 + t * dy
        val ex = px - projX
        val ey = py - projY
        return ex * ex + ey * ey
    }

    fun renderTextureToBitmap(material: PbrMaterial, sizePx: Int = 256): Bitmap {
        val dim = sizePx.coerceIn(64, 1024)
        val pixels = IntArray(dim * dim)
        for (y in 0 until dim) {
            val v = y.toFloat() / (dim - 1).toFloat()
            val rowOffset = y * dim
            for (x in 0 until dim) {
                val u = x.toFloat() / (dim - 1).toFloat()
                val (col, _) = sampleTextureAndRoughness(u, v, material)
                pixels[rowOffset + x] = col.copy(alpha = 1f).toArgb()
            }
        }
        return Bitmap.createBitmap(pixels, dim, dim, Bitmap.Config.ARGB_8888)
    }

    fun encodeTextureToPngBytes(material: PbrMaterial, sizePx: Int = 256): ByteArray {
        val bmp = renderTextureToBitmap(material, sizePx)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }
}
