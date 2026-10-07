package com.example.engine

import com.example.model.PbrMaterial
import com.example.model.SceneNode3D
import com.example.model.TextureType
import com.example.model.TriangleFace
import com.example.model.UvMappingMode
import com.example.model.Vec3
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class PrimitiveType3D(
    val titleFa: String,
    val titleEn: String,
    val categoryFa: String,
    val defaultColorHex: Long
) {
    CUBE("مکعب مهندسی", "Box / Cube", "اشکال پایه", 0xFF00E5FF),
    SPHERE("کره دقیق (UV Sphere)", "UV Sphere", "اشکال پایه", 0xFF38BDF8),
    ICOSPHERE("ایکوسفر ژئودزیک", "Geodesic Icosphere", "اشکال پایه", 0xFF818CF8),
    CYLINDER("استوانه صنعتی", "Cylinder", "اشکال پایه", 0xFF34D399),
    CONE("مخروط", "Cone", "اشکال پایه", 0xFFFBBF24),
    TORUS("حلقه / توروس", "Torus Ring", "اشکال پایه", 0xFFF472B6),
    PYRAMID("هرم چهاروجهی", "Pyramid", "اشکال پایه", 0xFFFB923C),
    HEX_PRISM("منشور شش‌ضلعی (مهره)", "Hex Prism", "قطعات مکانیکی", 0xFF94A3B8),
    CAPSULE("کپسول", "Capsule", "اشکال پایه", 0xFF2DD4BF),
    GEAR("چرخ‌دنده صنعتی", "Mechanical Gear", "قطعات مکانیکی", 0xFFF59E0B),
    TWISTED_VASE("گلدان پارامتریک", "Parametric Vase", "هنری و پرینت 3D", 0xFFA78BFA),
    DIAMOND_GEM("کریستال الماس", "Brilliant Gem", "هنری و پرینت 3D", 0xFF67E8F9)
}

data class ReadyModelPreset(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val badgeFa: String,
    val accentHex: Long,
    val partsCount: Int,
    val generator: () -> List<SceneNode3D>
)

object PrimitiveGenerator {

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().take(6)}"

    val curatedMaterials: List<PbrMaterial> = listOf(
        PbrMaterial("Cyber Cyan Alloy", "آلیاژ سایبری فیروزه‌ای", 0xFF00E5FF, 0.75f, 0.20f, 0xFF00E5FF, 0.15f),
        PbrMaterial("Brushed Titanium", "تیتانیوم برس‌خورده", 0xFF94A3B8, 0.88f, 0.25f, textureType = TextureType.BRUSHED_METAL, textureSecondaryHex = 0xFF475569),
        PbrMaterial("24K Royal Gold", "طلای ۲۴ عیار براق", 0xFFF59E0B, 0.95f, 0.12f),
        PbrMaterial("Sculpt Terracotta", "خشت مجسمه‌سازی (Clay)", 0xFFD97757, 0.05f, 0.65f),
        PbrMaterial("Matte PLA White", "فیلامنت PLA سفید مات", 0xFFF1F5F9, 0.05f, 0.55f),
        PbrMaterial("Carbon Fiber Pro", "بافت فیبر کربن صنعتی", 0xFF334155, 0.55f, 0.28f, textureType = TextureType.CARBON_WEAVE, textureSecondaryHex = 0xFF090D16, textureScale = 5f),
        PbrMaterial("Crimson Anodized", "آلومینیوم آنودایز قرمز", 0xFFEF4444, 0.80f, 0.22f),
        PbrMaterial("Emerald Crystal", "کریستال زمرد نیمه‌شفاف", 0xFF10B981, 0.20f, 0.08f, 0xFF059669, 0.10f, 0.85f),
        PbrMaterial("Cyber PCB Circuit", "مدار الکترونیکی سایبری", 0xFF00E5FF, 0.65f, 0.18f, 0xFF00E5FF, 0.25f, textureType = TextureType.CYBER_CIRCUIT, textureSecondaryHex = 0xFF091526, textureScale = 4f),
        PbrMaterial("Luxury White Marble", "سنگ مرمر سفید رگه‌دار", 0xFFF8FAFC, 0.10f, 0.16f, textureType = TextureType.MARBLE_VEINS, textureSecondaryHex = 0xFF475569, textureScale = 3.5f),
        PbrMaterial("Natural Walnut Wood", "چوب گردو طبیعی", 0xFFD97706, 0.05f, 0.48f, textureType = TextureType.WOOD_GRAIN, textureSecondaryHex = 0xFF78350F, textureScale = 4f),
        PbrMaterial("Sci-Fi Hex Armor", "زره شش‌ضلعی لانه زنبوری", 0xFF38BDF8, 0.78f, 0.22f, textureType = TextureType.HEX_ARMOR, textureSecondaryHex = 0xFF0F172A, textureScale = 5f),
        PbrMaterial("Weathered Industrial Rust", "فلز کهنه و زنگ‌زده", 0xFFEA580C, 0.45f, 0.62f, textureType = TextureType.RUST_WEATHERED, textureSecondaryHex = 0xFF334155, textureScale = 4.5f),
        PbrMaterial("UV Checker Calibration", "شطرنجی استاندارد UV", 0xFF00E5FF, 0.20f, 0.35f, textureType = TextureType.CHECKER_UV, textureSecondaryHex = 0xFF1E293B, textureScale = 4f),
        PbrMaterial("Architectural Red Brick", "آجرنمای معماری", 0xFFEF4444, 0.08f, 0.68f, textureType = TextureType.BRICK_TILES, textureSecondaryHex = 0xFFCBD5E1, textureScale = 4f),
        PbrMaterial("Tactical Camo Armor", "استتار تاکتیکی", 0xFF22C55E, 0.15f, 0.55f, textureType = TextureType.CAMO_TACTICAL, textureSecondaryHex = 0xFF14532D, textureScale = 3.5f)
    )

    fun createPrimitive(
        type: PrimitiveType3D,
        position: Vec3 = Vec3(0f, 0f, 0f),
        scale: Vec3 = Vec3(1f, 1f, 1f),
        rotation: Vec3 = Vec3(0f, 0f, 0f),
        materialOverride: PbrMaterial? = null,
        customNameFa: String? = null,
        customNameEn: String? = null
    ): SceneNode3D {
        val (verts, faces) = when (type) {
            PrimitiveType3D.CUBE -> buildBoxGeometry(1.6f, 1.6f, 1.6f, segments = 2)
            PrimitiveType3D.SPHERE -> buildSphereGeometry(radius = 1.05f, rings = 16, sectors = 24)
            PrimitiveType3D.ICOSPHERE -> buildIcosphereGeometry(radius = 1.1f, subdivisions = 1)
            PrimitiveType3D.CYLINDER -> buildCylinderGeometry(topRadius = 0.85f, bottomRadius = 0.85f, height = 1.8f, sectors = 24)
            PrimitiveType3D.CONE -> buildCylinderGeometry(topRadius = 0.0f, bottomRadius = 1.0f, height = 1.9f, sectors = 24)
            PrimitiveType3D.TORUS -> buildTorusGeometry(majorRadius = 0.95f, minorRadius = 0.34f, majorSegs = 24, minorSegs = 14)
            PrimitiveType3D.PYRAMID -> buildCylinderGeometry(topRadius = 0.0f, bottomRadius = 1.15f, height = 1.8f, sectors = 4)
            PrimitiveType3D.HEX_PRISM -> buildCylinderGeometry(topRadius = 0.95f, bottomRadius = 0.95f, height = 1.4f, sectors = 6)
            PrimitiveType3D.CAPSULE -> buildCapsuleGeometry(radius = 0.65f, cylinderHeight = 1.1f, sectors = 20, rings = 8)
            PrimitiveType3D.GEAR -> buildGearGeometry(outerRadius = 1.2f, innerRadius = 0.92f, holeRadius = 0.40f, thickness = 0.50f, teeth = 14)
            PrimitiveType3D.TWISTED_VASE -> buildTwistedVaseGeometry(height = 2.2f, layers = 18, radialSegs = 22)
            PrimitiveType3D.DIAMOND_GEM -> buildDiamondGemGeometry(radius = 1.1f, height = 1.45f, facets = 14)
        }
        val mat = materialOverride ?: PbrMaterial(
            name = type.titleEn,
            nameFa = type.titleFa,
            baseColorHex = type.defaultColorHex,
            metallic = if (type == PrimitiveType3D.GEAR || type == PrimitiveType3D.HEX_PRISM) 0.82f else 0.35f,
            roughness = if (type == PrimitiveType3D.DIAMOND_GEM) 0.08f else 0.28f
        )
        return SceneNode3D(
            id = newId(type.name.lowercase()),
            name = customNameEn ?: type.titleEn,
            nameFa = customNameFa ?: type.titleFa,
            typeTag = type.name,
            vertices = verts,
            faces = faces,
            position = position,
            rotation = rotation,
            scale = scale,
            material = mat
        )
    }

    fun buildBoxGeometry(w: Float, h: Float, d: Float, segments: Int = 2): Pair<List<Vec3>, List<TriangleFace>> {
        val hx = w / 2f
        val hy = h / 2f
        val hz = d / 2f
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()
        val seg = segments.coerceIn(1, 6)

        fun addGridFace(origin: Vec3, uAxis: Vec3, vAxis: Vec3) {
            val baseIdx = vertices.size
            for (iy in 0..seg) {
                val tv = iy.toFloat() / seg
                for (ix in 0..seg) {
                    val tu = ix.toFloat() / seg
                    vertices.add(origin + uAxis * tu + vAxis * tv)
                }
            }
            val rowLen = seg + 1
            for (iy in 0 until seg) {
                for (ix in 0 until seg) {
                    val i0 = baseIdx + iy * rowLen + ix
                    val i1 = i0 + 1
                    val i2 = i0 + rowLen
                    val i3 = i2 + 1
                    faces.add(TriangleFace(i0, i2, i1))
                    faces.add(TriangleFace(i1, i2, i3))
                }
            }
        }

        addGridFace(Vec3(-hx, -hy, hz), Vec3(w, 0f, 0f), Vec3(0f, h, 0f))
        addGridFace(Vec3(hx, -hy, -hz), Vec3(-w, 0f, 0f), Vec3(0f, h, 0f))
        addGridFace(Vec3(hx, -hy, hz), Vec3(0f, 0f, -d), Vec3(0f, h, 0f))
        addGridFace(Vec3(-hx, -hy, -hz), Vec3(0f, 0f, d), Vec3(0f, h, 0f))
        addGridFace(Vec3(-hx, hy, hz), Vec3(w, 0f, 0f), Vec3(0f, 0f, -d))
        addGridFace(Vec3(-hx, -hy, -hz), Vec3(w, 0f, 0f), Vec3(0f, 0f, d))

        return MeshModifiers.weldCloseVertices(vertices, faces, 1e-4f)
    }

    fun buildSphereGeometry(radius: Float, rings: Int, sectors: Int): Pair<List<Vec3>, List<TriangleFace>> {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()

        val rCount = rings.coerceAtLeast(4)
        val sCount = sectors.coerceAtLeast(6)

        for (r in 0..rCount) {
            val phi = PI * r.toDouble() / rCount
            val y = (cos(phi) * radius).toFloat()
            val sinPhi = sin(phi)
            for (s in 0 until sCount) {
                val theta = 2.0 * PI * s.toDouble() / sCount
                val x = (sinPhi * cos(theta) * radius).toFloat()
                val z = (sinPhi * sin(theta) * radius).toFloat()
                vertices.add(Vec3(x, y, z))
            }
        }

        for (r in 0 until rCount) {
            for (s in 0 until sCount) {
                val nextS = (s + 1) % sCount
                val i0 = r * sCount + s
                val i1 = r * sCount + nextS
                val i2 = (r + 1) * sCount + s
                val i3 = (r + 1) * sCount + nextS
                if (r != 0) faces.add(TriangleFace(i0, i1, i2))
                if (r != rCount - 1) faces.add(TriangleFace(i1, i3, i2))
            }
        }
        return MeshModifiers.weldCloseVertices(vertices, faces, 1e-4f)
    }

    fun buildIcosphereGeometry(radius: Float, subdivisions: Int = 1): Pair<List<Vec3>, List<TriangleFace>> {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
        val verts = listOf(
            Vec3(-1f, t, 0f), Vec3(1f, t, 0f), Vec3(-1f, -t, 0f), Vec3(1f, -t, 0f),
            Vec3(0f, -1f, t), Vec3(0f, 1f, t), Vec3(0f, -1f, -t), Vec3(0f, 1f, -t),
            Vec3(t, 0f, -1f), Vec3(t, 0f, 1f), Vec3(-t, 0f, -1f), Vec3(-t, 0f, 1f)
        ).map { it.normalized() * radius }.toMutableList()

        var faces = listOf(
            TriangleFace(0, 11, 5), TriangleFace(0, 5, 1), TriangleFace(0, 1, 7), TriangleFace(0, 7, 10), TriangleFace(0, 10, 11),
            TriangleFace(1, 5, 9), TriangleFace(5, 11, 4), TriangleFace(11, 10, 2), TriangleFace(10, 7, 6), TriangleFace(7, 1, 8),
            TriangleFace(3, 9, 4), TriangleFace(3, 4, 2), TriangleFace(3, 2, 6), TriangleFace(3, 6, 8), TriangleFace(3, 8, 9),
            TriangleFace(4, 9, 5), TriangleFace(2, 4, 11), TriangleFace(6, 2, 10), TriangleFace(8, 6, 7), TriangleFace(9, 8, 1)
        )

        repeat(subdivisions.coerceIn(0, 2)) {
            val midCache = HashMap<Long, Int>()
            fun midpoint(a: Int, b: Int): Int {
                val minIdx = minOf(a, b).toLong()
                val maxIdx = maxOf(a, b).toLong()
                val key = (minIdx shl 32) or maxIdx
                return midCache.getOrPut(key) {
                    val mid = ((verts[a] + verts[b]) * 0.5f).normalized() * radius
                    verts.add(mid)
                    verts.lastIndex
                }
            }
            val nextFaces = ArrayList<TriangleFace>(faces.size * 4)
            for (f in faces) {
                val ab = midpoint(f.v0, f.v1)
                val bc = midpoint(f.v1, f.v2)
                val ca = midpoint(f.v2, f.v0)
                nextFaces.add(TriangleFace(f.v0, ab, ca))
                nextFaces.add(TriangleFace(f.v1, bc, ab))
                nextFaces.add(TriangleFace(f.v2, ca, bc))
                nextFaces.add(TriangleFace(ab, bc, ca))
            }
            faces = nextFaces
        }
        return verts to faces
    }

    fun buildCylinderGeometry(
        topRadius: Float,
        bottomRadius: Float,
        height: Float,
        sectors: Int
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()
        val halfH = height / 2f
        val sCount = sectors.coerceAtLeast(3)

        for (i in 0 until sCount) {
            val a = 2.0 * PI * i / sCount
            vertices.add(Vec3((cos(a) * bottomRadius).toFloat(), -halfH, (sin(a) * bottomRadius).toFloat()))
        }
        for (i in 0 until sCount) {
            val a = 2.0 * PI * i / sCount
            vertices.add(Vec3((cos(a) * topRadius).toFloat(), halfH, (sin(a) * topRadius).toFloat()))
        }
        val bottomCenterIdx = vertices.size
        vertices.add(Vec3(0f, -halfH, 0f))
        val topCenterIdx = vertices.size
        vertices.add(Vec3(0f, halfH, 0f))

        for (i in 0 until sCount) {
            val next = (i + 1) % sCount
            val b0 = i
            val b1 = next
            val t0 = sCount + i
            val t1 = sCount + next

            faces.add(TriangleFace(b0, t0, b1))
            faces.add(TriangleFace(b1, t0, t1))

            if (bottomRadius > 1e-4f) faces.add(TriangleFace(bottomCenterIdx, b0, b1))
            if (topRadius > 1e-4f) faces.add(TriangleFace(topCenterIdx, t1, t0))
        }
        return MeshModifiers.weldCloseVertices(vertices, faces, 1e-4f)
    }

    fun buildTorusGeometry(
        majorRadius: Float,
        minorRadius: Float,
        majorSegs: Int,
        minorSegs: Int
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()

        for (i in 0 until majorSegs) {
            val u = 2.0 * PI * i / majorSegs
            val cosU = cos(u)
            val sinU = sin(u)
            for (j in 0 until minorSegs) {
                val v = 2.0 * PI * j / minorSegs
                val cosV = cos(v)
                val sinV = sin(v)
                val r = majorRadius + minorRadius * cosV
                val x = (r * cosU).toFloat()
                val y = (minorRadius * sinV).toFloat()
                val z = (r * sinU).toFloat()
                vertices.add(Vec3(x, y, z))
            }
        }

        for (i in 0 until majorSegs) {
            val nextI = (i + 1) % majorSegs
            for (j in 0 until minorSegs) {
                val nextJ = (j + 1) % minorSegs
                val i0 = i * minorSegs + j
                val i1 = nextI * minorSegs + j
                val i2 = nextI * minorSegs + nextJ
                val i3 = i * minorSegs + nextJ
                faces.add(TriangleFace(i0, i2, i1))
                faces.add(TriangleFace(i0, i3, i2))
            }
        }
        return vertices to faces
    }

    fun buildCapsuleGeometry(
        radius: Float,
        cylinderHeight: Float,
        sectors: Int,
        rings: Int
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val (verts, faces) = buildSphereGeometry(radius, rings * 2, sectors)
        val halfCyl = cylinderHeight / 2f
        val stretched = verts.map { v ->
            if (v.y > 0f) v.copy(y = v.y + halfCyl)
            else if (v.y < 0f) v.copy(y = v.y - halfCyl)
            else v
        }
        return stretched to faces
    }

    fun buildGearGeometry(
        outerRadius: Float,
        innerRadius: Float,
        holeRadius: Float,
        thickness: Float,
        teeth: Int
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()
        val steps = teeth * 4
        val halfT = thickness / 2f

        for (i in 0 until steps) {
            val angle = 2.0 * PI * i / steps
            val toothPhase = i % 4
            val rOuter = if (toothPhase == 1 || toothPhase == 2) outerRadius else innerRadius
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            vertices.add(Vec3(cosA * holeRadius, halfT, sinA * holeRadius))
            vertices.add(Vec3(cosA * rOuter, halfT, sinA * rOuter))
            vertices.add(Vec3(cosA * rOuter, -halfT, sinA * rOuter))
            vertices.add(Vec3(cosA * holeRadius, -halfT, sinA * holeRadius))
        }

        for (i in 0 until steps) {
            val next = (i + 1) % steps
            val b0 = i * 4
            val b1 = next * 4

            faces.add(TriangleFace(b0 + 0, b1 + 1, b0 + 1))
            faces.add(TriangleFace(b0 + 0, b1 + 0, b1 + 1))
            faces.add(TriangleFace(b0 + 1, b1 + 2, b0 + 2))
            faces.add(TriangleFace(b0 + 1, b1 + 1, b1 + 2))
            faces.add(TriangleFace(b0 + 2, b1 + 3, b0 + 3))
            faces.add(TriangleFace(b0 + 2, b1 + 2, b1 + 3))
            faces.add(TriangleFace(b0 + 3, b1 + 0, b0 + 0))
            faces.add(TriangleFace(b0 + 3, b1 + 3, b1 + 0))
        }
        return vertices to faces
    }

    fun buildTwistedVaseGeometry(
        height: Float,
        layers: Int,
        radialSegs: Int
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()
        val halfH = height / 2f

        for (ly in 0..layers) {
            val t = ly.toFloat() / layers
            val y = -halfH + t * height
            val profile = 0.62f + 0.28f * sin(t * PI * 1.35).toFloat() - 0.12f * sin(t * PI * 2.7).toFloat()
            val twist = t * (PI * 0.65).toFloat()

            for (s in 0 until radialSegs) {
                val baseAngle = (2.0 * PI * s / radialSegs).toFloat() + twist
                val rib = 1.0f + 0.11f * sin((s * 6.0 * 2.0 * PI / radialSegs)).toFloat()
                val r = profile * rib
                vertices.add(Vec3(cos(baseAngle) * r, y, sin(baseAngle) * r))
            }
        }

        val bottomCenter = vertices.size
        vertices.add(Vec3(0f, -halfH, 0f))
        val topCenter = vertices.size
        vertices.add(Vec3(0f, halfH, 0f))

        for (ly in 0 until layers) {
            for (s in 0 until radialSegs) {
                val nextS = (s + 1) % radialSegs
                val i0 = ly * radialSegs + s
                val i1 = ly * radialSegs + nextS
                val i2 = (ly + 1) * radialSegs + s
                val i3 = (ly + 1) * radialSegs + nextS
                faces.add(TriangleFace(i0, i2, i1))
                faces.add(TriangleFace(i1, i2, i3))
            }
        }

        for (s in 0 until radialSegs) {
            val nextS = (s + 1) % radialSegs
            faces.add(TriangleFace(bottomCenter, s, nextS))
            val topBase = layers * radialSegs
            faces.add(TriangleFace(topCenter, topBase + nextS, topBase + s))
        }
        return vertices to faces
    }

    fun buildDiamondGemGeometry(
        radius: Float,
        height: Float,
        facets: Int
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()
        val fCount = facets.coerceAtLeast(8)

        val girdleY = height * 0.18f
        val tableY = height * 0.48f
        val culetY = -height * 0.52f
        val tableR = radius * 0.56f

        for (i in 0 until fCount) {
            val a = 2.0 * PI * i / fCount
            vertices.add(Vec3((cos(a) * radius).toFloat(), girdleY, (sin(a) * radius).toFloat()))
        }
        for (i in 0 until fCount) {
            val a = 2.0 * PI * (i + 0.5) / fCount
            vertices.add(Vec3((cos(a) * tableR).toFloat(), tableY, (sin(a) * tableR).toFloat()))
        }
        val tableCenterIdx = vertices.size
        vertices.add(Vec3(0f, tableY, 0f))
        val culetIdx = vertices.size
        vertices.add(Vec3(0f, culetY, 0f))

        for (i in 0 until fCount) {
            val next = (i + 1) % fCount
            val g0 = i
            val g1 = next
            val t0 = fCount + i
            val t1 = fCount + next

            faces.add(TriangleFace(g0, t0, g1))
            faces.add(TriangleFace(g1, t0, t1))
            faces.add(TriangleFace(tableCenterIdx, t1, t0))
            faces.add(TriangleFace(culetIdx, g0, g1))
        }
        return vertices to faces
    }

    /**
     * Helper to build continuous revolved/lathe 3D models from a profile of (y, radius, waveAmp, waveFreq, zStretch)
     * so Ready-Made Models are clean, single-piece sculpted 3D meshes instead of scattered primitive blocks!
     */
    private fun buildLatheProfileGeometry(
        rings: List<Triple<Float, Float, Float>>, // (y, radius, ribAmplitude)
        radialSegs: Int = 24,
        ribFreq: Int = 6,
        scaleX: Float = 1f,
        scaleZ: Float = 1f
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<TriangleFace>()

        for ((y, rBase, ribAmp) in rings) {
            for (s in 0 until radialSegs) {
                val angle = (2.0 * PI * s / radialSegs).toFloat()
                val mod = 1f + ribAmp * cos(angle * ribFreq)
                val r = (rBase * mod).coerceAtLeast(0.02f)
                vertices.add(Vec3(cos(angle) * r * scaleX, y, sin(angle) * r * scaleZ))
            }
        }

        val bottomCenter = vertices.size
        vertices.add(Vec3(0f, rings.first().first, 0f))
        val topCenter = vertices.size
        vertices.add(Vec3(0f, rings.last().first, 0f))

        for (ly in 0 until rings.size - 1) {
            for (s in 0 until radialSegs) {
                val nextS = (s + 1) % radialSegs
                val i0 = ly * radialSegs + s
                val i1 = ly * radialSegs + nextS
                val i2 = (ly + 1) * radialSegs + s
                val i3 = (ly + 1) * radialSegs + nextS
                faces.add(TriangleFace(i0, i2, i1))
                faces.add(TriangleFace(i1, i2, i3))
            }
        }

        for (s in 0 until radialSegs) {
            val nextS = (s + 1) % radialSegs
            faces.add(TriangleFace(bottomCenter, s, nextS))
            val topBase = (rings.size - 1) * radialSegs
            faces.add(TriangleFace(topCenter, topBase + nextS, topBase + s))
        }
        return vertices to faces
    }

    /**
     * Returns 8 Clean, Unified, Single-Mesh Ready-Made 3D Models (each with rich procedural textures).
     */
    fun getReadyModelPresets(): List<ReadyModelPreset> = listOf(
        ReadyModelPreset(
            id = "sculpt_twisted_vase",
            titleFa = "گلدان هنری مارپیچ (ویژه پرینت سه‌بعدی)",
            titleEn = "Sculptural Ribbed Vase",
            subtitleFa = "مش یکپارچه و آب‌بند (Manifold) با تکسچر سنگ مرمر آماده اسلایسر پرینتر سه‌بعدی",
            badgeFa = "مش یکپارچه • STL Ready",
            accentHex = 0xFFF472B6,
            partsCount = 1,
            generator = ::buildDesignerVasePreset
        ),
        ReadyModelPreset(
            id = "planetary_gear_pro",
            titleFa = "چرخ‌دنده صنعتی دقیق (Spur Gear)",
            titleEn = "Precision Mechanical Gear",
            subtitleFa = "چرخ‌دنده مهندسی ۱۴ دندانه یکپارچه با تکسچر تیتانیوم برس‌خورده",
            badgeFa = "مهندسی CAD • تک‌قطعه",
            accentHex = 0xFFF59E0B,
            partsCount = 1,
            generator = ::buildPrecisionGearPreset
        ),
        ReadyModelPreset(
            id = "cyber_helmet_bust",
            titleFa = "کلاهخود سایبری یکپارچه (Cyber Helmet)",
            titleEn = "Sci-Fi Cyber Helmet",
            subtitleFa = "مدل سه‌بعدی یکپارچه کلاهخود آینده‌نگرانه با بافت زره شش‌ضلعی (Hex Armor)",
            badgeFa = "تکسچر Hex • Sci-Fi",
            accentHex = 0xFF00E5FF,
            partsCount = 1,
            generator = ::buildCyberHelmetPreset
        ),
        ReadyModelPreset(
            id = "royal_chalice_trophy",
            titleFa = "جام قهرمانی سلطنتی (Royal Trophy Cup)",
            titleEn = "Royal Championship Chalice",
            subtitleFa = "مدل تراش‌خورده یکپارچه جام طلایی ۲۴ عیار مناسب پرینت سه‌بعدی و دکوراسیون",
            badgeFa = "مش یکپارچه • طلایی",
            accentHex = 0xFFFBBF24,
            partsCount = 1,
            generator = ::buildRoyalTrophyPreset
        ),
        ReadyModelPreset(
            id = "parametric_tower_single",
            titleFa = "برج معماری پارامتریک پیچشی",
            titleEn = "Twisted Parametric Skyscraper",
            subtitleFa = "سازه‌ معماری مدرن یکپارچه با چرخش ۶۵ درجه‌ای و تکسچر نما",
            badgeFa = "معماری • Parametric",
            accentHex = 0xFF10B981,
            partsCount = 1,
            generator = ::buildParametricTowerPreset
        ),
        ReadyModelPreset(
            id = "deep_space_rocket",
            titleFa = "موشک فضایی آیرودینامیک (Space Rocket)",
            titleEn = "Aerospace Booster Rocket",
            subtitleFa = "بدنه یکپارچه موشک فضایی شامل دماغه مخروطی، بدنه، بالک‌ها و نازل پیشران",
            badgeFa = "هوافضا • تک‌قطعه",
            accentHex = 0xFF38BDF8,
            partsCount = 1,
            generator = ::buildRocketShipPreset
        ),
        ReadyModelPreset(
            id = "brilliant_crystal_gem",
            titleFa = "جواهر الماس تراش برلیان",
            titleEn = "Brilliant Cut Diamond Gem",
            subtitleFa = "نگین جواهر چندوجهی با ضریب بازتاب بالا و هندسه دقیق جواهرسازی",
            badgeFa = "جواهرسازی • Faceted",
            accentHex = 0xFF67E8F9,
            partsCount = 1,
            generator = ::buildDiamondGemPreset
        ),
        ReadyModelPreset(
            id = "chess_king_piece",
            titleFa = "مهره شاه شطرنج کلاسیک (Chess King)",
            titleEn = "Grandmaster Chess King",
            subtitleFa = "مهره شطرنج تراش‌خورده با بافت چوب گردو طبیعی آماده پرینت سه‌بعدی",
            badgeFa = "تکسچر چوب • پرینت 3D",
            accentHex = 0xFFD97706,
            partsCount = 1,
            generator = ::buildChessKingPreset
        )
    )

    fun buildDesignerVasePreset(): List<SceneNode3D> {
        return listOf(
            createPrimitive(
                PrimitiveType3D.TWISTED_VASE,
                scale = Vec3(1.15f, 1.1f, 1.15f),
                materialOverride = curatedMaterials[9], // Luxury White Marble
                customNameFa = "گلدان پارامتریک مرمر",
                customNameEn = "Marble Ribbed Vase"
            )
        )
    }

    fun buildPrecisionGearPreset(): List<SceneNode3D> {
        return listOf(
            createPrimitive(
                PrimitiveType3D.GEAR,
                rotation = Vec3(25f, 0f, 0f),
                scale = Vec3(1.25f, 1.25f, 1.25f),
                materialOverride = curatedMaterials[1], // Brushed Titanium
                customNameFa = "چرخ‌دنده صنعتی تیتانیوم",
                customNameEn = "Titanium Spur Gear"
            )
        )
    }

    fun buildCyberHelmetPreset(): List<SceneNode3D> {
        val (verts, faces) = buildLatheProfileGeometry(
            rings = listOf(
                Triple(-1.05f, 0.58f, 0.05f),
                Triple(-0.75f, 0.68f, 0.08f),
                Triple(-0.35f, 0.92f, 0.12f),
                Triple(0.05f, 1.02f, 0.10f),
                Triple(0.45f, 0.96f, 0.06f),
                Triple(0.82f, 0.78f, 0.04f),
                Triple(1.08f, 0.42f, 0.02f)
            ),
            radialSegs = 24,
            ribFreq = 4,
            scaleX = 0.88f,
            scaleZ = 1.12f
        )
        return listOf(
            SceneNode3D(
                id = newId("cyber_helmet"),
                name = "Cyber Hex Helmet",
                nameFa = "کلاهخود سایبری Hex",
                typeTag = "Sculpt",
                vertices = verts,
                faces = faces,
                material = curatedMaterials[11] // Sci-Fi Hex Armor
            )
        )
    }

    fun buildRoyalTrophyPreset(): List<SceneNode3D> {
        val (verts, faces) = buildLatheProfileGeometry(
            rings = listOf(
                Triple(-1.15f, 0.78f, 0.0f),
                Triple(-0.95f, 0.72f, 0.0f),
                Triple(-0.80f, 0.26f, 0.04f),
                Triple(-0.35f, 0.18f, 0.06f),
                Triple(0.05f, 0.32f, 0.04f),
                Triple(0.45f, 0.78f, 0.05f),
                Triple(0.85f, 0.96f, 0.05f),
                Triple(1.15f, 0.88f, 0.02f)
            ),
            radialSegs = 28,
            ribFreq = 8
        )
        return listOf(
            SceneNode3D(
                id = newId("trophy_cup"),
                name = "Royal Trophy Chalice",
                nameFa = "جام قهرمانی طلایی",
                typeTag = "Lathe",
                vertices = verts,
                faces = faces,
                material = curatedMaterials[2] // 24K Gold
            )
        )
    }

    fun buildParametricTowerPreset(): List<SceneNode3D> {
        val base = createPrimitive(
            PrimitiveType3D.CUBE,
            scale = Vec3(0.85f, 1.45f, 0.85f),
            materialOverride = curatedMaterials[8], // Cyber PCB Circuit
            customNameFa = "برج معماری پیچشی",
            customNameEn = "Twisted Parametric Tower"
        )
        val subdivided = MeshModifiers.subdivideMesh(base, smooth = false)
        val twisted = MeshModifiers.taperMesh(MeshModifiers.twistMesh(subdivided, 65f, Axis.Y), 0.68f)
        return listOf(twisted)
    }

    fun buildRocketShipPreset(): List<SceneNode3D> {
        val (verts, faces) = buildLatheProfileGeometry(
            rings = listOf(
                Triple(-1.20f, 0.56f, 0.0f),
                Triple(-0.92f, 0.36f, 0.0f),
                Triple(-0.65f, 0.82f, 0.28f), // 4 aerodynamic fins
                Triple(-0.20f, 0.58f, 0.04f),
                Triple(0.35f, 0.56f, 0.02f),
                Triple(0.82f, 0.38f, 0.0f),
                Triple(1.18f, 0.14f, 0.0f),
                Triple(1.32f, 0.02f, 0.0f)
            ),
            radialSegs = 24,
            ribFreq = 4
        )
        return listOf(
            SceneNode3D(
                id = newId("rocket_ship"),
                name = "Aerospace Rocket",
                nameFa = "موشک فضایی یکپارچه",
                typeTag = "Aerospace",
                vertices = verts,
                faces = faces,
                material = curatedMaterials[5] // Carbon Fiber Pro
            )
        )
    }

    fun buildDiamondGemPreset(): List<SceneNode3D> {
        val diamondMat = PbrMaterial(
            name = "Brilliant Diamond",
            nameFa = "الماس تراش برلیان",
            baseColorHex = 0xFF67E8F9,
            metallic = 0.25f,
            roughness = 0.05f,
            emissionHex = 0xFF00E5FF,
            emissionStrength = 0.18f,
            opacity = 0.90f
        )
        return listOf(
            createPrimitive(
                PrimitiveType3D.DIAMOND_GEM,
                scale = Vec3(1.25f, 1.25f, 1.25f),
                materialOverride = diamondMat,
                customNameFa = "کریستال الماس برلیان",
                customNameEn = "Brilliant Cut Diamond"
            )
        )
    }

    fun buildChessKingPreset(): List<SceneNode3D> {
        val (verts, faces) = buildLatheProfileGeometry(
            rings = listOf(
                Triple(-1.18f, 0.75f, 0.0f),
                Triple(-0.95f, 0.68f, 0.0f),
                Triple(-0.72f, 0.42f, 0.0f),
                Triple(-0.15f, 0.28f, 0.0f),
                Triple(0.35f, 0.46f, 0.06f),
                Triple(0.55f, 0.32f, 0.0f),
                Triple(0.88f, 0.48f, 0.08f),
                Triple(1.15f, 0.18f, 0.12f)
            ),
            radialSegs = 24,
            ribFreq = 8
        )
        return listOf(
            SceneNode3D(
                id = newId("chess_king"),
                name = "Grandmaster Chess King",
                nameFa = "مهره شاه شطرنج چوبی",
                typeTag = "Chess",
                vertices = verts,
                faces = faces,
                material = curatedMaterials[10] // Natural Walnut Wood
            )
        )
    }
}
