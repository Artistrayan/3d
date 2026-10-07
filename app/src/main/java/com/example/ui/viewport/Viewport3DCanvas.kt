package com.example.ui.viewport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.engine.TextureEngine
import com.example.model.AxisConstraint
import com.example.model.MeshEngineeringStats
import com.example.model.SceneNode3D
import com.example.model.StudioLightingPreset
import com.example.model.TransformToolMode
import com.example.model.Vec3
import com.example.model.ViewportShadingMode
import com.example.ui.theme.AxisBlueZ
import com.example.ui.theme.AxisGreenY
import com.example.ui.theme.AxisRedX
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SculptAmber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private data class ProjectedTriangle(
    val nodeId: String,
    val isSelected: Boolean,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val depthZ: Float,
    val fillColor: Color,
    val wireColor: Color?,
    val normalStartScreen: Offset?,
    val normalEndScreen: Offset?
)

private data class NodeScreenHitBox(
    val nodeId: String,
    val centerScreen: Offset,
    val radiusPx: Float,
    val depthZ: Float
)

@Composable
fun Viewport3DCanvas(
    nodes: List<SceneNode3D>,
    selectedNodeId: String?,
    shadingMode: ViewportShadingMode,
    lightingPreset: StudioLightingPreset,
    toolMode: TransformToolMode,
    axisConstraint: AxisConstraint,
    isOrthographic: Boolean,
    showGrid: Boolean,
    showWireframeOverlay: Boolean,
    showNormals: Boolean,
    showVertices: Boolean,
    showDimensions: Boolean,
    autoTurntable: Boolean,
    cameraYaw: Float,
    cameraPitch: Float,
    cameraZoom: Float,
    cameraPanX: Float,
    cameraPanY: Float,
    engineeringStats: MeshEngineeringStats,
    onOrbitCamera: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onZoomPanCamera: (zoomFactor: Float, panDx: Float, panDy: Float) -> Unit,
    onTransformSelectedByDrag: (dx: Float, dy: Float) -> Unit,
    onSelectNode: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val reusablePath = remember { Path() }

    val currentToolMode by rememberUpdatedState(toolMode)
    val currentSelectedId by rememberUpdatedState(selectedNodeId)
    val currentOnOrbit by rememberUpdatedState(onOrbitCamera)
    val currentOnZoomPan by rememberUpdatedState(onZoomPanCamera)
    val currentOnTransformDrag by rememberUpdatedState(onTransformSelectedByDrag)
    val currentOnSelectNode by rememberUpdatedState(onSelectNode)

    var lastHitBoxes by remember { androidx.compose.runtime.mutableStateOf<List<NodeScreenHitBox>>(emptyList()) }
    var turntableOffsetDeg by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(autoTurntable) {
        if (autoTurntable) {
            var lastTime = withFrameNanos { it }
            while (autoTurntable) {
                val now = withFrameNanos { it }
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now
                turntableOffsetDeg = (turntableOffsetDeg + dt * 28f) % 360f
            }
        } else {
            turntableOffsetDeg = 0f
        }
    }

    val effectiveYaw = cameraYaw + turntableOffsetDeg

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF17223B),
                        Color(0xFF0B101E),
                        Color(0xFF070A14)
                    )
                )
            )
            .testTag("viewport_3d_canvas")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (abs(zoom - 1f) > 0.015f) {
                        currentOnZoomPan(zoom, pan.x, pan.y)
                    } else if (currentToolMode != TransformToolMode.SELECT_ORBIT && currentSelectedId != null) {
                        currentOnTransformDrag(pan.x, pan.y)
                    } else {
                        currentOnOrbit(pan.x * 0.42f, pan.y * 0.38f)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val hit = lastHitBoxes
                        .filter { (it.centerScreen - tapOffset).getDistance() <= max(it.radiusPx, 36f) }
                        .minByOrNull { it.depthZ }
                    if (hit != null) {
                        currentOnSelectNode(hit.nodeId)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 10f || h <= 10f) return@Canvas

            val cx = w * 0.5f + cameraPanX
            val cy = h * 0.50f + cameraPanY
            val baseScale = min(w, h) * 0.56f

            val yawRad = Math.toRadians(effectiveYaw.toDouble()).toFloat()
            val pitchRad = Math.toRadians(cameraPitch.toDouble()).toFloat()
            val cosY = cos(yawRad); val sinY = sin(yawRad)
            val cosP = cos(pitchRad); val sinP = sin(pitchRad)
            val camDist = cameraZoom.coerceIn(1.8f, 22f)
            val fovFactor = 3.9f

            fun worldToCamera(v: Vec3): Vec3 {
                val x1 = v.x * cosY + v.z * sinY
                val z1 = -v.x * sinY + v.z * cosY
                val y2 = v.y * cosP - z1 * sinP
                val z2 = v.y * sinP + z1 * cosP
                return Vec3(x1, y2, z2 + camDist)
            }

            fun cameraToScreen(cam: Vec3): Offset? {
                if (cam.z < 0.25f) return null
                val factor = if (isOrthographic) {
                    baseScale / (camDist * 0.62f)
                } else {
                    (baseScale * fovFactor) / cam.z
                }
                return Offset(cx + cam.x * factor, cy - cam.y * factor)
            }

            fun projectWorld(v: Vec3): Pair<Offset, Float>? {
                val cam = worldToCamera(v)
                val scr = cameraToScreen(cam) ?: return null
                return scr to cam.z
            }

            // 1. Draw Full-Screen Perspective CAD Ground Grid & Build Plate (Y = -1.0f default floor)
            val gridY = -1.0f
            if (showGrid) {
                val gridRange = 10
                for (i in -gridRange..gridRange) {
                    val f = i.toFloat()
                    val distFade = (1f - (abs(i) / (gridRange + 2f)) * 0.55f).coerceIn(0.2f, 1f)
                    val pStartX = projectWorld(Vec3(-gridRange.toFloat(), gridY, f))
                    val pEndX = projectWorld(Vec3(gridRange.toFloat(), gridY, f))
                    if (pStartX != null && pEndX != null) {
                        val isCenterZ = i == 0
                        drawLine(
                            color = if (isCenterZ) AxisRedX.copy(alpha = 0.8f) else Color(0xFF334155).copy(alpha = (if (i % 2 == 0) 0.52f else 0.28f) * distFade),
                            start = pStartX.first,
                            end = pEndX.first,
                            strokeWidth = if (isCenterZ) 2.6f else if (i % 2 == 0) 1.4f else 0.9f
                        )
                    }
                    val pStartZ = projectWorld(Vec3(f, gridY, -gridRange.toFloat()))
                    val pEndZ = projectWorld(Vec3(f, gridY, gridRange.toFloat()))
                    if (pStartZ != null && pEndZ != null) {
                        val isCenterX = i == 0
                        drawLine(
                            color = if (isCenterX) AxisBlueZ.copy(alpha = 0.8f) else Color(0xFF334155).copy(alpha = (if (i % 2 == 0) 0.52f else 0.28f) * distFade),
                            start = pStartZ.first,
                            end = pEndZ.first,
                            strokeWidth = if (isCenterX) 2.6f else if (i % 2 == 0) 1.4f else 0.9f
                        )
                    }
                }

                val originBottom = projectWorld(Vec3(0f, gridY, 0f))
                val originTop = projectWorld(Vec3(0f, gridY + 2.8f, 0f))
                if (originBottom != null && originTop != null) {
                    drawLine(
                        color = AxisGreenY.copy(alpha = 0.42f),
                        start = originBottom.first,
                        end = originTop.first,
                        strokeWidth = 1.6f
                    )
                }
            }

            // 2. Collect & Shade All Visible Triangles (with Real-Time Procedural + Hand-Painted Texture Sampling!)
            val triangleBuffer = ArrayList<ProjectedTriangle>(2048)
            val vertexDotList = ArrayList<Pair<Offset, Boolean>>()
            val hitBoxes = ArrayList<NodeScreenHitBox>(nodes.size)

            val keyDir = lightingPreset.keyLightDir
            val keyColor = Color(lightingPreset.keyColorHex)
            val skyColor = Color(lightingPreset.ambientSkyHex)
            val groundColor = Color(lightingPreset.ambientGroundHex)
            val rimColor = Color(lightingPreset.rimColorHex)

            val viewDirWorld = Vec3(
                -sinY * cosP,
                sinP,
                cosY * cosP
            ).normalized()
            val halfVec = (keyDir + viewDirWorld).normalized()

            for (node in nodes) {
                if (!node.visible || node.vertices.isEmpty()) continue
                val isSelected = node.id == selectedNodeId
                val localVerts = node.vertices
                val worldVerts = node.worldVertices()

                val projOffsets = arrayOfNulls<Offset>(worldVerts.size)
                val projDepths = FloatArray(worldVerts.size)

                var sumX = 0f; var sumY = 0f; var validCount = 0
                var minScreenX = Float.POSITIVE_INFINITY; var maxScreenX = Float.NEGATIVE_INFINITY
                var minScreenY = Float.POSITIVE_INFINITY; var maxScreenY = Float.NEGATIVE_INFINITY
                var avgDepth = 0f

                for (i in worldVerts.indices) {
                    val cam = worldToCamera(worldVerts[i])
                    val scr = cameraToScreen(cam)
                    if (scr != null) {
                        projOffsets[i] = scr
                        projDepths[i] = cam.z
                        sumX += scr.x; sumY += scr.y; avgDepth += cam.z
                        validCount++
                        if (scr.x < minScreenX) minScreenX = scr.x
                        if (scr.x > maxScreenX) maxScreenX = scr.x
                        if (scr.y < minScreenY) minScreenY = scr.y
                        if (scr.y > maxScreenY) maxScreenY = scr.y

                        if (showVertices) {
                            vertexDotList.add(scr to isSelected)
                        }
                    }
                }

                if (validCount > 0) {
                    val centerScr = Offset(sumX / validCount, sumY / validCount)
                    val radiusPx = max(maxScreenX - minScreenX, maxScreenY - minScreenY) * 0.45f
                    hitBoxes.add(
                        NodeScreenHitBox(
                            nodeId = node.id,
                            centerScreen = centerScr,
                            radiusPx = radiusPx.coerceAtLeast(28f),
                            depthZ = avgDepth / validCount
                        )
                    )

                    val shadowCenter3D = Vec3(node.position.x, gridY + 0.01f, node.position.z)
                    val shadowLeft3D = Vec3(node.position.x - 0.75f * node.scale.x, gridY + 0.01f, node.position.z)
                    val sCenter = projectWorld(shadowCenter3D)
                    val sEdge = projectWorld(shadowLeft3D)
                    if (sCenter != null && sEdge != null) {
                        val sr = abs(sCenter.first.x - sEdge.first.x).coerceIn(12f, 160f)
                        drawOval(
                            color = Color.Black.copy(alpha = 0.28f),
                            topLeft = Offset(sCenter.first.x - sr, sCenter.first.y - sr * 0.42f),
                            size = androidx.compose.ui.geometry.Size(sr * 2f, sr * 0.84f)
                        )
                    }
                }

                val mat = node.material
                val hasTex = mat.hasActiveTexture() && shadingMode != ViewportShadingMode.CLAY_SCULPT
                val fallbackBaseColor = if (shadingMode == ViewportShadingMode.CLAY_SCULPT) {
                    Color(0xFFD97757)
                } else {
                    Color(mat.baseColorHex)
                }
                val baseMetallic = if (shadingMode == ViewportShadingMode.CLAY_SCULPT) 0.05f else mat.metallic
                val baseRoughness = if (shadingMode == ViewportShadingMode.CLAY_SCULPT) 0.65f else mat.roughness
                val emitColor = Color(mat.emissionHex)
                val emitStrength = mat.emissionStrength

                for (f in node.faces) {
                    val s0 = projOffsets.getOrNull(f.v0) ?: continue
                    val s1 = projOffsets.getOrNull(f.v1) ?: continue
                    val s2 = projOffsets.getOrNull(f.v2) ?: continue

                    val p0 = worldVerts[f.v0]
                    val p1 = worldVerts[f.v1]
                    val p2 = worldVerts[f.v2]

                    val faceNormal = (p1 - p0).cross(p2 - p0).normalized()
                    val centroid = (p0 + p1 + p2) / 3f
                    val avgZ = (projDepths[f.v0] + projDepths[f.v1] + projDepths[f.v2]) / 3f

                    // Sample 3D UV & Texture if active!
                    val (surfaceColor, roughnessDelta) = if (hasTex) {
                        val loc0 = localVerts[f.v0]
                        val loc1 = localVerts[f.v1]
                        val loc2 = localVerts[f.v2]
                        val localCentroid = (loc0 + loc1 + loc2) / 3f
                        val localNormal = (loc1 - loc0).cross(loc2 - loc0).normalized()
                        val (u, v) = TextureEngine.computeUV(localCentroid, localNormal, mat.uvMappingMode)
                        TextureEngine.sampleTextureAndRoughness(u, v, mat)
                    } else {
                        fallbackBaseColor to 0f
                    }

                    val roughness = (baseRoughness + roughnessDelta).coerceIn(0.04f, 1.0f)
                    val metallic = baseMetallic

                    val nDotVRaw = faceNormal.dot(viewDirWorld)
                    val orientedNormal = if (nDotVRaw < 0f) faceNormal * -1f else faceNormal
                    val nDotV = abs(nDotVRaw).coerceIn(0f, 1f)

                    val nDotL = orientedNormal.dot(keyDir).coerceAtLeast(0f)
                    val hemiFactor = (orientedNormal.y * 0.5f + 0.5f).coerceIn(0f, 1f)
                    val ambR = groundColor.red * (1f - hemiFactor) + skyColor.red * hemiFactor
                    val ambG = groundColor.green * (1f - hemiFactor) + skyColor.green * hemiFactor
                    val ambB = groundColor.blue * (1f - hemiFactor) + skyColor.blue * hemiFactor

                    val nDotH = orientedNormal.dot(halfVec).coerceAtLeast(0f)
                    val shininess = (2f / (roughness * roughness + 0.04f)).coerceIn(4f, 128f)
                    val specIntensity = nDotH.pow(shininess) * (1.15f - roughness * 0.75f) * (0.25f + metallic * 0.95f)
                    val rim = (1f - nDotV).pow(3.0f) * (0.28f + metallic * 0.35f)

                    val diffuseWeight = (1f - metallic * 0.45f)
                    val litR = (surfaceColor.red * (0.28f + ambR * 0.38f + nDotL * keyColor.red * 0.85f * diffuseWeight) +
                            (keyColor.red * 0.6f + surfaceColor.red * 0.4f) * specIntensity +
                            rimColor.red * rim +
                            emitColor.red * emitStrength).coerceIn(0f, 1f)

                    val litG = (surfaceColor.green * (0.28f + ambG * 0.38f + nDotL * keyColor.green * 0.85f * diffuseWeight) +
                            (keyColor.green * 0.6f + surfaceColor.green * 0.4f) * specIntensity +
                            rimColor.green * rim +
                            emitColor.green * emitStrength).coerceIn(0f, 1f)

                    val litB = (surfaceColor.blue * (0.28f + ambB * 0.38f + nDotL * keyColor.blue * 0.85f * diffuseWeight) +
                            (keyColor.blue * 0.6f + surfaceColor.blue * 0.4f) * specIntensity +
                            rimColor.blue * rim +
                            emitColor.blue * emitStrength).coerceIn(0f, 1f)

                    val alpha = when (shadingMode) {
                        ViewportShadingMode.WIREFRAME -> 0.0f
                        ViewportShadingMode.XRAY -> 0.38f
                        else -> mat.opacity.coerceIn(0.15f, 1.0f)
                    }

                    val wireCol = when {
                        shadingMode == ViewportShadingMode.WIREFRAME ->
                            if (isSelected) CyberCyan else surfaceColor.copy(alpha = 0.82f)
                        shadingMode == ViewportShadingMode.SHADED_WIRE || showWireframeOverlay ->
                            if (isSelected) CyberCyan.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.32f)
                        shadingMode == ViewportShadingMode.XRAY ->
                            if (isSelected) CyberCyan.copy(alpha = 0.85f) else surfaceColor.copy(alpha = 0.55f)
                        isSelected ->
                            CyberCyan.copy(alpha = 0.16f)
                        else -> null
                    }

                    var normStart: Offset? = null
                    var normEnd: Offset? = null
                    if (showNormals) {
                        normStart = projectWorld(centroid)?.first
                        normEnd = projectWorld(centroid + orientedNormal * 0.24f)?.first
                    }

                    triangleBuffer.add(
                        ProjectedTriangle(
                            nodeId = node.id,
                            isSelected = isSelected,
                            x0 = s0.x, y0 = s0.y,
                            x1 = s1.x, y1 = s1.y,
                            x2 = s2.x, y2 = s2.y,
                            depthZ = avgZ,
                            fillColor = Color(litR, litG, litB, alpha),
                            wireColor = wireCol,
                            normalStartScreen = normStart,
                            normalEndScreen = normEnd
                        )
                    )
                }
            }

            lastHitBoxes = hitBoxes

            triangleBuffer.sortByDescending { it.depthZ }

            for (tri in triangleBuffer) {
                reusablePath.reset()
                reusablePath.moveTo(tri.x0, tri.y0)
                reusablePath.lineTo(tri.x1, tri.y1)
                reusablePath.lineTo(tri.x2, tri.y2)
                reusablePath.close()

                if (tri.fillColor.alpha > 0.01f) {
                    drawPath(
                        path = reusablePath,
                        color = tri.fillColor,
                        style = Fill
                    )
                }
                if (tri.wireColor != null) {
                    drawPath(
                        path = reusablePath,
                        color = tri.wireColor,
                        style = Stroke(width = if (tri.isSelected) 1.35f else 0.9f)
                    )
                }
                if (tri.normalStartScreen != null && tri.normalEndScreen != null) {
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.8f),
                        start = tri.normalStartScreen,
                        end = tri.normalEndScreen,
                        strokeWidth = 1.4f
                    )
                }
            }

            if (showVertices) {
                for ((pt, sel) in vertexDotList) {
                    drawCircle(
                        color = if (sel) SculptAmber else CyberCyan,
                        radius = if (sel) 3.2f else 2.2f,
                        center = pt
                    )
                }
            }

            if (showDimensions && engineeringStats.objectCount > 0) {
                drawCadBoundingBox(
                    minB = engineeringStats.minBounds,
                    maxB = engineeringStats.maxBounds,
                    stats = engineeringStats,
                    projectWorld = ::projectWorld,
                    textMeasurer = textMeasurer
                )
            }

            val selectedNode = nodes.firstOrNull { it.id == selectedNodeId && it.visible }
            if (selectedNode != null) {
                drawTransformGizmo(
                    center = selectedNode.position,
                    toolMode = toolMode,
                    axisConstraint = axisConstraint,
                    projectWorld = ::projectWorld
                )
            }

            drawOrientationCompass(
                cosY = cosY,
                sinY = sinY,
                cosP = cosP,
                sinP = sinP,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawTransformGizmo(
    center: Vec3,
    toolMode: TransformToolMode,
    axisConstraint: AxisConstraint,
    projectWorld: (Vec3) -> Pair<Offset, Float>?
) {
    val originScr = projectWorld(center)?.first ?: return
    val axisLen = 1.35f

    val axes = listOf(
        Triple(Vec3(axisLen, 0f, 0f), AxisRedX, AxisConstraint.X),
        Triple(Vec3(0f, axisLen, 0f), AxisGreenY, AxisConstraint.Y),
        Triple(Vec3(0f, 0f, axisLen), AxisBlueZ, AxisConstraint.Z)
    )

    for ((dir, color, axisType) in axes) {
        val endScr = projectWorld(center + dir)?.first ?: continue
        val isHighlighted = axisConstraint == AxisConstraint.ALL || axisConstraint == axisType
        val strokeW = if (isHighlighted) 5.2f else 2.2f
        val drawCol = if (isHighlighted) color else color.copy(alpha = 0.35f)

        drawLine(
            color = drawCol,
            start = originScr,
            end = endScr,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        when (toolMode) {
            TransformToolMode.SCALE -> {
                drawCircle(color = drawCol, radius = 7.5f, center = endScr)
                drawCircle(color = Color.White, radius = 3.0f, center = endScr)
            }
            TransformToolMode.ROTATE -> {
                drawCircle(
                    color = drawCol,
                    radius = 9f,
                    center = endScr,
                    style = Stroke(width = 2.6f)
                )
            }
            else -> {
                drawCircle(color = drawCol, radius = 6.5f, center = endScr)
            }
        }
    }

    drawCircle(color = Color.White, radius = 6.5f, center = originScr)
    drawCircle(color = CyberCyan, radius = 4.0f, center = originScr)
}

private fun DrawScope.drawCadBoundingBox(
    minB: Vec3,
    maxB: Vec3,
    stats: MeshEngineeringStats,
    projectWorld: (Vec3) -> Pair<Offset, Float>?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val corners = listOf(
        Vec3(minB.x, minB.y, minB.z),
        Vec3(maxB.x, minB.y, minB.z),
        Vec3(maxB.x, minB.y, maxB.z),
        Vec3(minB.x, minB.y, maxB.z),
        Vec3(minB.x, maxB.y, minB.z),
        Vec3(maxB.x, maxB.y, minB.z),
        Vec3(maxB.x, maxB.y, maxB.z),
        Vec3(minB.x, maxB.y, maxB.z)
    )
    val proj = corners.map { projectWorld(it)?.first }
    val edges = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0,
        4 to 5, 5 to 6, 6 to 7, 7 to 4,
        0 to 4, 1 to 5, 2 to 6, 3 to 7
    )
    for ((a, b) in edges) {
        val pa = proj[a] ?: continue
        val pb = proj[b] ?: continue
        drawLine(
            color = CyberCyan.copy(alpha = 0.45f),
            start = pa,
            end = pb,
            strokeWidth = 1.3f
        )
    }

    val labelStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = CyberCyan
    )

    val midX = projectWorld(Vec3((minB.x + maxB.x) * 0.5f, minB.y, maxB.z))?.first
    if (midX != null) {
        val txt = String.format(Locale.US, "W: %.1f mm", stats.widthMm)
        drawText(textMeasurer, txt, Offset(midX.x - 28f, midX.y + 6f), style = labelStyle)
    }
    val midY = projectWorld(Vec3(maxB.x, (minB.y + maxB.y) * 0.5f, maxB.z))?.first
    if (midY != null) {
        val txt = String.format(Locale.US, "H: %.1f mm", stats.heightMm)
        drawText(textMeasurer, txt, Offset(midY.x + 8f, midY.y - 8f), style = labelStyle.copy(color = AxisGreenY))
    }
}

private fun DrawScope.drawOrientationCompass(
    cosY: Float,
    sinY: Float,
    cosP: Float,
    sinP: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val gizmoCenter = Offset(size.width - 115f, 290f)
    val armLen = 30f

    drawCircle(
        color = Color(0xFF111827).copy(alpha = 0.78f),
        radius = 40f,
        center = gizmoCenter
    )
    drawCircle(
        color = Color(0xFF334155),
        radius = 40f,
        center = gizmoCenter,
        style = Stroke(width = 1.2f)
    )

    fun rotDir(v: Vec3): Vec3 {
        val x1 = v.x * cosY + v.z * sinY
        val z1 = -v.x * sinY + v.z * cosY
        val y2 = v.y * cosP - z1 * sinP
        val z2 = v.y * sinP + z1 * cosP
        return Vec3(x1, y2, z2)
    }

    val axes = listOf(
        Triple(rotDir(Vec3(1f, 0f, 0f)), AxisRedX, "X"),
        Triple(rotDir(Vec3(0f, 1f, 0f)), AxisGreenY, "Y"),
        Triple(rotDir(Vec3(0f, 0f, 1f)), AxisBlueZ, "Z")
    ).sortedBy { it.first.z }

    val labelStyle = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    for ((dir, col, name) in axes) {
        val tip = Offset(gizmoCenter.x + dir.x * armLen, gizmoCenter.y - dir.y * armLen)
        drawLine(
            color = col,
            start = gizmoCenter,
            end = tip,
            strokeWidth = 3.0f,
            cap = StrokeCap.Round
        )
        drawCircle(color = col, radius = 8.5f, center = tip)
        drawText(
            textMeasurer = textMeasurer,
            text = name,
            topLeft = Offset(tip.x - 3.5f, tip.y - 6.5f),
            style = labelStyle
        )
    }
}
