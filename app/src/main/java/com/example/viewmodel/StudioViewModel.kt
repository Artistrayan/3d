package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExportLogEntity
import com.example.data.PolyForgeDatabase
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.engine.Axis
import com.example.engine.ExportPayload
import com.example.engine.FileExporterImporter
import com.example.engine.MeshModifiers
import com.example.engine.PrimitiveGenerator
import com.example.engine.PrimitiveType3D
import com.example.engine.ReadyModelPreset
import com.example.model.AxisConstraint
import com.example.model.ExportFormat3D
import com.example.model.MeshEngineeringStats
import com.example.model.PbrMaterial
import com.example.model.SceneNode3D
import com.example.model.StudioLightingPreset
import com.example.model.TransformToolMode
import com.example.model.Vec3
import com.example.model.ViewportShadingMode
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

enum class StudioTab(val titleFa: String, val titleEn: String) {
    VIEWPORT("میز کار 3D", "3D Studio"),
    LIBRARY("مدل‌های آماده", "Asset Library"),
    SCULPT_MESH("ابزار و مش", "Mesh & Sculpt"),
    MATERIAL_LIGHT("متریال و نور", "PBR & Light"),
    EXPORT_PROJECTS("خروجی GLB/STL", "Export & Files")
}

data class StudioUiState(
    val projectName: String = "Cyber_Mecha_Studio",
    val activeTab: StudioTab = StudioTab.VIEWPORT,
    val isPersian: Boolean = true,
    val nodes: List<SceneNode3D> = emptyList(),
    val selectedNodeId: String? = null,
    val toolMode: TransformToolMode = TransformToolMode.SELECT_ORBIT,
    val axisConstraint: AxisConstraint = AxisConstraint.ALL,
    val snapToGrid: Boolean = false,
    val shadingMode: ViewportShadingMode = ViewportShadingMode.PBR_LIT,
    val lightingPreset: StudioLightingPreset = StudioLightingPreset.CYBER_STUDIO,
    val isOrthographic: Boolean = false,
    val showGrid: Boolean = true,
    val showWireframeOverlay: Boolean = false,
    val showNormals: Boolean = false,
    val showVertices: Boolean = false,
    val showDimensions: Boolean = true,
    val autoTurntable: Boolean = false,
    val cameraYaw: Float = -32f,
    val cameraPitch: Float = 22f,
    val cameraZoom: Float = 6.2f,
    val cameraPanX: Float = 0f,
    val cameraPanY: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val engineeringStats: MeshEngineeringStats = MeshModifiers.computeEngineeringStats(emptyList()),
    val selectedExportFormat: ExportFormat3D = ExportFormat3D.GLB,
    val activeExportPayload: ExportPayload? = null,
    val statusBannerMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    val savedProjects: StateFlow<List<ProjectEntity>>
    val recentExports: StateFlow<List<ExportLogEntity>>

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<List<SceneNode3D>>()
    private val redoStack = ArrayDeque<List<SceneNode3D>>()

    init {
        val db = PolyForgeDatabase.getInstance(application)
        repository = ProjectRepository(db.projectDao())
        savedProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        recentExports = repository.recentExports.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load initial showcase 3D model (Cyber Guardian Mecha) and seed starter projects in Room
        val initialNodes = PrimitiveGenerator.buildCyberMechaPreset()
        applySceneNodes(
            newNodes = initialNodes,
            selectedId = initialNodes.firstOrNull()?.id,
            pushHistory = false,
            newProjectName = "Cyber_Mecha_Bot"
        )
        seedStarterProjectsIfNeeded()
    }

    private fun seedStarterProjectsIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getProjectCount() == 0) {
                val presets = PrimitiveGenerator.getReadyModelPresets().take(4)
                for (preset in presets) {
                    val nodes = preset.generator()
                    val stats = MeshModifiers.computeEngineeringStats(nodes)
                    repository.saveProject(
                        ProjectEntity(
                            title = preset.titleFa,
                            description = preset.subtitleFa,
                            sceneJson = serializeNodesToJson(nodes),
                            objectCount = stats.objectCount,
                            vertexCount = stats.totalVertices,
                            triangleCount = stats.totalTriangles,
                            accentHex = preset.accentHex
                        )
                    )
                }
            }
        }
    }

    private fun pushUndoSnapshot(currentNodes: List<SceneNode3D>) {
        if (undoStack.size >= 25) {
            undoStack.removeFirst()
        }
        undoStack.addLast(currentNodes)
        redoStack.clear()
    }

    private fun applySceneNodes(
        newNodes: List<SceneNode3D>,
        selectedId: String? = _uiState.value.selectedNodeId,
        pushHistory: Boolean = true,
        newProjectName: String? = null,
        statusMessage: String? = null
    ) {
        if (pushHistory) {
            pushUndoSnapshot(_uiState.value.nodes)
        }
        val resolvedSelected = selectedId?.takeIf { id -> newNodes.any { it.id == id } }
            ?: newNodes.firstOrNull()?.id
        val stats = MeshModifiers.computeEngineeringStats(newNodes)
        val projectTitle = newProjectName ?: _uiState.value.projectName
        val exportFormat = _uiState.value.selectedExportFormat
        val previewPayload = FileExporterImporter.generateExport(newNodes, exportFormat, projectTitle)

        _uiState.update { state ->
            state.copy(
                projectName = projectTitle,
                nodes = newNodes,
                selectedNodeId = resolvedSelected,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                engineeringStats = stats,
                activeExportPayload = previewPayload,
                statusBannerMessage = statusMessage ?: state.statusBannerMessage
            )
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { state ->
            val refreshedExport = if (tab == StudioTab.EXPORT_PROJECTS) {
                FileExporterImporter.generateExport(state.nodes, state.selectedExportFormat, state.projectName)
            } else state.activeExportPayload
            state.copy(activeTab = tab, activeExportPayload = refreshedExport)
        }
    }

    fun toggleLanguage() {
        _uiState.update { it.copy(isPersian = !it.isPersian) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun updateProjectName(name: String) {
        _uiState.update { state ->
            val payload = FileExporterImporter.generateExport(state.nodes, state.selectedExportFormat, name)
            state.copy(projectName = name, activeExportPayload = payload)
        }
    }

    fun selectNode(nodeId: String?) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    fun setToolMode(mode: TransformToolMode) {
        _uiState.update { it.copy(toolMode = mode) }
    }

    fun setAxisConstraint(axis: AxisConstraint) {
        _uiState.update { it.copy(axisConstraint = axis) }
    }

    fun toggleSnapToGrid() {
        _uiState.update { it.copy(snapToGrid = !it.snapToGrid) }
    }

    fun setShadingMode(mode: ViewportShadingMode) {
        _uiState.update { it.copy(shadingMode = mode) }
    }

    fun setLightingPreset(preset: StudioLightingPreset) {
        _uiState.update { it.copy(lightingPreset = preset) }
    }

    fun toggleOrthographic() {
        _uiState.update { it.copy(isOrthographic = !it.isOrthographic) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleWireframeOverlay() {
        _uiState.update { it.copy(showWireframeOverlay = !it.showWireframeOverlay) }
    }

    fun toggleNormals() {
        _uiState.update { it.copy(showNormals = !it.showNormals) }
    }

    fun toggleVertices() {
        _uiState.update { it.copy(showVertices = !it.showVertices) }
    }

    fun toggleDimensions() {
        _uiState.update { it.copy(showDimensions = !it.showDimensions) }
    }

    fun toggleAutoTurntable() {
        _uiState.update { it.copy(autoTurntable = !it.autoTurntable) }
    }

    // Camera Controls
    fun orbitCamera(deltaYaw: Float, deltaPitch: Float) {
        _uiState.update { state ->
            state.copy(
                cameraYaw = (state.cameraYaw + deltaYaw) % 360f,
                cameraPitch = (state.cameraPitch + deltaPitch).coerceIn(-85f, 85f)
            )
        }
    }

    fun zoomAndPanCamera(zoomFactor: Float, panDx: Float, panDy: Float) {
        _uiState.update { state ->
            val nextZoom = (state.cameraZoom / zoomFactor.coerceIn(0.75f, 1.35f)).coerceIn(2.4f, 18f)
            state.copy(
                cameraZoom = nextZoom,
                cameraPanX = (state.cameraPanX + panDx * 0.5f).coerceIn(-280f, 280f),
                cameraPanY = (state.cameraPanY + panDy * 0.5f).coerceIn(-280f, 280f)
            )
        }
    }

    fun setCameraPreset(yaw: Float, pitch: Float, zoom: Float = 6.0f, ortho: Boolean? = null) {
        _uiState.update { state ->
            state.copy(
                cameraYaw = yaw,
                cameraPitch = pitch,
                cameraZoom = zoom,
                cameraPanX = 0f,
                cameraPanY = 0f,
                isOrthographic = ortho ?: state.isOrthographic
            )
        }
    }

    // Undo / Redo
    fun undo() {
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(_uiState.value.nodes)
        applySceneNodes(prev, pushHistory = false, statusMessage = "بازگشت به مرحله قبل (Undo)")
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(_uiState.value.nodes)
        applySceneNodes(next, pushHistory = false, statusMessage = "تکرار عملیات (Redo)")
    }

    // Adding Primitives & Presets
    fun addPrimitive(type: PrimitiveType3D) {
        val current = _uiState.value.nodes
        val offsetIndex = current.size % 5
        val spawnPos = if (current.isEmpty()) Vec3(0f, 0f, 0f) else Vec3((offsetIndex - 2) * 0.35f, 0.2f, 0f)
        val node = PrimitiveGenerator.createPrimitive(type, position = spawnPos)
        applySceneNodes(
            newNodes = current + node,
            selectedId = node.id,
            statusMessage = "${type.titleFa} به صحنه سه‌بعدی اضافه شد"
        )
    }

    fun loadReadyModelPreset(preset: ReadyModelPreset, appendToScene: Boolean = false) {
        val generated = preset.generator()
        val nextNodes = if (appendToScene) _uiState.value.nodes + generated else generated
        applySceneNodes(
            newNodes = nextNodes,
            selectedId = generated.firstOrNull()?.id,
            newProjectName = preset.id,
            statusMessage = "مدل آماده «${preset.titleFa}» بارگذاری شد"
        )
        _uiState.update { it.copy(activeTab = StudioTab.VIEWPORT) }
    }

    fun clearScene() {
        val starterCube = PrimitiveGenerator.createPrimitive(PrimitiveType3D.CUBE)
        applySceneNodes(
            newNodes = listOf(starterCube),
            selectedId = starterCube.id,
            newProjectName = "New_3D_Model",
            statusMessage = "صحنه جدید ایجاد شد"
        )
    }

    // Object Outliner & Transform Operations
    fun toggleNodeVisibility(nodeId: String) {
        val updated = _uiState.value.nodes.map {
            if (it.id == nodeId) it.copy(visible = !it.visible) else it
        }
        applySceneNodes(updated, pushHistory = false)
    }

    fun duplicateSelectedNode() {
        val sel = getSelectedNode() ?: return
        val copy = sel.copy(
            id = "${sel.id}_copy_${(100..999).random()}",
            name = "${sel.name} Copy",
            nameFa = "${sel.nameFa} (کپی)",
            position = sel.position + Vec3(0.45f, 0.15f, 0.35f)
        )
        applySceneNodes(
            newNodes = _uiState.value.nodes + copy,
            selectedId = copy.id,
            statusMessage = "کپی از قطعه انتخاب‌شده ایجاد شد"
        )
    }

    fun deleteSelectedNode() {
        val selId = _uiState.value.selectedNodeId ?: return
        val remaining = _uiState.value.nodes.filterNot { it.id == selId }
        applySceneNodes(
            newNodes = remaining,
            selectedId = remaining.firstOrNull()?.id,
            statusMessage = "قطعه حذف شد"
        )
    }

    fun updateSelectedTransform(
        position: Vec3? = null,
        rotation: Vec3? = null,
        scale: Vec3? = null
    ) {
        val selId = _uiState.value.selectedNodeId ?: return
        val snap = _uiState.value.snapToGrid
        val updated = _uiState.value.nodes.map { node ->
            if (node.id == selId) {
                val nextPos = position?.let { p ->
                    if (snap) Vec3(
                        (p.x * 4f).roundToInt() / 4f,
                        (p.y * 4f).roundToInt() / 4f,
                        (p.z * 4f).roundToInt() / 4f
                    ) else p
                } ?: node.position
                val nextRot = rotation?.let { r ->
                    if (snap) Vec3(
                        (r.x / 15f).roundToInt() * 15f,
                        (r.y / 15f).roundToInt() * 15f,
                        (r.z / 15f).roundToInt() * 15f
                    ) else r
                } ?: node.rotation
                val nextScale = scale?.let { s ->
                    Vec3(
                        s.x.coerceIn(0.08f, 6.0f),
                        s.y.coerceIn(0.08f, 6.0f),
                        s.z.coerceIn(0.08f, 6.0f)
                    )
                } ?: node.scale
                node.copy(position = nextPos, rotation = nextRot, scale = nextScale)
            } else node
        }
        applySceneNodes(updated, pushHistory = false)
    }

    fun transformSelectedByViewportDrag(dx: Float, dy: Float) {
        val state = _uiState.value
        val sel = getSelectedNode() ?: return
        val axis = state.axisConstraint

        when (state.toolMode) {
            TransformToolMode.MOVE -> {
                val speed = 0.0085f
                val nextPos = when (axis) {
                    AxisConstraint.X -> sel.position.copy(x = sel.position.x + dx * speed)
                    AxisConstraint.Y -> sel.position.copy(y = sel.position.y - dy * speed)
                    AxisConstraint.Z -> sel.position.copy(z = sel.position.z + (dx - dy) * speed * 0.7f)
                    AxisConstraint.ALL -> sel.position.copy(
                        x = sel.position.x + dx * speed,
                        y = sel.position.y - dy * speed
                    )
                }
                updateSelectedTransform(position = nextPos)
            }
            TransformToolMode.ROTATE -> {
                val speed = 0.45f
                val nextRot = when (axis) {
                    AxisConstraint.X -> sel.rotation.copy(x = (sel.rotation.x + dy * speed) % 360f)
                    AxisConstraint.Y -> sel.rotation.copy(y = (sel.rotation.y + dx * speed) % 360f)
                    AxisConstraint.Z -> sel.rotation.copy(z = (sel.rotation.z + dx * speed) % 360f)
                    AxisConstraint.ALL -> sel.rotation.copy(
                        x = (sel.rotation.x + dy * speed) % 360f,
                        y = (sel.rotation.y + dx * speed) % 360f
                    )
                }
                updateSelectedTransform(rotation = nextRot)
            }
            TransformToolMode.SCALE -> {
                val factor = 1f + (dx - dy) * 0.0045f
                val nextScale = when (axis) {
                    AxisConstraint.X -> sel.scale.copy(x = sel.scale.x * factor)
                    AxisConstraint.Y -> sel.scale.copy(y = sel.scale.y * factor)
                    AxisConstraint.Z -> sel.scale.copy(z = sel.scale.z * factor)
                    AxisConstraint.ALL -> sel.scale * factor
                }
                updateSelectedTransform(scale = nextScale)
            }
            TransformToolMode.SELECT_ORBIT -> Unit
        }
    }

    // Mesh Modifiers & Sculpting Operations
    fun applySubdivide(smooth: Boolean = true) {
        val sel = getSelectedNode() ?: return
        val modified = MeshModifiers.subdivideMesh(sel, smooth)
        replaceSelectedNode(modified, "تقسیم سطوح (Subdivision) انجام شد: ${modified.faces.size} مثلث")
    }

    fun applyExtrude(distance: Float = 0.32f) {
        val sel = getSelectedNode() ?: return
        val modified = MeshModifiers.extrudeTopFaces(sel, distance)
        replaceSelectedNode(modified, "اکسترود سطوح (Extrude) اعمال شد")
    }

    fun applySmooth(iterations: Int = 2) {
        val sel = getSelectedNode() ?: return
        val modified = MeshModifiers.smoothLaplacian(sel, iterations = iterations, lambda = 0.42f)
        replaceSelectedNode(modified, "نرم‌سازی لبه‌ها (Laplacian Smooth) اعمال شد")
    }

    fun applyTwist(angleDegrees: Float = 45f, axis: Axis = Axis.Y) {
        val sel = getSelectedNode() ?: return
        val modified = MeshModifiers.twistMesh(sel, angleDegrees, axis)
        replaceSelectedNode(modified, "پیچش محوری ($angleDegrees°) اعمال شد")
    }

    fun applyTaper(factor: Float = 0.62f) {
        val sel = getSelectedNode() ?: return
        val modified = MeshModifiers.taperMesh(sel, factor)
        replaceSelectedNode(modified, "مخروطی‌سازی (Taper) اعمال شد")
    }

    fun applyInflate(amount: Float = 0.10f) {
        val sel = getSelectedNode() ?: return
        val modified = MeshModifiers.inflateSculpt(sel, amount)
        val label = if (amount >= 0f) "تورم حجمی (Inflate)" else "فشرده‌سازی (Deflate)"
        replaceSelectedNode(modified, "$label روی مش اعمال شد")
    }

    fun applyVoxelDecimate() {
        val sel = getSelectedNode() ?: return
        val modified = MeshModifiers.voxelDecimate(sel, 0.24f)
        replaceSelectedNode(modified, "بهینه‌سازی پلی‌گان (Low-Poly Decimate): ${modified.faces.size} مثلث")
    }

    fun applyMirror(axis: Axis = Axis.X) {
        val sel = getSelectedNode() ?: return
        val mirrored = MeshModifiers.mirrorNode(sel, axis)
        applySceneNodes(
            newNodes = _uiState.value.nodes + mirrored,
            selectedId = mirrored.id,
            statusMessage = "قرینه محور ${axis.name} ایجاد شد"
        )
    }

    fun applyDropToFloor() {
        val sel = getSelectedNode() ?: return
        val dropped = MeshModifiers.dropToFloor(sel)
        replaceSelectedNode(dropped, "مدل روی سطح بستر پرینت سه‌بعدی (Y=0) تراز شد")
    }

    fun applyBooleanWeldAll() {
        val merged = MeshModifiers.mergeNodesIntoSingleMesh(_uiState.value.nodes) ?: return
        applySceneNodes(
            newNodes = listOf(merged),
            selectedId = merged.id,
            statusMessage = "تمامی قطعات در یک مش یکپارچه (Single Manifold Mesh) ادغام شدند"
        )
    }

    private fun replaceSelectedNode(modified: SceneNode3D, message: String) {
        val updated = _uiState.value.nodes.map { if (it.id == modified.id) modified else it }
        applySceneNodes(updated, selectedId = modified.id, statusMessage = message)
    }

    // Material Operations
    fun updateSelectedMaterial(updater: (PbrMaterial) -> PbrMaterial) {
        val selId = _uiState.value.selectedNodeId ?: return
        val updated = _uiState.value.nodes.map { node ->
            if (node.id == selId) node.copy(material = updater(node.material)) else node
        }
        applySceneNodes(updated, pushHistory = false)
    }

    fun applyMaterialPresetToSelected(preset: PbrMaterial, applyToAll: Boolean = false) {
        val selId = _uiState.value.selectedNodeId
        val updated = _uiState.value.nodes.map { node ->
            if (applyToAll || node.id == selId) node.copy(material = preset) else node
        }
        applySceneNodes(updated, statusMessage = "متریال «${preset.nameFa}» اعمال شد")
    }

    // Export & Import Operations
    fun selectExportFormat(format: ExportFormat3D) {
        val state = _uiState.value
        val payload = FileExporterImporter.generateExport(state.nodes, format, state.projectName)
        _uiState.update { it.copy(selectedExportFormat = format, activeExportPayload = payload) }
    }

    fun writeExportToUri(context: Context, targetUri: Uri) {
        val payload = _uiState.value.activeExportPayload ?: FileExporterImporter.generateExport(
            _uiState.value.nodes,
            _uiState.value.selectedExportFormat,
            _uiState.value.projectName
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(payload.bytes)
                    out.flush()
                }
                repository.recordExport(
                    ExportLogEntity(
                        fileName = payload.fileName,
                        formatName = payload.format.titleEn,
                        byteSize = payload.bytes.size,
                        vertexCount = payload.vertexCount,
                        triangleCount = payload.triangleCount
                    )
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(statusBannerMessage = "فایل «${payload.fileName}» (${payload.bytes.size / 1024} KB) با موفقیت ذخیره شد!")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(statusBannerMessage = "خطا در ذخیره فایل: ${err.localizedMessage}")
                }
            }
        }
    }

    fun shareExportFile(context: Context) {
        val payload = _uiState.value.activeExportPayload ?: FileExporterImporter.generateExport(
            _uiState.value.nodes,
            _uiState.value.selectedExportFormat,
            _uiState.value.projectName
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val outFile = File(exportDir, payload.fileName)
                outFile.writeBytes(payload.bytes)

                repository.recordExport(
                    ExportLogEntity(
                        fileName = payload.fileName,
                        formatName = payload.format.titleEn,
                        byteSize = payload.bytes.size,
                        vertexCount = payload.vertexCount,
                        triangleCount = payload.triangleCount
                    )
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outFile
                )
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = payload.format.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, payload.fileName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "اشتراک‌گذاری مدل سه‌بعدی ${payload.fileName}")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(statusBannerMessage = "خطا در اشتراک‌گذاری: ${err.localizedMessage}")
                }
            }
        }
    }

    fun importExternal3DUri(context: Context, uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@launch
                val importedNodes = FileExporterImporter.import3DFile(displayName, bytes)
                withContext(Dispatchers.Main) {
                    if (importedNodes.isNotEmpty()) {
                        applySceneNodes(
                            newNodes = _uiState.value.nodes + importedNodes,
                            selectedId = importedNodes.first().id,
                            statusMessage = "فایل سه‌بعدی «$displayName» با موفقیت وارد شد (${importedNodes.sumOf { it.faces.size }} مثلث)"
                        )
                        _uiState.update { it.copy(activeTab = StudioTab.VIEWPORT) }
                    } else {
                        _uiState.update {
                            it.copy(statusBannerMessage = "فرمت فایل قابل استخراج نبود. فرمت‌های مجاز: STL, OBJ, PLY")
                        }
                    }
                }
            }
        }
    }

    // Room Database Project Persistence
    fun saveCurrentProjectToRoom(customTitle: String? = null) {
        val state = _uiState.value
        val title = customTitle?.takeIf { it.isNotBlank() } ?: state.projectName
        val stats = state.engineeringStats
        val accent = state.nodes.firstOrNull()?.material?.baseColorHex ?: 0xFF00E5FF

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(
                ProjectEntity(
                    title = title,
                    description = "${stats.objectCount} قطعه • ${stats.totalTriangles} مثلث • ${stats.widthMm.roundToInt()}×${stats.heightMm.roundToInt()} mm",
                    sceneJson = serializeNodesToJson(state.nodes),
                    objectCount = stats.objectCount,
                    vertexCount = stats.totalVertices,
                    triangleCount = stats.totalTriangles,
                    accentHex = accent
                )
            )
            _uiState.update {
                it.copy(
                    projectName = title,
                    statusBannerMessage = "پروژه «$title» در پایگاه داده ذخیره شد"
                )
            }
        }
    }

    fun loadProjectFromRoom(entity: ProjectEntity) {
        val loadedNodes = deserializeNodesFromJson(entity.sceneJson)
        if (loadedNodes.isNotEmpty()) {
            applySceneNodes(
                newNodes = loadedNodes,
                selectedId = loadedNodes.first().id,
                newProjectName = entity.title,
                statusMessage = "پروژه «${entity.title}» بارگذاری شد"
            )
            _uiState.update { it.copy(activeTab = StudioTab.VIEWPORT) }
        }
    }

    fun deleteProjectFromRoom(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(id)
        }
    }

    fun getSelectedNode(): SceneNode3D? {
        val id = _uiState.value.selectedNodeId ?: return null
        return _uiState.value.nodes.firstOrNull { it.id == id }
    }

    private fun serializeNodesToJson(nodes: List<SceneNode3D>): String {
        val arr = JSONArray()
        nodes.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    private fun deserializeNodesFromJson(jsonStr: String): List<SceneNode3D> {
        return runCatching {
            val arr = JSONArray(jsonStr)
            val list = ArrayList<SceneNode3D>(arr.length())
            for (i in 0 until arr.length()) {
                list.add(SceneNode3D.fromJson(arr.getJSONObject(i)))
            }
            list
        }.getOrDefault(emptyList())
    }
}
