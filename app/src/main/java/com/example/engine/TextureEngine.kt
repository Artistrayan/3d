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
            TextureType.PHOTO_TEXTURE -> {
                patternFactor = 0f
            }
        }

        val effectiveFactor = (patternFactor * blend).coerceIn(0f, 1f)
        var outR = primary.red * (1f - effectiveFactor) + secondary.red * effectiveFactor
        var outG = primary.green * (1f - effectiveFactor) + secondary.green * effectiveFactor
        var outB = primary.blue * (1f - effectiveFactor) + secondary.blue * effectiveFactor

        // Sample imported photo pixels if present
        if (material.customPhotoPixels.isNotEmpty() && material.customPhotoSize > 0) {
            val dim = material.customPhotoSize
            val px = ((u * (dim - 1)).toInt()).coerceIn(0, dim - 1)
            val py = (((1f - v) * (dim - 1)).toInt()).coerceIn(0, dim - 1)
            val idx = (py * dim + px).coerceIn(0, material.customPhotoPixels.lastIndex)
            val argb = material.customPhotoPixels[idx]
            val pr = ((argb shr 16) and 0xFF) / 255f
            val pg = ((argb shr 8) and 0xFF) / 255f
            val pb = (argb and 0xFF) / 255f
            outR = outR * (1f - blend) + pr * blend
            outG = outG * (1f - blend) + pg * blend
            outB = outB * (1f - blend) + pb * blend
        }

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

    /**
     * Downsamples an imported Bitmap into a square pixel grid (default 48x48) for fast real-time 3D UV sampling.
     */
    fun extractPhotoPixelsGrid(bitmap: Bitmap, gridSize: Int = 48): List<Int> {
        val scaled = Bitmap.createScaledBitmap(bitmap, gridSize, gridSize, true)
        val arr = IntArray(gridSize * gridSize)
        scaled.getPixels(arr, 0, gridSize, 0, 0, gridSize, gridSize)
        return arr.toList()
    }

    /**
     * Converts an imported 2D photo/image into a watertight 3D Relief / Heightmap / Lithophane Mesh
     * with a solid backing plate and baked photo colors.
     */
    fun create3DReliefMeshFromPhoto(
        bitmap: Bitmap,
        displayName: String,
        gridRes: Int = 34,
        reliefHeight: Float = 0.42f
    ): com.example.model.SceneNode3D {
        val res = gridRes.coerceIn(16, 56)
        val scaled = Bitmap.createScaledBitmap(bitmap, res, res, true)
        val pixels = IntArray(res * res)
        scaled.getPixels(pixels, 0, res, 0, 0, res, res)

        val verts = ArrayList<Vec3>(res * res * 2)
        val faces = ArrayList<com.example.model.TriangleFace>((res - 1) * (res - 1) * 4 + res * 8)
        val halfSpan = 1.05f
        val baseY = -0.14f

        // 1. Top relief vertices displaced by pixel luminance
        for (gy in 0 until res) {
            val v = gy.toFloat() / (res - 1).toFloat()
            val z = (v - 0.5f) * 2f * halfSpan
            for (gx in 0 until res) {
                val u = gx.toFloat() / (res - 1).toFloat()
                val x = (u - 0.5f) * 2f * halfSpan
                val argb = pixels[gy * res + gx]
                val r = ((argb shr 16) and 0xFF) / 255f
                val g = ((argb shr 8) and 0xFF) / 255f
                val b = (argb and 0xFF) / 255f
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).coerceIn(0f, 1f)
                val y = 0.04f + lum * reliefHeight
                verts.add(Vec3(x, y, z))
            }
        }

        // 2. Bottom flat base vertices for watertight 3D printability
        val bottomOffset = verts.size
        for (gy in 0 until res) {
            val v = gy.toFloat() / (res - 1).toFloat()
            val z = (v - 0.5f) * 2f * halfSpan
            for (gx in 0 until res) {
                val u = gx.toFloat() / (res - 1).toFloat()
                val x = (u - 0.5f) * 2f * halfSpan
                verts.add(Vec3(x, baseY, z))
            }
        }

        // 3. Top and Bottom grid triangles
        for (gy in 0 until res - 1) {
            for (gx in 0 until res - 1) {
                val i00 = gy * res + gx
                val i10 = i00 + 1
                val i01 = (gy + 1) * res + gx
                val i11 = i01 + 1

                faces.add(com.example.model.TriangleFace(i00, i01, i10))
                faces.add(com.example.model.TriangleFace(i10, i01, i11))

                val b00 = bottomOffset + i00
                val b10 = bottomOffset + i10
                val b01 = bottomOffset + i01
                val b11 = bottomOffset + i11
                faces.add(com.example.model.TriangleFace(b00, b10, b01))
                faces.add(com.example.model.TriangleFace(b10, b11, b01))
            }
        }

        // 4. Side walls stitching top relief to bottom base
        for (i in 0 until res - 1) {
            // Front & Back edges
            val tFront0 = i; val tFront1 = i + 1
            val bFront0 = bottomOffset + tFront0; val bFront1 = bottomOffset + tFront1
            faces.add(com.example.model.TriangleFace(tFront0, tFront1, bFront1))
            faces.add(com.example.model.TriangleFace(tFront0, bFront1, bFront0))

            val tBack0 = (res - 1) * res + i; val tBack1 = tBack0 + 1
            val bBack0 = bottomOffset + tBack0; val bBack1 = bottomOffset + tBack1
            faces.add(com.example.model.TriangleFace(tBack1, tBack0, bBack0))
            faces.add(com.example.model.TriangleFace(tBack1, bBack0, bBack1))

            // Left & Right edges
            val tLeft0 = i * res; val tLeft1 = (i + 1) * res
            val bLeft0 = bottomOffset + tLeft0; val bLeft1 = bottomOffset + tLeft1
            faces.add(com.example.model.TriangleFace(tLeft1, tLeft0, bLeft0))
            faces.add(com.example.model.TriangleFace(tLeft1, bLeft0, bLeft1))

            val tRight0 = i * res + (res - 1); val tRight1 = (i + 1) * res + (res - 1)
            val bRight0 = bottomOffset + tRight0; val bRight1 = bottomOffset + tRight1
            faces.add(com.example.model.TriangleFace(tRight0, tRight1, bRight1))
            faces.add(com.example.model.TriangleFace(tRight0, bRight1, bRight0))
        }

        val texGrid = extractPhotoPixelsGrid(bitmap, 48)
        val cleanTitle = displayName.substringBeforeLast('.').ifBlank { "Photo_Relief_3D" }

        return com.example.model.SceneNode3D(
            id = "photo3d_${java.util.UUID.randomUUID().toString().take(6)}",
            name = "3D Photo Relief ($cleanTitle)",
            nameFa = "مدل سه‌بعدی عکس ($cleanTitle)",
            typeTag = "Photo3D",
            vertices = verts,
            faces = faces,
            material = PbrMaterial(
                name = "Photo Relief Material",
                nameFa = "تکسچر عکس واردشده",
                baseColorHex = 0xFFFFFFFF,
                metallic = 0.18f,
                roughness = 0.35f,
                textureType = TextureType.PHOTO_TEXTURE,
                textureScale = 1.0f,
                textureBlend = 0.96f,
                uvMappingMode = UvMappingMode.TRIPLANAR_BOX,
                customPhotoPixels = texGrid,
                customPhotoSize = 48
            )
        )
    }
}
