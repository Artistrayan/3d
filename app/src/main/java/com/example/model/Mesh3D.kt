package com.example.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

/**
 * 3D Vector with full linear algebra operations for 3D CAD, sculpting, and projection.
 */
data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float): Vec3 {
        val s = if (abs(scalar) < 1e-7f) 1f else scalar
        return Vec3(x / s, y / s, z / s)
    }

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length()
        return if (len < 1e-6f) Vec3(0f, 1f, 0f) else Vec3(x / len, y / len, z / len)
    }

    fun distanceTo(other: Vec3): Float = (this - other).length()

    fun rotateX(radians: Float): Vec3 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec3(x, y * c - z * s, y * s + z * c)
    }

    fun rotateY(radians: Float): Vec3 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec3(x * c + z * s, y, -x * s + z * c)
    }

    fun rotateZ(radians: Float): Vec3 {
        val c = cos(radians)
        val s = sin(radians)
        return Vec3(x * c - y * s, x * s + y * c, z)
    }

    fun toJson(): JSONArray = JSONArray().apply {
        put(x.toDouble())
        put(y.toDouble())
        put(z.toDouble())
    }

    companion object {
        fun fromJson(arr: JSONArray): Vec3 = Vec3(
            arr.optDouble(0, 0.0).toFloat(),
            arr.optDouble(1, 0.0).toFloat(),
            arr.optDouble(2, 0.0).toFloat()
        )
    }
}

/**
 * Indexed triangle face referencing 3 vertices in a mesh's vertex list.
 */
data class TriangleFace(
    val v0: Int,
    val v1: Int,
    val v2: Int
) {
    fun toJson(): JSONArray = JSONArray().apply {
        put(v0)
        put(v1)
        put(v2)
    }

    companion object {
        fun fromJson(arr: JSONArray): TriangleFace = TriangleFace(
            arr.optInt(0, 0),
            arr.optInt(1, 0),
            arr.optInt(2, 0)
        )
    }
}

/**
 * Physically Based Rendering (PBR) Material properties compatible with glTF 2.0 / GLB.
 */
data class PbrMaterial(
    val name: String = "Standard PBR",
    val nameFa: String = "فلز استاندارد",
    val baseColorHex: Long = 0xFF00E5FF,
    val metallic: Float = 0.35f,
    val roughness: Float = 0.30f,
    val emissionHex: Long = 0xFF000000,
    val emissionStrength: Float = 0.0f,
    val opacity: Float = 1.0f,
    val flatShading: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("nameFa", nameFa)
        put("baseColorHex", baseColorHex)
        put("metallic", metallic.toDouble())
        put("roughness", roughness.toDouble())
        put("emissionHex", emissionHex)
        put("emissionStrength", emissionStrength.toDouble())
        put("opacity", opacity.toDouble())
        put("flatShading", flatShading)
    }

    companion object {
        fun fromJson(obj: JSONObject): PbrMaterial = PbrMaterial(
            name = obj.optString("name", "Standard PBR"),
            nameFa = obj.optString("nameFa", "متریال استاندارد"),
            baseColorHex = obj.optLong("baseColorHex", 0xFF00E5FF),
            metallic = obj.optDouble("metallic", 0.35).toFloat(),
            roughness = obj.optDouble("roughness", 0.30).toFloat(),
            emissionHex = obj.optLong("emissionHex", 0xFF000000),
            emissionStrength = obj.optDouble("emissionStrength", 0.0).toFloat(),
            opacity = obj.optDouble("opacity", 1.0).toFloat(),
            flatShading = obj.optBoolean("flatShading", false)
        )
    }
}

/**
 * A single 3D Mesh Node in the scene graph with local geometry and transform (Position, Euler Rotation in degrees, Scale).
 */
data class SceneNode3D(
    val id: String,
    val name: String,
    val nameFa: String,
    val typeTag: String = "Mesh",
    val vertices: List<Vec3>,
    val faces: List<TriangleFace>,
    val position: Vec3 = Vec3(0f, 0f, 0f),
    val rotation: Vec3 = Vec3(0f, 0f, 0f), // Degrees X, Y, Z
    val scale: Vec3 = Vec3(1f, 1f, 1f),
    val material: PbrMaterial = PbrMaterial(),
    val visible: Boolean = true,
    val locked: Boolean = false
) {
    /**
     * Transforms local vertices into 3D world space using Scale -> Rotation (X, Y, Z) -> Position.
     */
    fun worldVertices(): List<Vec3> {
        val rx = Math.toRadians(rotation.x.toDouble()).toFloat()
        val ry = Math.toRadians(rotation.y.toDouble()).toFloat()
        val rz = Math.toRadians(rotation.z.toDouble()).toFloat()
        return vertices.map { v ->
            val scaled = Vec3(v.x * scale.x, v.y * scale.y, v.z * scale.z)
            val rotated = scaled.rotateX(rx).rotateY(ry).rotateZ(rz)
            rotated + position
        }
    }

    /**
     * Computes averaged per-vertex normals in world space for smooth shading & GLB export.
     */
    fun computeWorldVertexNormals(worldVerts: List<Vec3> = worldVertices()): List<Vec3> {
        val accum = Array(worldVerts.size) { Vec3(0f, 0f, 0f) }
        for (face in faces) {
            if (face.v0 in worldVerts.indices && face.v1 in worldVerts.indices && face.v2 in worldVerts.indices) {
                val p0 = worldVerts[face.v0]
                val p1 = worldVerts[face.v1]
                val p2 = worldVerts[face.v2]
                val n = (p1 - p0).cross(p2 - p0).normalized()
                accum[face.v0] = accum[face.v0] + n
                accum[face.v1] = accum[face.v1] + n
                accum[face.v2] = accum[face.v2] + n
            }
        }
        return accum.map { it.normalized() }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("nameFa", nameFa)
        put("typeTag", typeTag)
        val vArr = JSONArray()
        vertices.forEach { vArr.put(it.toJson()) }
        put("vertices", vArr)
        val fArr = JSONArray()
        faces.forEach { fArr.put(it.toJson()) }
        put("faces", fArr)
        put("position", position.toJson())
        put("rotation", rotation.toJson())
        put("scale", scale.toJson())
        put("material", material.toJson())
        put("visible", visible)
        put("locked", locked)
    }

    companion object {
        fun fromJson(obj: JSONObject): SceneNode3D {
            val vArr = obj.optJSONArray("vertices") ?: JSONArray()
            val verts = ArrayList<Vec3>(vArr.length())
            for (i in 0 until vArr.length()) {
                verts.add(Vec3.fromJson(vArr.getJSONArray(i)))
            }
            val fArr = obj.optJSONArray("faces") ?: JSONArray()
            val faceList = ArrayList<TriangleFace>(fArr.length())
            for (i in 0 until fArr.length()) {
                faceList.add(TriangleFace.fromJson(fArr.getJSONArray(i)))
            }
            return SceneNode3D(
                id = obj.optString("id", "node_${System.currentTimeMillis()}"),
                name = obj.optString("name", "Mesh"),
                nameFa = obj.optString("nameFa", "مدل سه‌بعدی"),
                typeTag = obj.optString("typeTag", "Mesh"),
                vertices = verts,
                faces = faceList,
                position = obj.optJSONArray("position")?.let { Vec3.fromJson(it) } ?: Vec3(),
                rotation = obj.optJSONArray("rotation")?.let { Vec3.fromJson(it) } ?: Vec3(),
                scale = obj.optJSONArray("scale")?.let { Vec3.fromJson(it) } ?: Vec3(1f, 1f, 1f),
                material = obj.optJSONObject("material")?.let { PbrMaterial.fromJson(it) } ?: PbrMaterial(),
                visible = obj.optBoolean("visible", true),
                locked = obj.optBoolean("locked", false)
            )
        }
    }
}

/**
 * Engineering & 3D Printing diagnostics calculated in real-time from scene geometry.
 * Scale convention: 1.0 studio unit = 10.0 mm (1.0 cm) for 3D printing slicers.
 */
data class MeshEngineeringStats(
    val objectCount: Int,
    val totalVertices: Int,
    val totalTriangles: Int,
    val widthMm: Float,
    val heightMm: Float,
    val depthMm: Float,
    val surfaceAreaCm2: Float,
    val volumeCm3: Float,
    val estimatedPlaGrams: Float,
    val estimatedResinGrams: Float,
    val estimatedPrintMinutes: Int,
    val isManifoldWatertight: Boolean,
    val minBounds: Vec3,
    val maxBounds: Vec3
)

enum class TransformToolMode {
    SELECT_ORBIT,
    MOVE,
    ROTATE,
    SCALE
}

enum class AxisConstraint {
    ALL,
    X,
    Y,
    Z
}

enum class ViewportShadingMode(val labelFa: String, val labelEn: String) {
    PBR_LIT("رندر واقعی PBR", "PBR Studio"),
    SHADED_WIRE("سایه‌زنی + مش", "Wire + Shaded"),
    WIREFRAME("شبکه سیمی (Wire)", "Wireframe"),
    CLAY_SCULPT("خشت مجسمه‌سازی", "Clay Sculpt"),
    XRAY("پرتو ایکس (X-Ray)", "X-Ray")
}

enum class StudioLightingPreset(
    val labelFa: String,
    val labelEn: String,
    val keyLightDir: Vec3,
    val keyColorHex: Long,
    val ambientSkyHex: Long,
    val ambientGroundHex: Long,
    val rimColorHex: Long
) {
    CYBER_STUDIO(
        "استودیو سایبری نئون",
        "Cyber Neon",
        Vec3(0.6f, 0.85f, 0.5f).normalized(),
        0xFFFFFFFF,
        0xFF1E3A8A,
        0xFF090D16,
        0xFF00E5FF
    ),
    INDUSTRIAL_CAD(
        "مهندسی دقیق CAD",
        "Industrial CAD",
        Vec3(-0.5f, 0.9f, 0.6f).normalized(),
        0xFFF8FAFC,
        0xFF334155,
        0xFF0F172A,
        0xFF94A3B8
    ),
    WARM_CLAY(
        "نور گرم مجسمه‌سازی",
        "Warm Sculpt",
        Vec3(0.7f, 0.7f, 0.4f).normalized(),
        0xFFFFF7ED,
        0xFF7C2D12,
        0xFF1C1917,
        0xFFFF9100
    ),
    GOLD_SHOWROOM(
        "نمایشگاه طلایی لوکس",
        "Gold Showroom",
        Vec3(0.3f, 0.95f, 0.4f).normalized(),
        0xFFFEF08A,
        0xFF422006,
        0xFF090D16,
        0xFFF59E0B
    ),
    ARCTIC_LAB(
        "آزمایشگاه قطبی روشن",
        "Arctic Lab",
        Vec3(-0.6f, 0.8f, 0.5f).normalized(),
        0xFFE0F2FE,
        0xFF0284C7,
        0xFF0F172A,
        0xFF38BDF8
    )
}

enum class ExportFormat3D(
    val ext: String,
    val mimeType: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val badge: String
) {
    GLB(
        ext = "glb",
        mimeType = "model/gltf-binary",
        titleFa = "فرمت باینری استاندارد GLB (glTF 2.0)",
        titleEn = "GLB Binary (glTF 2.0)",
        subtitleFa = "حفظ کامل متریال PBR، رنگ، متالیک، نرمال‌ها و ساختار صحنه (مناسب وب، بازی، AR و Blender)",
        badge = "PBR + Web/Game"
    ),
    STL_BINARY(
        ext = "stl",
        mimeType = "model/stl",
        titleFa = "فرمت پرینت سه‌بعدی باینری (Binary STL)",
        titleEn = "Binary STL (3D Print)",
        subtitleFa = "فرمت استاندارد صنعتی با ابعاد دقیق میلی‌متری برای اسلایسرهای Cura، PrusaSlicer و CNC",
        badge = "3D Print Ready"
    ),
    STL_ASCII(
        ext = "stl",
        mimeType = "model/stl",
        titleFa = "فرمت پرینت سه‌بعدی متنی (ASCII STL)",
        titleEn = "ASCII STL (CAD Text)",
        subtitleFa = "ساختار متنی قابل خواندن مختصات مثلث‌ها و بردارهای نرمال برای نرم‌افزارهای مهندسی",
        badge = "CAD ASCII"
    ),
    OBJ(
        ext = "obj",
        mimeType = "model/obj",
        titleFa = "فرمت جهانی Wavefront OBJ",
        titleEn = "Wavefront OBJ",
        subtitleFa = "سازگار با تمامی نرم‌افزارهای گرافیکی (3ds Max، Maya، ZBrush، Blender) شامل گروه‌ها و نرمال‌ها",
        badge = "Universal 3D"
    ),
    PLY(
        ext = "ply",
        mimeType = "application/octet-stream",
        titleFa = "فرمت علمی و رنگی Stanford PLY",
        titleEn = "Stanford PLY (Vertex RGB)",
        subtitleFa = "ذخیره دقیق رئوس، بردارهای نرمال و رنگ RGB هر رأس برای اسکن سه‌بعدی و پردازش مش",
        badge = "Vertex Color"
    )
}
