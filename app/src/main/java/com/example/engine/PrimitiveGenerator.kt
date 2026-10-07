package com.example.engine

import com.example.model.PbrMaterial
import com.example.model.SceneNode3D
import com.example.model.TriangleFace
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
        PbrMaterial("Brushed Titanium", "تیتانیوم برس‌خورده", 0xFF94A3B8, 0.88f, 0.25f),
        PbrMaterial("24K Royal Gold", "طلای ۲۴ عیار براق", 0xFFF59E0B, 0.95f, 0.12f),
        PbrMaterial("Sculpt Terracotta", "خشت مجسمه‌سازی (Clay)", 0xFFD97757, 0.05f, 0.65f),
        PbrMaterial("Matte PLA White", "فیلامنت PLA سفید مات", 0xFFF1F5F9, 0.05f, 0.55f),
        PbrMaterial("Carbon Stealth", "فیبر کربن مات", 0xFF1E293B, 0.45f, 0.35f),
        PbrMaterial("Crimson Anodized", "آلومینیوم آنودایز قرمز", 0xFFEF4444, 0.80f, 0.22f),
        PbrMaterial("Emerald Crystal", "کریستال زمرد نیمه‌شفاف", 0xFF10B981, 0.20f, 0.08f, 0xFF059669, 0.10f, 0.85f),
        PbrMaterial("Neon Plasma Core", "پلاسمای نئون درخشان", 0xFF38BDF8, 0.10f, 0.10f, 0xFF00E5FF, 0.85f),
        PbrMaterial("Amber Industrial", "کهربایی صنعتی", 0xFFFF9100, 0.65f, 0.28f),
        PbrMaterial("Royal Amethyst", "آمتیست بنفش", 0xFFA855F7, 0.40f, 0.15f),
        PbrMaterial("Copper Forge", "مس چکش‌کاری شده", 0xFFEA580C, 0.90f, 0.24f),
        PbrMaterial("Cobalt Chrome", "کروم کبالت آیینه‌ای", 0xFF3B82F6, 0.92f, 0.10f),
        PbrMaterial("SLA Grey Resin", "رزین خاکستری پرینت 3D", 0xFF64748B, 0.10f, 0.45f),
        PbrMaterial("Cyber Magenta Glow", "مگنتای نئون سایبرپانک", 0xFFEC4899, 0.50f, 0.20f, 0xFFEC4899, 0.55f),
        PbrMaterial("Glass Canopy", "شیشه کابین شفاف", 0xFF7DD3FC, 0.10f, 0.05f, 0xFF000000, 0f, 0.60f)
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
            PrimitiveType3D.SPHERE -> buildSphereGeometry(radius = 1.0f, rings = 14, sectors = 20)
            PrimitiveType3D.ICOSPHERE -> buildIcosphereGeometry(radius = 1.05f, subdivisions = 1)
            PrimitiveType3D.CYLINDER -> buildCylinderGeometry(topRadius = 0.85f, bottomRadius = 0.85f, height = 1.8f, sectors = 22)
            PrimitiveType3D.CONE -> buildCylinderGeometry(topRadius = 0.0f, bottomRadius = 1.0f, height = 1.9f, sectors = 22)
            PrimitiveType3D.TORUS -> buildTorusGeometry(majorRadius = 0.95f, minorRadius = 0.34f, majorSegs = 22, minorSegs = 12)
            PrimitiveType3D.PYRAMID -> buildCylinderGeometry(topRadius = 0.0f, bottomRadius = 1.15f, height = 1.8f, sectors = 4)
            PrimitiveType3D.HEX_PRISM -> buildCylinderGeometry(topRadius = 0.95f, bottomRadius = 0.95f, height = 1.4f, sectors = 6)
            PrimitiveType3D.CAPSULE -> buildCapsuleGeometry(radius = 0.65f, cylinderHeight = 1.1f, sectors = 18, rings = 8)
            PrimitiveType3D.GEAR -> buildGearGeometry(outerRadius = 1.15f, innerRadius = 0.88f, holeRadius = 0.38f, thickness = 0.48f, teeth = 12)
            PrimitiveType3D.TWISTED_VASE -> buildTwistedVaseGeometry(height = 2.2f, layers = 16, radialSegs = 20)
            PrimitiveType3D.DIAMOND_GEM -> buildDiamondGemGeometry(radius = 1.05f, height = 1.4f, facets = 12)
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

    /**
     * Subdivided Box geometry (closed manifold mesh).
     */
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

        // Front (+Z)
        addGridFace(Vec3(-hx, -hy, hz), Vec3(w, 0f, 0f), Vec3(0f, h, 0f))
        // Back (-Z)
        addGridFace(Vec3(hx, -hy, -hz), Vec3(-w, 0f, 0f), Vec3(0f, h, 0f))
        // Right (+X)
        addGridFace(Vec3(hx, -hy, hz), Vec3(0f, 0f, -d), Vec3(0f, h, 0f))
        // Left (-X)
        addGridFace(Vec3(-hx, -hy, -hz), Vec3(0f, 0f, d), Vec3(0f, h, 0f))
        // Top (+Y)
        addGridFace(Vec3(-hx, hy, hz), Vec3(w, 0f, 0f), Vec3(0f, 0f, -d))
        // Bottom (-Y)
        addGridFace(Vec3(-hx, -hy, -hz), Vec3(w, 0f, 0f), Vec3(0f, 0f, d))

        return MeshModifiers.weldCloseVertices(vertices, faces, 1e-4f)
    }

    /**
     * Closed UV Sphere geometry.
     */
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
                if (r != 0) {
                    faces.add(TriangleFace(i0, i1, i2))
                }
                if (r != rCount - 1) {
                    faces.add(TriangleFace(i1, i3, i2))
                }
            }
        }
        return MeshModifiers.weldCloseVertices(vertices, faces, 1e-4f)
    }

    /**
     * Geodesic Icosphere built from golden-ratio icosahedron + midpoint subdivision.
     */
    fun buildIcosphereGeometry(radius: Float, subdivisions: Int = 1): Pair<List<Vec3>, List<TriangleFace>> {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
        var verts = listOf(
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

    /**
     * Closed Cylinder / Cone / Prism geometry.
     */
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

        // Bottom ring: 0 until sCount
        for (i in 0 until sCount) {
            val a = 2.0 * PI * i / sCount
            vertices.add(Vec3((cos(a) * bottomRadius).toFloat(), -halfH, (sin(a) * bottomRadius).toFloat()))
        }
        // Top ring: sCount until 2*sCount
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

            // Side quads
            faces.add(TriangleFace(b0, t0, b1))
            faces.add(TriangleFace(b1, t0, t1))

            // Bottom cap
            if (bottomRadius > 1e-4f) {
                faces.add(TriangleFace(bottomCenterIdx, b0, b1))
            }
            // Top cap
            if (topRadius > 1e-4f) {
                faces.add(TriangleFace(topCenterIdx, t1, t0))
            }
        }
        return MeshModifiers.weldCloseVertices(vertices, faces, 1e-4f)
    }

    /**
     * Closed Torus ring geometry.
     */
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

    /**
     * Closed Capsule geometry.
     */
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

    /**
     * Precision Mechanical Spur Gear with center bore and involute-style teeth.
     */
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

            // 4 vertices per radial slice:
            // 0: top inner, 1: top outer, 2: bottom outer, 3: bottom inner
            vertices.add(Vec3(cosA * holeRadius, halfT, sinA * holeRadius))
            vertices.add(Vec3(cosA * rOuter, halfT, sinA * rOuter))
            vertices.add(Vec3(cosA * rOuter, -halfT, sinA * rOuter))
            vertices.add(Vec3(cosA * holeRadius, -halfT, sinA * holeRadius))
        }

        for (i in 0 until steps) {
            val next = (i + 1) % steps
            val b0 = i * 4
            val b1 = next * 4

            // Top face
            faces.add(TriangleFace(b0 + 0, b1 + 1, b0 + 1))
            faces.add(TriangleFace(b0 + 0, b1 + 0, b1 + 1))
            // Outer tooth rim
            faces.add(TriangleFace(b0 + 1, b1 + 2, b0 + 2))
            faces.add(TriangleFace(b0 + 1, b1 + 1, b1 + 2))
            // Bottom face
            faces.add(TriangleFace(b0 + 2, b1 + 3, b0 + 3))
            faces.add(TriangleFace(b0 + 2, b1 + 2, b1 + 3))
            // Inner bore wall
            faces.add(TriangleFace(b0 + 3, b1 + 0, b0 + 0))
            faces.add(TriangleFace(b0 + 3, b1 + 3, b1 + 0))
        }
        return vertices to faces
    }

    /**
     * Sculptural Twisted Ribbed Vase designed for 3D printing.
     */
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

    /**
     * Multi-faceted Brilliant Cut Diamond Gem.
     */
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

        // 0..fCount-1: Girdle ring
        for (i in 0 until fCount) {
            val a = 2.0 * PI * i / fCount
            vertices.add(Vec3((cos(a) * radius).toFloat(), girdleY, (sin(a) * radius).toFloat()))
        }
        // fCount..2*fCount-1: Crown table ring (half-step rotated for diamond facets)
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

            // Crown facets
            faces.add(TriangleFace(g0, t0, g1))
            faces.add(TriangleFace(g1, t0, t1))
            // Table top
            faces.add(TriangleFace(tableCenterIdx, t1, t0))
            // Pavilion bottom cone facets
            faces.add(TriangleFace(culetIdx, g0, g1))
        }
        return vertices to faces
    }

    /**
     * Returns the 8 complete, multi-part Ready-Made 3D Models (مدل‌های آماده حرفه‌ای).
     */
    fun getReadyModelPresets(): List<ReadyModelPreset> = listOf(
        ReadyModelPreset(
            id = "cyber_mecha_bot",
            titleFa = "روبات نگهبان سایبری (Cyber Mecha)",
            titleEn = "Cyber Guardian Mecha",
            subtitleFa = "مدل چندبخشی شامل کلاهخود، ویزور نئونی، زره سینه، راکتور انرژی، شانه‌ها و بازوهای مکانیکی",
            badgeFa = "۹ قطعه مجزا • PBR",
            accentHex = 0xFF00E5FF,
            partsCount = 9,
            generator = ::buildCyberMechaPreset
        ),
        ReadyModelPreset(
            id = "concept_supercar",
            titleFa = "خودرو مفهومی اسپرت (Concept Supercar)",
            titleEn = "Apex Cyber Concept Car",
            subtitleFa = "شاسی آیرودینامیک، کابین شیشه‌ای، اسپویلر عقب، دیفیوزر و ۴ چرخ اسپرت",
            badgeFa = "۸ قطعه مجزا • خودرو",
            accentHex = 0xFFEF4444,
            partsCount = 8,
            generator = ::buildConceptCarPreset
        ),
        ReadyModelPreset(
            id = "planetary_gearbox",
            titleFa = "مکانیزم چرخ‌دنده سیاره‌ای (مناسب پرینت 3D)",
            titleEn = "Planetary Gearset (Print Ready)",
            subtitleFa = "مجموعه مهندسی دقیق شامل چرخ‌دنده خورشیدی، ۳ دنده سیاره‌ای و محور مرکزی آماده خروجی STL",
            badgeFa = "۵ قطعه • Print Ready",
            accentHex = 0xFFF59E0B,
            partsCount = 5,
            generator = ::buildPlanetaryGearPreset
        ),
        ReadyModelPreset(
            id = "starship_explorer",
            titleFa = "فضاپیمای اکتشافی (Starlight Cruiser)",
            titleEn = "Deep Space Cruiser",
            subtitleFa = "بدنه اصلی فضاپیما، بال‌های دلتا، ۲ موتور وارپ نئونی و برج فرماندهی",
            badgeFa = "۶ قطعه • Sci-Fi",
            accentHex = 0xFF38BDF8,
            partsCount = 6,
            generator = ::buildStarshipPreset
        ),
        ReadyModelPreset(
            id = "parametric_tower",
            titleFa = "برج معماری پارامتریک مدرن",
            titleEn = "Parametric Twisted Tower",
            subtitleFa = "سازه‌ معماری پیشرفته با پایه پودیوم، برج پیچشی شیشه‌ای و تاج مناره",
            badgeFa = "۴ قطعه • معماری CAD",
            accentHex = 0xFF10B981,
            partsCount = 4,
            generator = ::buildParametricTowerPreset
        ),
        ReadyModelPreset(
            id = "royal_diamond_ring",
            titleFa = "انگشتر طلا و تک‌نگین الماس",
            titleEn = "Royal Diamond Solitaire Ring",
            subtitleFa = "حلقه طلای ۲۴ عیار به همراه پایه جواهر و نگین الماس تراش برلیان",
            badgeFa = "۶ قطعه • جواهرسازی",
            accentHex = 0xFFFBBF24,
            partsCount = 6,
            generator = ::buildDiamondRingPreset
        ),
        ReadyModelPreset(
            id = "armored_knight",
            titleFa = "کاراکتر شوالیه زره‌پوش (Game Asset)",
            titleEn = "Stylized Armored Knight",
            subtitleFa = "کاراکتر آماده بازی شامل کلاهخود، زره، سپر دفاعی و شمشیر فولادی",
            badgeFa = "۷ قطعه • Game Ready",
            accentHex = 0xFFA855F7,
            partsCount = 7,
            generator = ::buildKnightPreset
        ),
        ReadyModelPreset(
            id = "sculpt_twisted_vase",
            titleFa = "گلدان هنری مارپیچ (ویژه پرینت سه‌بعدی)",
            titleEn = "Sculptural Ribbed Vase",
            subtitleFa = "مش یکپارچه و آب‌بند (Manifold) طراحی‌شده اختصاصی برای اسلایسرهای پرینتر سه‌بعدی",
            badgeFa = "تک‌قطعه • STL Manifold",
            accentHex = 0xFFF472B6,
            partsCount = 2,
            generator = ::buildDesignerVasePreset
        )
    )

    fun buildCyberMechaPreset(): List<SceneNode3D> {
        val armorMat = curatedMaterials[1] // Brushed Titanium
        val cyanGlow = curatedMaterials[8] // Neon Plasma
        val amberMat = curatedMaterials[9] // Amber Industrial
        val darkCarbon = curatedMaterials[5]

        return listOf(
            createPrimitive(
                PrimitiveType3D.HEX_PRISM,
                position = Vec3(0f, 0.35f, 0f),
                scale = Vec3(1.1f, 0.95f, 0.72f),
                materialOverride = armorMat,
                customNameFa = "زره سینه (Torso)",
                customNameEn = "Mecha Torso"
            ),
            createPrimitive(
                PrimitiveType3D.SPHERE,
                position = Vec3(0f, 0.45f, 0.58f),
                scale = Vec3(0.32f, 0.32f, 0.18f),
                materialOverride = cyanGlow,
                customNameFa = "راکتور انرژی سینه",
                customNameEn = "Arc Core Reactor"
            ),
            createPrimitive(
                PrimitiveType3D.ICOSPHERE,
                position = Vec3(0f, 1.55f, 0f),
                scale = Vec3(0.55f, 0.52f, 0.58f),
                materialOverride = armorMat,
                customNameFa = "کلاهخود روبات (Head)",
                customNameEn = "Mecha Head"
            ),
            createPrimitive(
                PrimitiveType3D.CUBE,
                position = Vec3(0f, 1.58f, 0.44f),
                scale = Vec3(0.52f, 0.14f, 0.22f),
                materialOverride = cyanGlow,
                customNameFa = "ویزور نئون چشم",
                customNameEn = "Holo Visor"
            ),
            createPrimitive(
                PrimitiveType3D.SPHERE,
                position = Vec3(-1.25f, 0.88f, 0f),
                scale = Vec3(0.45f, 0.42f, 0.48f),
                materialOverride = amberMat,
                customNameFa = "زره شانه چپ",
                customNameEn = "Left Pauldron"
            ),
            createPrimitive(
                PrimitiveType3D.SPHERE,
                position = Vec3(1.25f, 0.88f, 0f),
                scale = Vec3(0.45f, 0.42f, 0.48f),
                materialOverride = amberMat,
                customNameFa = "زره شانه راست",
                customNameEn = "Right Pauldron"
            ),
            createPrimitive(
                PrimitiveType3D.CAPSULE,
                position = Vec3(-1.35f, 0.05f, 0.15f),
                rotation = Vec3(-18f, 0f, 8f),
                scale = Vec3(0.36f, 0.58f, 0.36f),
                materialOverride = darkCarbon,
                customNameFa = "بازوی مکانیکی چپ",
                customNameEn = "Left Mecha Arm"
            ),
            createPrimitive(
                PrimitiveType3D.CAPSULE,
                position = Vec3(1.35f, 0.05f, 0.15f),
                rotation = Vec3(-18f, 0f, -8f),
                scale = Vec3(0.36f, 0.58f, 0.36f),
                materialOverride = darkCarbon,
                customNameFa = "بازوی مکانیکی راست",
                customNameEn = "Right Mecha Arm"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, -0.82f, 0f),
                scale = Vec3(0.75f, 0.62f, 0.62f),
                materialOverride = darkCarbon,
                customNameFa = "پایه و پیشران حرکتی",
                customNameEn = "Base Thruster"
            )
        )
    }

    fun buildConceptCarPreset(): List<SceneNode3D> {
        val redBody = curatedMaterials[6] // Crimson Anodized
        val canopyGlass = curatedMaterials[15]
        val carbonMat = curatedMaterials[5]
        val neonLight = curatedMaterials[8]

        return listOf(
            createPrimitive(
                PrimitiveType3D.CUBE,
                position = Vec3(0f, -0.25f, 0f),
                scale = Vec3(1.15f, 0.34f, 2.25f),
                materialOverride = redBody,
                customNameFa = "شاسی اصلی خودرو",
                customNameEn = "Main Chassis"
            ),
            createPrimitive(
                PrimitiveType3D.SPHERE,
                position = Vec3(0f, 0.12f, -0.15f),
                scale = Vec3(0.76f, 0.36f, 1.15f),
                materialOverride = canopyGlass,
                customNameFa = "کابین شیشه‌ای (Canopy)",
                customNameEn = "Glass Canopy"
            ),
            createPrimitive(
                PrimitiveType3D.CUBE,
                position = Vec3(0f, 0.25f, -1.65f),
                scale = Vec3(1.22f, 0.06f, 0.28f),
                materialOverride = carbonMat,
                customNameFa = "اسپویلر عقب (GT Wing)",
                customNameEn = "Rear GT Spoiler"
            ),
            createPrimitive(
                PrimitiveType3D.CUBE,
                position = Vec3(0f, -0.22f, 1.82f),
                scale = Vec3(1.02f, 0.08f, 0.12f),
                materialOverride = neonLight,
                customNameFa = "چراغ نئون جلو",
                customNameEn = "LED Headlight Bar"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(-0.98f, -0.38f, 1.12f),
                rotation = Vec3(0f, 0f, 90f),
                scale = Vec3(0.45f, 0.18f, 0.45f),
                materialOverride = carbonMat,
                customNameFa = "چرخ جلو چپ",
                customNameEn = "Wheel Front-L"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0.98f, -0.38f, 1.12f),
                rotation = Vec3(0f, 0f, 90f),
                scale = Vec3(0.45f, 0.18f, 0.45f),
                materialOverride = carbonMat,
                customNameFa = "چرخ جلو راست",
                customNameEn = "Wheel Front-R"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(-0.98f, -0.35f, -1.15f),
                rotation = Vec3(0f, 0f, 90f),
                scale = Vec3(0.50f, 0.20f, 0.50f),
                materialOverride = carbonMat,
                customNameFa = "چرخ عقب چپ",
                customNameEn = "Wheel Rear-L"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0.98f, -0.35f, -1.15f),
                rotation = Vec3(0f, 0f, 90f),
                scale = Vec3(0.50f, 0.20f, 0.50f),
                materialOverride = carbonMat,
                customNameFa = "چرخ عقب راست",
                customNameEn = "Wheel Rear-R"
            )
        )
    }

    fun buildPlanetaryGearPreset(): List<SceneNode3D> {
        val goldMat = curatedMaterials[2]
        val titaniumMat = curatedMaterials[1]
        val copperMat = curatedMaterials[11]

        return listOf(
            createPrimitive(
                PrimitiveType3D.GEAR,
                position = Vec3(0f, 0f, 0f),
                scale = Vec3(0.85f, 1.0f, 0.85f),
                materialOverride = goldMat,
                customNameFa = "چرخ‌دنده خورشیدی مرکزی",
                customNameEn = "Center Sun Gear"
            ),
            createPrimitive(
                PrimitiveType3D.GEAR,
                position = Vec3(1.62f, 0f, 0f),
                rotation = Vec3(0f, 15f, 0f),
                scale = Vec3(0.62f, 1.0f, 0.62f),
                materialOverride = titaniumMat,
                customNameFa = "چرخ‌دنده سیاره‌ای ۱",
                customNameEn = "Planet Gear 1"
            ),
            createPrimitive(
                PrimitiveType3D.GEAR,
                position = Vec3(-0.81f, 0f, 1.40f),
                rotation = Vec3(0f, 15f, 0f),
                scale = Vec3(0.62f, 1.0f, 0.62f),
                materialOverride = titaniumMat,
                customNameFa = "چرخ‌دنده سیاره‌ای ۲",
                customNameEn = "Planet Gear 2"
            ),
            createPrimitive(
                PrimitiveType3D.GEAR,
                position = Vec3(-0.81f, 0f, -1.40f),
                rotation = Vec3(0f, 15f, 0f),
                scale = Vec3(0.62f, 1.0f, 0.62f),
                materialOverride = titaniumMat,
                customNameFa = "چرخ‌دنده سیاره‌ای ۳",
                customNameEn = "Planet Gear 3"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, 0f, 0f),
                scale = Vec3(0.32f, 0.9f, 0.32f),
                materialOverride = copperMat,
                customNameFa = "شفت انتقال قدرت مرکزی",
                customNameEn = "Drive Shaft"
            )
        )
    }

    fun buildStarshipPreset(): List<SceneNode3D> {
        val hullMat = curatedMaterials[1]
        val cyanCore = curatedMaterials[8]
        val cobaltMat = curatedMaterials[12]

        return listOf(
            createPrimitive(
                PrimitiveType3D.CONE,
                position = Vec3(0f, 0f, 0.2f),
                rotation = Vec3(90f, 0f, 0f),
                scale = Vec3(0.68f, 1.65f, 0.38f),
                materialOverride = hullMat,
                customNameFa = "بدنه اصلی فضاپیما",
                customNameEn = "Main Hull Fuselage"
            ),
            createPrimitive(
                PrimitiveType3D.SPHERE,
                position = Vec3(0f, 0.22f, 0.35f),
                scale = Vec3(0.34f, 0.22f, 0.65f),
                materialOverride = cyanCore,
                customNameFa = "گنبد فرماندهی",
                customNameEn = "Command Bridge"
            ),
            createPrimitive(
                PrimitiveType3D.CUBE,
                position = Vec3(0f, -0.04f, -0.25f),
                scale = Vec3(1.85f, 0.06f, 0.72f),
                materialOverride = cobaltMat,
                customNameFa = "بال‌های دلتا",
                customNameEn = "Delta Wings"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(-1.18f, 0.08f, -0.45f),
                rotation = Vec3(90f, 0f, 0f),
                scale = Vec3(0.22f, 0.78f, 0.22f),
                materialOverride = hullMat,
                customNameFa = "موتور وارپ چپ",
                customNameEn = "Left Warp Nacelle"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(1.18f, 0.08f, -0.45f),
                rotation = Vec3(90f, 0f, 0f),
                scale = Vec3(0.22f, 0.78f, 0.22f),
                materialOverride = hullMat,
                customNameFa = "موتور وارپ راست",
                customNameEn = "Right Warp Nacelle"
            ),
            createPrimitive(
                PrimitiveType3D.TORUS,
                position = Vec3(0f, 0f, -1.25f),
                rotation = Vec3(90f, 0f, 0f),
                scale = Vec3(0.45f, 0.45f, 0.45f),
                materialOverride = cyanCore,
                customNameFa = "حلقه پیشران یونی",
                customNameEn = "Ion Thruster Ring"
            )
        )
    }

    fun buildParametricTowerPreset(): List<SceneNode3D> {
        val emeraldGlass = curatedMaterials[7]
        val titanium = curatedMaterials[1]
        val cyanGlow = curatedMaterials[0]

        val baseTower = createPrimitive(
            PrimitiveType3D.CUBE,
            position = Vec3(0f, 0.25f, 0f),
            scale = Vec3(0.72f, 1.65f, 0.72f),
            materialOverride = emeraldGlass,
            customNameFa = "بدنه برج پیچشی",
            customNameEn = "Twisted Glass Tower"
        )
        val twistedTower = MeshModifiers.twistMesh(MeshModifiers.subdivideMesh(baseTower, smooth = false), 65f, Axis.Y)

        return listOf(
            createPrimitive(
                PrimitiveType3D.HEX_PRISM,
                position = Vec3(0f, -1.15f, 0f),
                scale = Vec3(1.25f, 0.28f, 1.25f),
                materialOverride = titanium,
                customNameFa = "پودیوم پایه برج",
                customNameEn = "Podium Base"
            ),
            twistedTower,
            createPrimitive(
                PrimitiveType3D.PYRAMID,
                position = Vec3(0f, 1.85f, 0f),
                scale = Vec3(0.55f, 0.45f, 0.55f),
                materialOverride = cyanGlow,
                customNameFa = "تاج شیشه‌ای برج",
                customNameEn = "Crown Spire"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, 2.35f, 0f),
                scale = Vec3(0.06f, 0.45f, 0.06f),
                materialOverride = titanium,
                customNameFa = "آنتن مخابراتی نوک برج",
                customNameEn = "Spire Antenna"
            )
        )
    }

    fun buildDiamondRingPreset(): List<SceneNode3D> {
        val gold = curatedMaterials[2]
        val diamond = PbrMaterial("Brilliant Diamond", "الماس تراش برلیان", 0xFFE0F2FE, 0.15f, 0.04f, 0xFF38BDF8, 0.22f, 0.88f)

        return listOf(
            createPrimitive(
                PrimitiveType3D.TORUS,
                position = Vec3(0f, -0.15f, 0f),
                rotation = Vec3(90f, 0f, 0f),
                scale = Vec3(1.15f, 1.15f, 0.72f),
                materialOverride = gold,
                customNameFa = "حلقه طلای ۲۴ عیار",
                customNameEn = "24K Gold Band"
            ),
            createPrimitive(
                PrimitiveType3D.DIAMOND_GEM,
                position = Vec3(0f, 1.28f, 0f),
                scale = Vec3(0.62f, 0.55f, 0.62f),
                materialOverride = diamond,
                customNameFa = "نگین الماس برلیان",
                customNameEn = "Brilliant Diamond"
            ),
            createPrimitive(
                PrimitiveType3D.CONE,
                position = Vec3(0f, 0.95f, 0f),
                rotation = Vec3(180f, 0f, 0f),
                scale = Vec3(0.45f, 0.28f, 0.45f),
                materialOverride = gold,
                customNameFa = "پایه نگهدارنده نگین",
                customNameEn = "Crown Basket"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0.38f, 1.18f, 0.38f),
                scale = Vec3(0.07f, 0.25f, 0.07f),
                materialOverride = gold,
                customNameFa = "چنگک طلایی ۱",
                customNameEn = "Gold Prong 1"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(-0.38f, 1.18f, 0.38f),
                scale = Vec3(0.07f, 0.25f, 0.07f),
                materialOverride = gold,
                customNameFa = "چنگک طلایی ۲",
                customNameEn = "Gold Prong 2"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, 1.18f, -0.45f),
                scale = Vec3(0.07f, 0.25f, 0.07f),
                materialOverride = gold,
                customNameFa = "چنگک طلایی ۳",
                customNameEn = "Gold Prong 3"
            )
        )
    }

    fun buildKnightPreset(): List<SceneNode3D> {
        val steel = curatedMaterials[1]
        val gold = curatedMaterials[2]
        val royalPurple = curatedMaterials[10]
        val crimson = curatedMaterials[6]

        return listOf(
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, 0.15f, 0f),
                scale = Vec3(0.78f, 0.85f, 0.58f),
                materialOverride = steel,
                customNameFa = "زره سینه شوالیه",
                customNameEn = "Knight Breastplate"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, 1.25f, 0f),
                scale = Vec3(0.48f, 0.42f, 0.48f),
                materialOverride = steel,
                customNameFa = "کلاهخود فولادی",
                customNameEn = "Great Helm"
            ),
            createPrimitive(
                PrimitiveType3D.CONE,
                position = Vec3(0f, 1.85f, -0.08f),
                scale = Vec3(0.22f, 0.35f, 0.48f),
                materialOverride = crimson,
                customNameFa = "پر تاج کلاهخود",
                customNameEn = "Helmet Plume"
            ),
            createPrimitive(
                PrimitiveType3D.HEX_PRISM,
                position = Vec3(-0.95f, 0.15f, 0.45f),
                rotation = Vec3(90f, 0f, -15f),
                scale = Vec3(0.68f, 0.12f, 0.88f),
                materialOverride = royalPurple,
                customNameFa = "سپر سلطنتی",
                customNameEn = "Royal Shield"
            ),
            createPrimitive(
                PrimitiveType3D.CUBE,
                position = Vec3(1.05f, 0.55f, 0.35f),
                rotation = Vec3(15f, 0f, -10f),
                scale = Vec3(0.09f, 1.15f, 0.18f),
                materialOverride = steel,
                customNameFa = "تیغه شمشیر",
                customNameEn = "Broadsword Blade"
            ),
            createPrimitive(
                PrimitiveType3D.CUBE,
                position = Vec3(0.95f, -0.15f, 0.22f),
                rotation = Vec3(15f, 0f, -10f),
                scale = Vec3(0.38f, 0.08f, 0.12f),
                materialOverride = gold,
                customNameFa = "محافظ دسته شمشیر",
                customNameEn = "Sword Crossguard"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, -0.95f, 0f),
                scale = Vec3(1.1f, 0.18f, 1.1f),
                materialOverride = gold,
                customNameFa = "پایه استند کاراکتر",
                customNameEn = "Display Pedestal"
            )
        )
    }

    fun buildDesignerVasePreset(): List<SceneNode3D> {
        val terracotta = curatedMaterials[3]
        val goldBase = curatedMaterials[2]
        return listOf(
            createPrimitive(
                PrimitiveType3D.TWISTED_VASE,
                position = Vec3(0f, 0.2f, 0f),
                scale = Vec3(1.15f, 1.15f, 1.15f),
                materialOverride = terracotta,
                customNameFa = "بدنه گلدان پارامتریک",
                customNameEn = "Twisted Ribbed Vase"
            ),
            createPrimitive(
                PrimitiveType3D.CYLINDER,
                position = Vec3(0f, -1.05f, 0f),
                scale = Vec3(0.88f, 0.12f, 0.88f),
                materialOverride = goldBase,
                customNameFa = "کفی تراز پرینت سه‌بعدی",
                customNameEn = "Print Bed Base"
            )
        )
    }
}
