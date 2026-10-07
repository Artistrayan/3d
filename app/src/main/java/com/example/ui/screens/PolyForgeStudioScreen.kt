package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.PrimitiveType3D
import com.example.model.AxisConstraint
import com.example.model.SceneNode3D
import com.example.model.TransformToolMode
import com.example.model.Vec3
import com.example.ui.theme.AxisBlueZ
import com.example.ui.theme.AxisGreenY
import com.example.ui.theme.AxisRedX
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ManifoldEmerald
import com.example.ui.theme.SculptAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.viewport.Viewport3DCanvas
import com.example.viewmodel.StudioTab
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import java.util.Locale

@Composable
fun PolyForgeStudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val recentExports by viewModel.recentExports.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = uiState.activeTab != StudioTab.VIEWPORT) {
        viewModel.selectTab(StudioTab.VIEWPORT)
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.writeExportToUri(context, uri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var fileName = "imported_model.stl"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            viewModel.importExternal3DUri(context, uri, fileName)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = StudioObsidian,
        topBar = {
            StudioTopHudBar(
                uiState = uiState,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onToggleLanguage = viewModel::toggleLanguage,
                onQuickTextureClick = { viewModel.selectTab(StudioTab.MATERIAL_LIGHT) },
                onQuickExportClick = { viewModel.selectTab(StudioTab.EXPORT_PROJECTS) }
            )
        },
        bottomBar = {
            StudioBottomNavigationBar(
                activeTab = uiState.activeTab,
                isPersian = uiState.isPersian,
                onSelectTab = viewModel::selectTab
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 720.dp
            val selectedNode = uiState.nodes.firstOrNull { it.id == uiState.selectedNodeId }

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                    uiState.statusBannerMessage?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF083344))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::clearStatusMessage,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1.25f).fillMaxHeight()) {
                            InteractiveViewportWorkspace(
                                uiState = uiState,
                                selectedNode = selectedNode,
                                viewModel = viewModel
                            )
                        }
                        if (uiState.activeTab != StudioTab.VIEWPORT) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(StudioSurface)
                            ) {
                                ActiveSecondaryTabContent(
                                    uiState = uiState,
                                    selectedNode = selectedNode,
                                    savedProjects = savedProjects,
                                    recentExports = recentExports,
                                    viewModel = viewModel,
                                    context = context,
                                    onLaunchSaveFile = { fileName -> saveDocumentLauncher.launch(fileName) },
                                    onLaunchShareFile = { viewModel.shareExportFile(context) },
                                    onLaunchImportFile = { openDocumentLauncher.launch(arrayOf("*/*")) }
                                )
                            }
                        }
                    }
                } else {
                    if (uiState.activeTab == StudioTab.VIEWPORT) {
                        InteractiveViewportWorkspace(
                            uiState = uiState,
                            selectedNode = selectedNode,
                            viewModel = viewModel
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .border(1.dp, StudioBorder)
                            ) {
                                Viewport3DCanvas(
                                    nodes = uiState.nodes,
                                    selectedNodeId = uiState.selectedNodeId,
                                    shadingMode = uiState.shadingMode,
                                    lightingPreset = uiState.lightingPreset,
                                    toolMode = uiState.toolMode,
                                    axisConstraint = uiState.axisConstraint,
                                    isOrthographic = uiState.isOrthographic,
                                    showGrid = uiState.showGrid,
                                    showWireframeOverlay = uiState.showWireframeOverlay,
                                    showNormals = uiState.showNormals,
                                    showVertices = uiState.showVertices,
                                    showDimensions = false,
                                    autoTurntable = uiState.autoTurntable,
                                    cameraYaw = uiState.cameraYaw,
                                    cameraPitch = uiState.cameraPitch,
                                    cameraZoom = uiState.cameraZoom,
                                    cameraPanX = uiState.cameraPanX,
                                    cameraPanY = uiState.cameraPanY,
                                    engineeringStats = uiState.engineeringStats,
                                    onOrbitCamera = viewModel::orbitCamera,
                                    onZoomPanCamera = viewModel::zoomAndPanCamera,
                                    onTransformSelectedByDrag = viewModel::transformSelectedByViewportDrag,
                                    onSelectNode = viewModel::selectNode
                                )
                                Surface(
                                    color = StudioSurface.copy(alpha = 0.82f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isPersian)
                                            "پیش‌نمایش زنده 3D (${uiState.engineeringStats.totalTriangles} مثلث)"
                                        else
                                            "Live 3D Preview (${uiState.engineeringStats.totalTriangles} Tris)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(StudioSurface)
                            ) {
                                ActiveSecondaryTabContent(
                                    uiState = uiState,
                                    selectedNode = selectedNode,
                                    savedProjects = savedProjects,
                                    recentExports = recentExports,
                                    viewModel = viewModel,
                                    context = context,
                                    onLaunchSaveFile = { fileName -> saveDocumentLauncher.launch(fileName) },
                                    onLaunchShareFile = { viewModel.shareExportFile(context) },
                                    onLaunchImportFile = { openDocumentLauncher.launch(arrayOf("*/*")) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSecondaryTabContent(
    uiState: StudioUiState,
    selectedNode: SceneNode3D?,
    savedProjects: List<com.example.data.ProjectEntity>,
    recentExports: List<com.example.data.ExportLogEntity>,
    viewModel: StudioViewModel,
    context: android.content.Context,
    onLaunchSaveFile: (String) -> Unit,
    onLaunchShareFile: () -> Unit,
    onLaunchImportFile: () -> Unit
) {
    when (uiState.activeTab) {
        StudioTab.LIBRARY -> {
            LibraryPanel(
                isPersian = uiState.isPersian,
                onAddPrimitive = { prim ->
                    viewModel.addPrimitive(prim, appendToScene = false)
                    viewModel.selectTab(StudioTab.VIEWPORT)
                },
                onLoadPreset = viewModel::loadReadyModelPreset
            )
        }
        StudioTab.SCULPT_MESH -> {
            SculptAndModifiersPanel(
                isPersian = uiState.isPersian,
                selectedNode = selectedNode,
                engineeringStats = uiState.engineeringStats,
                onSubdivide = viewModel::applySubdivide,
                onExtrude = viewModel::applyExtrude,
                onSmooth = { viewModel.applySmooth(2) },
                onTwist = viewModel::applyTwist,
                onTaper = viewModel::applyTaper,
                onInflate = viewModel::applyInflate,
                onVoxelDecimate = viewModel::applyVoxelDecimate,
                onMirror = viewModel::applyMirror,
                onDropToFloor = viewModel::applyDropToFloor,
                onBooleanWeldAll = viewModel::applyBooleanWeldAll,
                onDuplicate = viewModel::duplicateSelectedNode
            )
        }
        StudioTab.MATERIAL_LIGHT -> {
            MaterialAndLightPanel(
                isPersian = uiState.isPersian,
                selectedNode = selectedNode,
                shadingMode = uiState.shadingMode,
                lightingPreset = uiState.lightingPreset,
                onSetShadingMode = viewModel::setShadingMode,
                onSetLightingPreset = viewModel::setLightingPreset,
                onUpdateMaterial = viewModel::updateSelectedMaterial,
                onApplyPresetMaterial = viewModel::applyMaterialPresetToSelected,
                onSelectTextureType = viewModel::selectTextureType,
                onAddPaintStroke = viewModel::addPaintStrokeToSelected,
                onClearPaintStrokes = viewModel::clearPaintStrokesOnSelected,
                onRandomizeTexture = viewModel::randomizeProceduralTexture,
                onExportTexturePng = { viewModel.shareCurrentTextureAsPng(context) }
            )
        }
        StudioTab.EXPORT_PROJECTS -> {
            ExportAndProjectsPanel(
                isPersian = uiState.isPersian,
                projectName = uiState.projectName,
                selectedFormat = uiState.selectedExportFormat,
                exportPayload = uiState.activeExportPayload,
                savedProjects = savedProjects,
                recentExports = recentExports,
                onUpdateProjectName = viewModel::updateProjectName,
                onSelectFormat = viewModel::selectExportFormat,
                onRequestSaveFileToDevice = { _, fileName -> onLaunchSaveFile(fileName) },
                onShareExportFile = onLaunchShareFile,
                onRequestImportFile = onLaunchImportFile,
                onSaveProjectToRoom = { viewModel.saveCurrentProjectToRoom() },
                onLoadProjectFromRoom = viewModel::loadProjectFromRoom,
                onDeleteProjectFromRoom = viewModel::deleteProjectFromRoom
            )
        }
        StudioTab.VIEWPORT -> Unit
    }
}

@Composable
private fun StudioTopHudBar(
    uiState: StudioUiState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleLanguage: () -> Unit,
    onQuickTextureClick: () -> Unit,
    onQuickExportClick: () -> Unit
) {
    Surface(
        color = StudioSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.18f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "PolyForge 3D",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PolyForge 3D",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%d Model • %d Verts • %d Tris",
                            uiState.engineeringStats.objectCount,
                            uiState.engineeringStats.totalVertices,
                            uiState.engineeringStats.totalTriangles
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = uiState.canUndo,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (uiState.canUndo) Color.White else TextSecondary.copy(alpha = 0.35f)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = uiState.canRedo,
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (uiState.canRedo) Color.White else TextSecondary.copy(alpha = 0.35f)
                    )
                }

                // Quick Texture Maker Button
                Surface(
                    color = SculptAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, SculptAmber, RoundedCornerShape(10.dp))
                        .clickable(onClick = onQuickTextureClick)
                        .testTag("quick_texture_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Texture Studio",
                            tint = SculptAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isPersian) "ساخت تکسچر" else "Texture",
                            style = MaterialTheme.typography.labelMedium,
                            color = SculptAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("language_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Switch Language",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    color = CyberCyan,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clickable(onClick = onQuickExportClick)
                        .testTag("quick_export_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export GLB/STL",
                            tint = Color(0xFF00242B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GLB / STL",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF00242B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveViewportWorkspace(
    uiState: StudioUiState,
    selectedNode: SceneNode3D?,
    viewModel: StudioViewModel
) {
    var inspectorExpanded by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Main Real-Time 3D Viewport Canvas
        Viewport3DCanvas(
            nodes = uiState.nodes,
            selectedNodeId = uiState.selectedNodeId,
            shadingMode = uiState.shadingMode,
            lightingPreset = uiState.lightingPreset,
            toolMode = uiState.toolMode,
            axisConstraint = uiState.axisConstraint,
            isOrthographic = uiState.isOrthographic,
            showGrid = uiState.showGrid,
            showWireframeOverlay = uiState.showWireframeOverlay,
            showNormals = uiState.showNormals,
            showVertices = uiState.showVertices,
            showDimensions = uiState.showDimensions,
            autoTurntable = uiState.autoTurntable,
            cameraYaw = uiState.cameraYaw,
            cameraPitch = uiState.cameraPitch,
            cameraZoom = uiState.cameraZoom,
            cameraPanX = uiState.cameraPanX,
            cameraPanY = uiState.cameraPanY,
            engineeringStats = uiState.engineeringStats,
            onOrbitCamera = viewModel::orbitCamera,
            onZoomPanCamera = viewModel::zoomAndPanCamera,
            onTransformSelectedByDrag = viewModel::transformSelectedByViewportDrag,
            onSelectNode = viewModel::selectNode,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Floating Camera View & Clean Workspace Pill Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 10.dp, top = 10.dp, end = 96.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1-Tap Clear Workspace button so the user can empty the CAD table anytime!
            Surface(
                color = Color(0xFF450A0A).copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .border(1.dp, AxisRedX, RoundedCornerShape(10.dp))
                    .clickable(onClick = viewModel::clearWorkspaceToEmpty)
                    .testTag("clear_workspace_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Workspace",
                        tint = AxisRedX,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.isPersian) "خلوت کردن میز کار" else "Clear Scene",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }

            HudChip(
                label = if (uiState.isOrthographic) "ORTHO" else "PERSP",
                active = uiState.isOrthographic,
                onClick = viewModel::toggleOrthographic,
                tag = "cam_ortho_toggle"
            )
            HudChip(
                label = if (uiState.isPersian) "ایزومتریک" else "Iso",
                active = false,
                onClick = { viewModel.setCameraPreset(-35f, 25f, 5.4f) },
                tag = "cam_preset_iso"
            )
            HudChip(
                label = if (uiState.isPersian) "روبرو" else "Front",
                active = false,
                onClick = { viewModel.setCameraPreset(0f, 0f, 5.2f) },
                tag = "cam_preset_front"
            )
            HudChip(
                label = if (uiState.isPersian) "بالا" else "Top",
                active = false,
                onClick = { viewModel.setCameraPreset(0f, 85f, 5.8f) },
                tag = "cam_preset_top"
            )
            HudChip(
                label = if (uiState.isPersian) "مش سیمی" else "Wire",
                active = uiState.showWireframeOverlay,
                onClick = viewModel::toggleWireframeOverlay,
                tag = "toggle_wireframe_chip"
            )
            HudChip(
                label = if (uiState.isPersian) "ابعاد mm" else "Dims mm",
                active = uiState.showDimensions,
                onClick = viewModel::toggleDimensions,
                tag = "toggle_dims_chip"
            )
            HudChip(
                label = if (uiState.isPersian) "نرمال‌ها" else "Normals",
                active = uiState.showNormals,
                onClick = viewModel::toggleNormals,
                tag = "toggle_normals_chip"
            )
            HudChip(
                label = if (uiState.isPersian) "چرخش ۳۶۰°" else "Turntable",
                active = uiState.autoTurntable,
                onClick = viewModel::toggleAutoTurntable,
                tag = "toggle_turntable_chip"
            )
        }

        // 3. Left Floating 3D Transform Tool & Axis Constraint Dock
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioSurface.copy(alpha = 0.88f))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ToolDockIconButton(
                icon = Icons.Default.CenterFocusStrong,
                label = "Orbit",
                selected = uiState.toolMode == TransformToolMode.SELECT_ORBIT,
                onClick = { viewModel.setToolMode(TransformToolMode.SELECT_ORBIT) },
                tag = "tool_orbit"
            )
            ToolDockIconButton(
                icon = Icons.Default.OpenWith,
                label = "Move",
                selected = uiState.toolMode == TransformToolMode.MOVE,
                onClick = { viewModel.setToolMode(TransformToolMode.MOVE) },
                tag = "tool_move"
            )
            ToolDockIconButton(
                icon = Icons.Default.ControlCamera,
                label = "Rotate",
                selected = uiState.toolMode == TransformToolMode.ROTATE,
                onClick = { viewModel.setToolMode(TransformToolMode.ROTATE) },
                tag = "tool_rotate"
            )
            ToolDockIconButton(
                icon = Icons.Default.ZoomOutMap,
                label = "Scale",
                selected = uiState.toolMode == TransformToolMode.SCALE,
                onClick = { viewModel.setToolMode(TransformToolMode.SCALE) },
                tag = "tool_scale"
            )

            AxisPillButton("XYZ", AxisConstraint.ALL, uiState.axisConstraint, CyberCyan) {
                viewModel.setAxisConstraint(AxisConstraint.ALL)
            }
            AxisPillButton("X", AxisConstraint.X, uiState.axisConstraint, AxisRedX) {
                viewModel.setAxisConstraint(AxisConstraint.X)
            }
            AxisPillButton("Y", AxisConstraint.Y, uiState.axisConstraint, AxisGreenY) {
                viewModel.setAxisConstraint(AxisConstraint.Y)
            }
            AxisPillButton("Z", AxisConstraint.Z, uiState.axisConstraint, AxisBlueZ) {
                viewModel.setAxisConstraint(AxisConstraint.Z)
            }
        }

        // 4. Bottom Collapsible Single-Model Switcher + Precision CAD Transform Inspector
        Surface(
            color = StudioSurface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(
                    1.dp,
                    StudioBorder,
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clean Single-Shape Switcher Pills (replaces active shape so workspace stays clean!)
                        val quickPrims = listOf(
                            PrimitiveType3D.CUBE to (if (uiState.isPersian) "مکعب" else "Cube"),
                            PrimitiveType3D.SPHERE to (if (uiState.isPersian) "کره" else "Sphere"),
                            PrimitiveType3D.CYLINDER to (if (uiState.isPersian) "استوانه" else "Cylinder"),
                            PrimitiveType3D.GEAR to (if (uiState.isPersian) "چرخ‌دنده" else "Gear"),
                            PrimitiveType3D.TWISTED_VASE to (if (uiState.isPersian) "گلدان" else "Vase"),
                            PrimitiveType3D.DIAMOND_GEM to (if (uiState.isPersian) "الماس" else "Gem")
                        )
                        quickPrims.forEach { (primType, label) ->
                            val isActiveType = selectedNode?.typeTag == primType.name && uiState.nodes.size == 1
                            Surface(
                                color = if (isActiveType) CyberCyan.copy(alpha = 0.24f) else StudioSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (isActiveType) CyberCyan else StudioBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.addPrimitive(primType, appendToScene = false) }
                                    .testTag("quick_switch_${primType.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isActiveType) CyberCyan else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Scene Node Chips if user added multiple objects
                        if (uiState.nodes.size > 1) {
                            uiState.nodes.forEach { node ->
                                val isSel = node.id == uiState.selectedNodeId
                                Surface(
                                    color = if (isSel) Color(0xFF083344) else StudioSurfaceElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (isSel) CyberCyan else StudioBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.selectNode(node.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(node.material.baseColorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (uiState.isPersian) node.nameFa else node.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSel) CyberCyan else Color.White,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (node.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle visibility",
                                            tint = if (node.visible) TextSecondary else Color(0xFFEF4444),
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clickable { viewModel.toggleNodeVisibility(node.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { inspectorExpanded = !inspectorExpanded },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("toggle_inspector_drawer_btn")
                    ) {
                        Icon(
                            imageVector = if (inspectorExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Toggle Transform Inspector",
                            tint = Color.White
                        )
                    }
                }

                AnimatedVisibility(visible = inspectorExpanded && selectedNode != null) {
                    if (selectedNode != null) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (uiState.isPersian)
                                        "${selectedNode.nameFa} (${selectedNode.faces.size} مثلث)"
                                    else
                                        "${selectedNode.name} (${selectedNode.faces.size} Tris)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = SculptAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { viewModel.selectTab(StudioTab.MATERIAL_LIGHT) }
                                            .testTag("inspector_texture_btn")
                                    ) {
                                        Text(
                                            text = if (uiState.isPersian) "تکسچر+" else "+Texture",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SculptAmber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }

                                    Surface(
                                        color = StudioSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { viewModel.applySubdivide(true) }
                                            .testTag("quick_subdivide_btn")
                                    ) {
                                        Text(
                                            text = "Subdivide+",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }

                                    Surface(
                                        color = StudioSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { viewModel.duplicateSelectedNode() }
                                            .testTag("quick_duplicate_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate",
                                            tint = Color.White,
                                            modifier = Modifier.padding(5.dp).size(15.dp)
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFF450A0A),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { viewModel.deleteSelectedNode() }
                                            .testTag("quick_delete_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = AxisRedX,
                                            modifier = Modifier.padding(5.dp).size(15.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val activeMode = if (uiState.toolMode == TransformToolMode.SELECT_ORBIT) {
                                TransformToolMode.MOVE
                            } else uiState.toolMode

                            when (activeMode) {
                                TransformToolMode.MOVE, TransformToolMode.SELECT_ORBIT -> {
                                    TransformTripletSliders(
                                        prefix = if (uiState.isPersian) "مکان (Pos)" else "Pos",
                                        vec = selectedNode.position,
                                        range = -3.5f..3.5f,
                                        unitSuffix = "u",
                                        onChanged = { viewModel.updateSelectedTransform(position = it) }
                                    )
                                }
                                TransformToolMode.ROTATE -> {
                                    TransformTripletSliders(
                                        prefix = if (uiState.isPersian) "چرخش (Rot)" else "Rot",
                                        vec = selectedNode.rotation,
                                        range = -180f..180f,
                                        unitSuffix = "°",
                                        onChanged = { viewModel.updateSelectedTransform(rotation = it) }
                                    )
                                }
                                TransformToolMode.SCALE -> {
                                    TransformTripletSliders(
                                        prefix = if (uiState.isPersian) "مقیاس (Scale)" else "Scale",
                                        vec = selectedNode.scale,
                                        range = 0.15f..3.2f,
                                        unitSuffix = "x",
                                        onChanged = { viewModel.updateSelectedTransform(scale = it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransformTripletSliders(
    prefix: String,
    vec: Vec3,
    range: ClosedFloatingPointRange<Float>,
    unitSuffix: String,
    onChanged: (Vec3) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactAxisSlider(
            label = "$prefix X",
            value = vec.x,
            range = range,
            unit = unitSuffix,
            color = AxisRedX,
            onValueChange = { onChanged(vec.copy(x = it)) },
            modifier = Modifier.weight(1f)
        )
        CompactAxisSlider(
            label = "Y",
            value = vec.y,
            range = range,
            unit = unitSuffix,
            color = AxisGreenY,
            onValueChange = { onChanged(vec.copy(y = it)) },
            modifier = Modifier.weight(1f)
        )
        CompactAxisSlider(
            label = "Z",
            value = vec.z,
            range = range,
            unit = unitSuffix,
            color = AxisBlueZ,
            onValueChange = { onChanged(vec.copy(z = it)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactAxisSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                text = String.format(Locale.US, "%.1f%s", value, unit),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}

@Composable
private fun HudChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = if (active) CyberCyan.copy(alpha = 0.25f) else StudioSurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .border(
                1.dp,
                if (active) CyberCyan else StudioBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) CyberCyan else Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ToolDockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CyberCyan else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF00242B) else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AxisPillButton(
    label: String,
    axis: AxisConstraint,
    current: AxisConstraint,
    color: Color,
    onClick: () -> Unit
) {
    val selected = axis == current
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.28f) else StudioSurfaceElevated)
            .border(
                1.dp,
                if (selected) color else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StudioBottomNavigationBar(
    activeTab: StudioTab,
    isPersian: Boolean,
    onSelectTab: (StudioTab) -> Unit
) {
    NavigationBar(
        containerColor = StudioSurface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(StudioTab.VIEWPORT, Icons.Default.ViewInAr, "nav_tab_viewport"),
            Triple(StudioTab.LIBRARY, Icons.Default.Layers, "nav_tab_library"),
            Triple(StudioTab.SCULPT_MESH, Icons.Default.AutoFixHigh, "nav_tab_sculpt"),
            Triple(StudioTab.MATERIAL_LIGHT, Icons.Default.Brush, "nav_tab_material"),
            Triple(StudioTab.EXPORT_PROJECTS, Icons.Default.IosShare, "nav_tab_export")
        )
        for ((tab, icon, tag) in items) {
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.titleEn
                    )
                },
                label = {
                    Text(
                        text = if (isPersian) tab.titleFa else tab.titleEn,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00242B),
                    selectedTextColor = CyberCyan,
                    indicatorColor = CyberCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag(tag)
            )
        }
    }
}
