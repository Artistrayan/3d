package com.example.engine

import com.example.model.MeshEngineeringStats
import com.example.model.SceneNode3D
import com.example.model.TriangleFace
import com.example.model.Vec3
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class Axis { X, Y, Z }

/**
 * Real 3D Mesh Topology Modifiers, Sculpting Deformers, and CAD Engineering Diagnostics.
 */
object MeshModifiers {

    /**
     * Welds vertices closer than epsilon and removes degenerate triangles.
     */
    fun weldCloseVertices(
        vertices: List<Vec3>,
        faces: List<TriangleFace>,
        epsilon: Float = 1e-4f
    ): Pair<List<Vec3>, List<TriangleFace>> {
        val inv = 1f / epsilon
        val uniqueVerts = ArrayList<Vec3>()
        val cellMap = HashMap< String, Int>()
        val remap = IntArray(vertices.size)

        for (i in vertices.indices) {
            val v = vertices[i]
            val kx = (v.x * inv).roundToInt()
            val ky = (v.y * inv).roundToInt()
            val kz = (v.z * inv).roundToInt()
            val key = "${kx}_${ky}_${kz}"
            val existing = cellMap[key]
            if (existing != null) {
                remap[i] = existing
            } else {
                val newIdx = uniqueVerts.size
                uniqueVerts.add(v)
                cellMap[key] = newIdx
                remap[i] = newIdx
            }
        }

        val cleanFaces = ArrayList<TriangleFace>(faces.size)
        for (f in faces) {
            val a = remap[f.v0]
            val b = remap[f.v1]
            val c = remap[f.v2]
            if (a != b && b != c && c != a) {
                cleanFaces.add(TriangleFace(a, b, c))
            }
        }
        return uniqueVerts to cleanFaces
    }

    /**
     * Subdivides every triangle into 4 triangles using edge midpoints, with optional Laplacian smoothing.
     * Cap at 6,000 triangles per object to guarantee 60fps real-time rendering.
     */
    fun subdivideMesh(node: SceneNode3D, smooth: Boolean = true): SceneNode3D {
        if (node.faces.size >= 4500) {
            return if (smooth) smoothLaplacian(node, iterations = 1, lambda = 0.35f) else node
        }
        val verts = node.vertices.toMutableList()
        val midCache = HashMap<Long, Int>()

        fun getMidpoint(a: Int, b: Int): Int {
            val minIdx = min(a, b).toLong()
            val maxIdx = max(a, b).toLong()
            val key = (minIdx shl 32) or maxIdx
            return midCache.getOrPut(key) {
                val mid = (verts[a] + verts[b]) * 0.5f
                verts.add(mid)
                verts.lastIndex
            }
        }

        val newFaces = ArrayList<TriangleFace>(node.faces.size * 4)
        for (f in node.faces) {
            val ab = getMidpoint(f.v0, f.v1)
            val bc = getMidpoint(f.v1, f.v2)
            val ca = getMidpoint(f.v2, f.v0)
            newFaces.add(TriangleFace(f.v0, ab, ca))
            newFaces.add(TriangleFace(f.v1, bc, ab))
            newFaces.add(TriangleFace(f.v2, ca, bc))
            newFaces.add(TriangleFace(ab, bc, ca))
        }

        val subdivided = node.copy(vertices = verts, faces = newFaces)
        return if (smooth) smoothLaplacian(subdivided, iterations = 1, lambda = 0.28f) else subdivided
    }

    /**
     * Laplacian mesh smoothing using vertex neighbor adjacency graph.
     */
    fun smoothLaplacian(node: SceneNode3D, iterations: Int = 1, lambda: Float = 0.42f): SceneNode3D {
        var currentVerts = node.vertices
        val n = currentVerts.size
        if (n == 0) return node

        val neighbors = Array(n) { HashSet<Int>() }
        for (f in node.faces) {
            if (f.v0 in 0 until n && f.v1 in 0 until n && f.v2 in 0 until n) {
                neighbors[f.v0].add(f.v1); neighbors[f.v0].add(f.v2)
                neighbors[f.v1].add(f.v0); neighbors[f.v1].add(f.v2)
                neighbors[f.v2].add(f.v0); neighbors[f.v2].add(f.v1)
            }
        }

        repeat(iterations.coerceIn(1, 5)) {
            val nextVerts = ArrayList<Vec3>(n)
            for (i in 0 until n) {
                val adj = neighbors[i]
                if (adj.isEmpty()) {
                    nextVerts.add(currentVerts[i])
                } else {
                    var sum = Vec3(0f, 0f, 0f)
                    for (nb in adj) {
                        sum = sum + currentVerts[nb]
                    }
                    val avg = sum / adj.size.toFloat()
                    val blended = currentVerts[i] * (1f - lambda) + avg * lambda
                    nextVerts.add(blended)
                }
            }
            currentVerts = nextVerts
        }
        return node.copy(vertices = currentVerts)
    }

    /**
     * Extrudes upper/outer faces along their outward normals and builds connecting side walls.
     */
    fun extrudeTopFaces(node: SceneNode3D, distance: Float = 0.35f): SceneNode3D {
        val verts = node.vertices.toMutableList()
        val keptFaces = mutableListOf<TriangleFace>()
        val extrudedFaces = mutableListOf<TriangleFace>()

        for (f in node.faces) {
            val p0 = verts[f.v0]
            val p1 = verts[f.v1]
            val p2 = verts[f.v2]
            val normal = (p1 - p0).cross(p2 - p0).normalized()
            val center = (p0 + p1 + p2) / 3f

            if (normal.y > 0.55f && center.y > 0.15f) {
                val offset = normal * distance
                val n0 = verts.size; verts.add(p0 + offset)
                val n1 = verts.size; verts.add(p1 + offset)
                val n2 = verts.size; verts.add(p2 + offset)

                // Top cap
                extrudedFaces.add(TriangleFace(n0, n1, n2))
                // Side walls
                extrudedFaces.add(TriangleFace(f.v0, f.v1, n1))
                extrudedFaces.add(TriangleFace(f.v0, n1, n0))
                extrudedFaces.add(TriangleFace(f.v1, f.v2, n2))
                extrudedFaces.add(TriangleFace(f.v1, n2, n1))
                extrudedFaces.add(TriangleFace(f.v2, f.v0, n0))
                extrudedFaces.add(TriangleFace(f.v2, n0, n2))
            } else {
                keptFaces.add(f)
            }
        }
        keptFaces.addAll(extrudedFaces)
        return node.copy(vertices = verts, faces = keptFaces)
    }

    /**
     * Twists mesh vertices around the chosen axis by angleDegrees from min to max coordinate.
     */
    fun twistMesh(node: SceneNode3D, angleDegrees: Float = 45f, axis: Axis = Axis.Y): SceneNode3D {
        if (node.vertices.isEmpty()) return node
        val coords = node.vertices.map {
            when (axis) {
                Axis.X -> it.x
                Axis.Y -> it.y
                Axis.Z -> it.z
            }
        }
        val minC = coords.minOrNull() ?: -1f
        val maxC = coords.maxOrNull() ?: 1f
        val span = (maxC - minC).coerceAtLeast(1e-4f)
        val totalRad = Math.toRadians(angleDegrees.toDouble()).toFloat()

        val twisted = node.vertices.map { v ->
            val c = when (axis) {
                Axis.X -> v.x
                Axis.Y -> v.y
                Axis.Z -> v.z
            }
            val t = (c - minC) / span - 0.5f
            val theta = t * totalRad
            when (axis) {
                Axis.X -> v.rotateX(theta)
                Axis.Y -> v.rotateY(theta)
                Axis.Z -> v.rotateZ(theta)
            }
        }
        return node.copy(vertices = twisted)
    }

    /**
     * Tapers cross-section linearly along Y axis (top flare or pinch).
     */
    fun taperMesh(node: SceneNode3D, topScaleFactor: Float = 0.65f): SceneNode3D {
        if (node.vertices.isEmpty()) return node
        val minY = node.vertices.minOf { it.y }
        val maxY = node.vertices.maxOf { it.y }
        val span = (maxY - minY).coerceAtLeast(1e-4f)

        val tapered = node.vertices.map { v ->
            val t = (v.y - minY) / span
            val factor = 1f + (topScaleFactor - 1f) * t
            Vec3(v.x * factor, v.y, v.z * factor)
        }
        return node.copy(vertices = tapered)
    }

    /**
     * Inflates or deflates vertices along their averaged outward vertex normals (Sculpt Inflate/Deflate).
     */
    fun inflateSculpt(node: SceneNode3D, amount: Float = 0.12f): SceneNode3D {
        val accum = Array(node.vertices.size) { Vec3(0f, 0f, 0f) }
        for (f in node.faces) {
            if (f.v0 in node.vertices.indices && f.v1 in node.vertices.indices && f.v2 in node.vertices.indices) {
                val p0 = node.vertices[f.v0]
                val p1 = node.vertices[f.v1]
                val p2 = node.vertices[f.v2]
                val n = (p1 - p0).cross(p2 - p0).normalized()
                accum[f.v0] = accum[f.v0] + n
                accum[f.v1] = accum[f.v1] + n
                accum[f.v2] = accum[f.v2] + n
            }
        }
        val displaced = node.vertices.mapIndexed { idx, v ->
            v + accum[idx].normalized() * amount
        }
        return node.copy(vertices = displaced)
    }

    /**
     * Voxel Vertex Clustering Decimation (low-poly stylizer & polygon optimizer).
     */
    fun voxelDecimate(node: SceneNode3D, cellSize: Float = 0.22f): SceneNode3D {
        val (verts, faces) = weldCloseVertices(node.vertices, node.faces, cellSize.coerceAtLeast(0.05f))
        return if (faces.size >= 4) node.copy(vertices = verts, faces = faces) else node
    }

    /**
     * Mirrors a node across X, Y, or Z axis and corrects triangle winding order so normals stay valid.
     */
    fun mirrorNode(node: SceneNode3D, axis: Axis = Axis.X): SceneNode3D {
        val mirroredVerts = node.vertices.map { v ->
            when (axis) {
                Axis.X -> v.copy(x = -v.x)
                Axis.Y -> v.copy(y = -v.y)
                Axis.Z -> v.copy(z = -v.z)
            }
        }
        val mirroredPos = when (axis) {
            Axis.X -> node.position.copy(x = -node.position.x)
            Axis.Y -> node.position.copy(y = -node.position.y)
            Axis.Z -> node.position.copy(z = -node.position.z)
        }
        val flippedFaces = node.faces.map { TriangleFace(it.v0, it.v2, it.v1) }
        return node.copy(
            id = "mirror_${UUID.randomUUID().toString().take(6)}",
            name = "${node.name} (Mirror)",
            nameFa = "${node.nameFa} (قرینه)",
            vertices = mirroredVerts,
            faces = flippedFaces,
            position = mirroredPos
        )
    }

    /**
     * Aligns the object's lowest world vertex exactly onto the Y = 0.0 build plate for 3D printing.
     */
    fun dropToFloor(node: SceneNode3D): SceneNode3D {
        val worldVerts = node.worldVertices()
        val minY = worldVerts.minOfOrNull { it.y } ?: 0f
        return node.copy(position = node.position.copy(y = node.position.y - minY))
    }

    /**
     * Boolean Union / Scene Weld: bakes world transforms and combines multiple nodes into one unified mesh.
     */
    fun mergeNodesIntoSingleMesh(nodes: List<SceneNode3D>): SceneNode3D? {
        val visibleNodes = nodes.filter { it.visible && it.vertices.isNotEmpty() }
        if (visibleNodes.isEmpty()) return null

        val allVerts = mutableListOf<Vec3>()
        val allFaces = mutableListOf<TriangleFace>()

        for (n in visibleNodes) {
            val offset = allVerts.size
            val wVerts = n.worldVertices()
            allVerts.addAll(wVerts)
            for (f in n.faces) {
                allFaces.add(TriangleFace(f.v0 + offset, f.v1 + offset, f.v2 + offset))
            }
        }

        val first = visibleNodes.first()
        return SceneNode3D(
            id = "merged_${UUID.randomUUID().toString().take(6)}",
            name = "Merged Assembly (${visibleNodes.size} parts)",
            nameFa = "مش یکپارچه (${visibleNodes.size} قطعه)",
            typeTag = "Merged",
            vertices = allVerts,
            faces = allFaces,
            position = Vec3(0f, 0f, 0f),
            rotation = Vec3(0f, 0f, 0f),
            scale = Vec3(1f, 1f, 1f),
            material = first.material
        )
    }

    /**
     * Computes real-time CAD & 3D Printing Engineering statistics across visible scene nodes.
     * Studio unit convention: 1.0 unit = 10.0 mm (1.0 cm).
     */
    fun computeEngineeringStats(nodes: List<SceneNode3D>): MeshEngineeringStats {
        val visibleNodes = nodes.filter { it.visible && it.vertices.isNotEmpty() }
        if (visibleNodes.isEmpty()) {
            return MeshEngineeringStats(
                objectCount = 0,
                totalVertices = 0,
                totalTriangles = 0,
                widthMm = 0f,
                heightMm = 0f,
                depthMm = 0f,
                surfaceAreaCm2 = 0f,
                volumeCm3 = 0f,
                estimatedPlaGrams = 0f,
                estimatedResinGrams = 0f,
                estimatedPrintMinutes = 0,
                isManifoldWatertight = true,
                minBounds = Vec3(),
                maxBounds = Vec3()
            )
        }

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        var totalVerts = 0
        var totalTris = 0
        var totalAreaUnits2 = 0.0
        var totalVolumeUnits3 = 0.0
        var allManifold = true

        for (node in visibleNodes) {
            val wVerts = node.worldVertices()
            totalVerts += wVerts.size
            totalTris += node.faces.size

            for (v in wVerts) {
                if (v.x < minX) minX = v.x
                if (v.y < minY) minY = v.y
                if (v.z < minZ) minZ = v.z
                if (v.x > maxX) maxX = v.x
                if (v.y > maxY) maxY = v.y
                if (v.z > maxZ) maxZ = v.z
            }

            var nodeSignedVol = 0.0
            val edgeCounts = HashMap<Long, Int>()
            fun recordEdge(a: Int, b: Int) {
                val u = min(a, b).toLong()
                val v = max(a, b).toLong()
                val key = (u shl 32) or v
                edgeCounts[key] = (edgeCounts[key] ?: 0) + 1
            }

            for (f in node.faces) {
                if (f.v0 in wVerts.indices && f.v1 in wVerts.indices && f.v2 in wVerts.indices) {
                    val p0 = wVerts[f.v0]
                    val p1 = wVerts[f.v1]
                    val p2 = wVerts[f.v2]

                    // Triangle area = 0.5 * |(p1 - p0) x (p2 - p0)|
                    val cross = (p1 - p0).cross(p2 - p0)
                    totalAreaUnits2 += 0.5 * cross.length()

                    // Signed tetrahedron volume = (p0 . (p1 x p2)) / 6.0
                    nodeSignedVol += p0.dot(p1.cross(p2)) / 6.0

                    recordEdge(f.v0, f.v1)
                    recordEdge(f.v1, f.v2)
                    recordEdge(f.v2, f.v0)
                }
            }

            totalVolumeUnits3 += abs(nodeSignedVol)
            if (edgeCounts.values.any { it < 2 }) {
                allManifold = false
            }
        }

        // 1 unit = 10 mm = 1 cm -> 1 unit^2 = 1 cm^2, 1 unit^3 = 1 cm^3
        val widthMm = (maxX - minX) * 10f
        val heightMm = (maxY - minY) * 10f
        val depthMm = (maxZ - minZ) * 10f
        val areaCm2 = totalAreaUnits2.toFloat()
        val volumeCm3 = totalVolumeUnits3.toFloat().coerceAtLeast(0.1f)

        // Typical FDM infill + shell factor ~ 0.38 of solid volume, PLA density = 1.24 g/cm3
        val effectiveVolume = volumeCm3 * 0.42f
        val plaGrams = effectiveVolume * 1.24f
        val resinGrams = volumeCm3 * 1.12f
        val printMinutes = max(8, (plaGrams * 3.2f + heightMm * 0.45f).roundToInt())

        return MeshEngineeringStats(
            objectCount = visibleNodes.size,
            totalVertices = totalVerts,
            totalTriangles = totalTris,
            widthMm = widthMm,
            heightMm = heightMm,
            depthMm = depthMm,
            surfaceAreaCm2 = areaCm2,
            volumeCm3 = volumeCm3,
            estimatedPlaGrams = plaGrams,
            estimatedResinGrams = resinGrams,
            estimatedPrintMinutes = printMinutes,
            isManifoldWatertight = allManifold,
            minBounds = Vec3(minX, minY, minZ),
            maxBounds = Vec3(maxX, maxY, maxZ)
        )
    }
}
