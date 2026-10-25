package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.example.model.ExportFormat3D
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

    var isFullScreenImmersive by remember { mutableStateOf(false) }
    var inspectorExpanded by remember { mutableStateOf(false) }
    var sheetMaximized by remember { mutableStateOf(false) }
    var showWebStudioPreview by remember { mutableStateOf(false) }
    var showQuickImportDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.activeTab != StudioTab.VIEWPORT || isFullScreenImmersive) {
        if (isFullScreenImmersive) {
            isFullScreenImmersive = false
        } else {
            viewModel.selectTab(StudioTab.VIEWPORT)
        }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.writeExportToUri(context, uri)
        }
    }

    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) {
            viewModel.writeInstalledApkToUri(context, uri)
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

    val photoTextureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            var fileName = "gallery_photo.jpg"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            viewModel.importPhotoAsTextureUri(context, uri, fileName)
        }
    }

    val photoTo3DReliefLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            var fileName = "photo_relief.jpg"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            viewModel.importPhotoAs3DReliefUri(context, uri, fileName)
        }
    }

    val selectedNode = uiState.nodes.firstOrNull { it.id == uiState.selectedNodeId }
    val baseDensity = androidx.compose.ui.platform.LocalDensity.current
    val fixedStudioDensity = remember(baseDensity.density) {
        androidx.compose.ui.unit.Density(density = baseDensity.density, fontScale = 1f)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
        androidx.compose.ui.platform.LocalDensity provides fixedStudioDensity
    ) {
        // Structured, Non-Overlapping Column Layout so no bars or panels ever collide
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(StudioObsidian)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // 1. Clean Top Header Bar (Hidden in Immersive Full-Screen mode)
            if (!isFullScreenImmersive) {
                StudioTopHeaderBar(
                    uiState = uiState,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onEnterFullScreen = {
                        viewModel.selectTab(StudioTab.VIEWPORT)
                        isFullScreenImmersive = true
                    },
                    onQuickImportClick = { showQuickImportDialog = true },
                    onQuickPhotoClick = { showQuickImportDialog = true },
                    onQuickWebClick = { showWebStudioPreview = true },
                    onQuickTextureClick = { viewModel.selectTab(StudioTab.MATERIAL_LIGHT) },
                    onQuickExportClick = { viewModel.selectTab(StudioTab.EXPORT_PROJECTS) }
                )

                // Status toast banner if present
                AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                    uiState.statusBannerMessage?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF083344))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::clearStatusMessage,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Main Middle Workspace Area (Zero overlap with top or bottom navigation bars)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.activeTab == StudioTab.VIEWPORT) {
                    // Full 3D Viewport Canvas
                    Viewport3DCanvas(
                        nodes = uiState.nodes,
                        selectedNodeId = uiState.selectedNodeId,
                        shadingMode = uiState.shadingMode,
                        lightingPreset = uiState.lightingPreset,
                        lightIntensity = uiState.lightIntensity,
                        ambientLightColorHex = uiState.ambientLightColorHex,
                        shadowSoftness = uiState.shadowSoftness,
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

                    if (isFullScreenImmersive) {
                        // Single minimal button in top-left corner to exit Full-Screen mode
                        Surface(
                            color = StudioSurface.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .border(1.dp, CyberCyan, RoundedCornerShape(10.dp))
                                .clickable { isFullScreenImmersive = false }
                                .testTag("exit_fullscreen_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isPersian) "نمایش نوار ابزار" else "Show UI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Top Single Scrollable Strip of Camera & View Presets (Never wraps or crowds)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = CyberCyan,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { showQuickImportDialog = true }
                                    .testTag("viewport_import_file_photo_chip")
                            ) {
                                Text(
                                    text = if (uiState.isPersian) "📂 فایل / 🖼️ عکس" else "📂 File / Photo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00242B),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
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
                                onClick = { viewModel.setCameraPreset(-35f, 25f, 5.0f) },
                                tag = "cam_preset_iso"
                            )
                            HudChip(
                                label = if (uiState.isPersian) "روبرو" else "Front",
                                active = false,
                                onClick = { viewModel.setCameraPreset(0f, 0f, 5.0f) },
                                tag = "cam_preset_front"
                            )
                            HudChip(
                                label = if (uiState.isPersian) "بالا" else "Top",
                                active = false,
                                onClick = { viewModel.setCameraPreset(0f, 85f, 5.5f) },
                                tag = "cam_preset_top"
                            )
                            HudChip(
                                label = if (uiState.isPersian) "پهلو" else "Side",
                                active = false,
                                onClick = { viewModel.setCameraPreset(90f, 0f, 5.0f) },
                                tag = "cam_preset_side"
                            )
                            HudChip(
                                label = if (uiState.isPersian) "مش سیمی" else "Wire",
                                active = uiState.showWireframeOverlay,
                                onClick = viewModel::toggleWireframeOverlay,
                                tag = "toggle_wireframe_chip"
                            )
                            HudChip(
                                label = if (uiState.isPersian) "رئوس" else "Verts",
                                active = uiState.showVertices,
                                onClick = viewModel::toggleVertices,
                                tag = "toggle_vertices_chip"
                            )
                            HudChip(
                                label = if (uiState.isPersian) "ابعاد mm" else "Dims mm",
                                active = uiState.showDimensions,
                                onClick = viewModel::toggleDimensions,
                                tag = "toggle_dims_chip"
                            )
                            HudChip(
                                label = if (uiState.isPersian) "چرخش ۳۶۰°" else "360°",
                                active = uiState.autoTurntable,
                                onClick = viewModel::toggleAutoTurntable,
                                tag = "toggle_turntable_chip"
                            )
                            Surface(
                                color = Color(0xCC450A0A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, AxisRedX.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .clickable(onClick = viewModel::clearWorkspaceToEmpty)
                                    .testTag("clear_workspace_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear Workspace",
                                        tint = AxisRedX,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (uiState.isPersian) "خلوت کردن" else "Clear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }
                        }

                        // Bottom Unified Control Dock (Two spacious non-cramped rows so buttons never squish on any phone)
                        Surface(
                            color = StudioSurface.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .border(1.dp, StudioBorder.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Row 1: 4 Transform Tool Buttons + Axis Constraint Pills + XYZ Numeric Sliders Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
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
                                    }

                                    if (uiState.toolMode != TransformToolMode.SELECT_ORBIT) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 6.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    // Toggle button for exact X/Y/Z numeric sliders
                                    Surface(
                                        color = if (inspectorExpanded) CyberCyan.copy(alpha = 0.22f) else StudioSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                if (inspectorExpanded) CyberCyan else StudioBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { inspectorExpanded = !inspectorExpanded }
                                            .testTag("toggle_inspector_drawer_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Toggle XYZ Sliders",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (uiState.isPersian) "ابعاد X/Y/Z" else "XYZ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyberCyan,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                // Row 2: Dedicated Full-Width Quick Shapes Strip (Never squished on narrow screens)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val quickPrims = listOf(
                                        PrimitiveType3D.CUBE to (if (uiState.isPersian) "مکعب" else "Cube"),
                                        PrimitiveType3D.SPHERE to (if (uiState.isPersian) "کره" else "Sphere"),
                                        PrimitiveType3D.CYLINDER to (if (uiState.isPersian) "استوانه" else "Cylinder"),
                                        PrimitiveType3D.GEAR to (if (uiState.isPersian) "چرخ‌دنده" else "Gear"),
                                        PrimitiveType3D.TWISTED_VASE to (if (uiState.isPersian) "گلدان" else "Vase"),
                                        PrimitiveType3D.DIAMOND_GEM to (if (uiState.isPersian) "الماس" else "Gem"),
                                        PrimitiveType3D.TORUS to (if (uiState.isPersian) "حلقه" else "Torus"),
                                        PrimitiveType3D.CONE to (if (uiState.isPersian) "مخروط" else "Cone"),
                                        PrimitiveType3D.CAPSULE to (if (uiState.isPersian) "کپسول" else "Capsule"),
                                        PrimitiveType3D.PYRAMID to (if (uiState.isPersian) "هرم" else "Pyramid")
                                    )
                                    quickPrims.forEach { (primType, label) ->
                                        val isActiveType = selectedNode?.typeTag == primType.name && uiState.nodes.size == 1
                                        Surface(
                                            color = if (isActiveType) CyberCyan.copy(alpha = 0.22f) else StudioSurfaceElevated,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (isActiveType) CyberCyan else StudioBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { viewModel.addPrimitive(primType, appendToScene = false) }
                                                .testTag("quick_switch_${primType.name.lowercase()}")
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isActiveType) CyberCyan else Color.White,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                            // Expandable Precision X/Y/Z Sliders Drawer
                            AnimatedVisibility(visible = inspectorExpanded && selectedNode != null) {
                                if (selectedNode != null) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                        HorizontalDivider(color = StudioBorder.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.height(6.dp))
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
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                                        modifier = Modifier.padding(4.dp).size(14.dp)
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
                                                        modifier = Modifier.padding(4.dp).size(14.dp)
                                                    )
                                                }
                                            }
                                        }

                                        val activeMode = if (uiState.toolMode == TransformToolMode.SELECT_ORBIT) {
                                            TransformToolMode.MOVE
                                        } else uiState.toolMode

                                        when (activeMode) {
                                            TransformToolMode.MOVE, TransformToolMode.SELECT_ORBIT -> {
                                                TransformTripletSliders(
                                                    prefix = if (uiState.isPersian) "مکان" else "Pos",
                                                    vec = selectedNode.position,
                                                    range = -3.5f..3.5f,
                                                    unitSuffix = "u",
                                                    onChanged = { viewModel.updateSelectedTransform(position = it) }
                                                )
                                            }
                                            TransformToolMode.ROTATE -> {
                                                TransformTripletSliders(
                                                    prefix = if (uiState.isPersian) "چرخش" else "Rot",
                                                    vec = selectedNode.rotation,
                                                    range = -180f..180f,
                                                    unitSuffix = "°",
                                                    onChanged = { viewModel.updateSelectedTransform(rotation = it) }
                                                )
                                            }
                                            TransformToolMode.SCALE -> {
                                                TransformTripletSliders(
                                                    prefix = if (uiState.isPersian) "مقیاس" else "Scale",
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
            } else {
                // Secondary Tab View: Clean Split Layout (Live 3D Preview on top + Dedicated Panel below, ZERO overlap)
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!sheetMaximized) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.36f)
                        ) {
                            Viewport3DCanvas(
                                nodes = uiState.nodes,
                                selectedNodeId = uiState.selectedNodeId,
                                shadingMode = uiState.shadingMode,
                                lightingPreset = uiState.lightingPreset,
                                lightIntensity = uiState.lightIntensity,
                                ambientLightColorHex = uiState.ambientLightColorHex,
                                shadowSoftness = uiState.shadowSoftness,
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
                        }
                    }

                    // Active Tab Panel Card
                    Surface(
                        color = StudioSurface,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (sheetMaximized) 1f else 0.64f)
                            .border(
                                1.dp,
                                CyberCyan.copy(alpha = 0.35f),
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Panel Header Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StudioSurfaceElevated)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (uiState.isPersian) uiState.activeTab.titleFa else uiState.activeTab.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = StudioSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { sheetMaximized = !sheetMaximized }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (sheetMaximized) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                contentDescription = "Resize panel",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (sheetMaximized)
                                                    (if (uiState.isPersian) "پیش‌نمایش 3D" else "Show 3D")
                                                else
                                                    (if (uiState.isPersian) "تمام‌صفحه پنل" else "Expand"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Surface(
                                        color = CyberCyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { viewModel.selectTab(StudioTab.VIEWPORT) }
                                            .testTag("close_secondary_sheet_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Back to 3D Workspace",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (uiState.isPersian) "میز کار 3D" else "3D View",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyberCyan
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                ActiveSecondaryTabContent(
                                    uiState = uiState,
                                    selectedNode = selectedNode,
                                    savedProjects = savedProjects,
                                    recentExports = recentExports,
                                    viewModel = viewModel,
                                    context = context,
                                    onLaunchSaveFile = { fileName -> saveDocumentLauncher.launch(fileName) },
                                    onLaunchShareFile = { viewModel.shareExportFile(context) },
                                    onLaunchImportFile = { openDocumentLauncher.launch(arrayOf("*/*")) },
                                    onLaunchImportPhotoTexture = {
                                        photoTextureLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onLaunchImportPhotoTo3D = {
                                        photoTo3DReliefLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onOpenLiveWebStudio = { showWebStudioPreview = true },
                                    onShareStandaloneWebHtml = { viewModel.shareStandaloneWebHtml(context) },
                                    onSaveAndroidApkFile = { apkFileName -> saveApkLauncher.launch(apkFileName) },
                                    onShareAndroidApkFile = { viewModel.shareInstalledApk(context) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom Navigation Bar (Hidden in Immersive Full-Screen mode)
        if (!isFullScreenImmersive) {
            StudioBottomNavigationBar(
                activeTab = uiState.activeTab,
                isPersian = uiState.isPersian,
                onSelectTab = viewModel::selectTab
            )
        }
    }

    if (showQuickImportDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showQuickImportDialog = false }) {
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CyberCyan, RoundedCornerShape(20.dp))
                    .testTag("quick_import_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (uiState.isPersian)
                                "وارد کردن فایل سه‌بعدی و عکس (Import)"
                            else
                                "Import 3D File & Photo",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showQuickImportDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Text(
                        text = if (uiState.isPersian)
                            "یک فایل سه‌بعدی از حافظه گوشی وارد کنید یا عکس‌های گالری را به مدل سه‌بعدی برجسته و تکسچر تبدیل نمایید:"
                        else
                            "Choose a 3D file from storage or select a photo from gallery:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Surface(
                        color = CyberCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberCyan, RoundedCornerShape(14.dp))
                            .clickable {
                                showQuickImportDialog = false
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            }
                            .testTag("dialog_import_3d_file_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (uiState.isPersian)
                                        "📂 وارد کردن فایل سه‌بعدی (.GLB, .STL, .OBJ, .PLY)"
                                    else
                                        "Import 3D Model File (.GLB, .STL, .OBJ, .PLY)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isPersian)
                                        "بارگذاری مستقیم مدل‌های سه‌بعدی در مرکز میز کار"
                                    else
                                        "Load 3D meshes directly onto the workspace",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Surface(
                        color = SculptAmber.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SculptAmber, RoundedCornerShape(14.dp))
                            .clickable {
                                showQuickImportDialog = false
                                photoTo3DReliefLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("dialog_photo_to_3d_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = SculptAmber)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (uiState.isPersian)
                                        "🏔️ تبدیل عکس گالری به مدل سه‌بعدی برجسته (Photo to 3D)"
                                    else
                                        "Convert Photo to 3D Relief Model",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isPersian)
                                        "ساخت مش سه‌بعدی برجسته (Heightmap / Lithophane) از هر عکس یا لوگو"
                                    else
                                        "Generate a 3D heightmap relief mesh from any photo or logo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Surface(
                        color = ManifoldEmerald.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ManifoldEmerald, RoundedCornerShape(14.dp))
                            .clickable {
                                showQuickImportDialog = false
                                photoTextureLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("dialog_photo_texture_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = null, tint = ManifoldEmerald)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (uiState.isPersian)
                                        "🖼️ اعمال عکس گالری به عنوان تکسچر روی مدل فعلی"
                                    else
                                        "Apply Gallery Photo as 3D Surface Texture",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isPersian)
                                        "پوشاندن سطح مدل سه‌بعدی فعلی با عکس دلخواه از گالری"
                                    else
                                        "Wrap any gallery image onto the active 3D model",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWebStudioPreview) {
        val webHtml = remember(uiState.nodes, uiState.projectName) {
            viewModel.generateCurrentWebStudioHtml()
        }
        WebStudioPreviewDialog(
            isPersian = uiState.isPersian,
            projectName = uiState.projectName,
            htmlContent = webHtml,
            apkSizeKb = uiState.installedApkSizeKb,
            onDismiss = { showWebStudioPreview = false },
            onSaveWebHtmlFile = { fileName ->
                viewModel.selectExportFormat(ExportFormat3D.HTML_WEB3D)
                saveDocumentLauncher.launch(fileName)
            },
            onShareWebHtmlFile = {
                viewModel.shareStandaloneWebHtml(context)
            },
            onSaveAndroidApkFile = { apkFileName ->
                saveApkLauncher.launch(apkFileName)
            },
            onShareAndroidApkFile = {
                viewModel.shareInstalledApk(context)
            }
        )
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
    onLaunchImportFile: () -> Unit,
    onLaunchImportPhotoTexture: () -> Unit,
    onLaunchImportPhotoTo3D: () -> Unit,
    onOpenLiveWebStudio: () -> Unit,
    onShareStandaloneWebHtml: () -> Unit,
    onSaveAndroidApkFile: (String) -> Unit,
    onShareAndroidApkFile: () -> Unit
) {
    when (uiState.activeTab) {
        StudioTab.LIBRARY -> {
            LibraryPanel(
                isPersian = uiState.isPersian,
                onAddPrimitive = { prim ->
                    viewModel.addPrimitive(prim, appendToScene = false)
                    viewModel.selectTab(StudioTab.VIEWPORT)
                },
                onLoadPreset = viewModel::loadReadyModelPreset,
                onImport3DFile = onLaunchImportFile,
                onImportPhotoTo3D = onLaunchImportPhotoTo3D,
                onImportPhotoTexture = onLaunchImportPhotoTexture
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
                lightIntensity = uiState.lightIntensity,
                ambientLightColorHex = uiState.ambientLightColorHex,
                shadowSoftness = uiState.shadowSoftness,
                onSetShadingMode = viewModel::setShadingMode,
                onSetLightingPreset = viewModel::setLightingPreset,
                onSetLightIntensity = viewModel::setLightIntensity,
                onSetAmbientLightColor = viewModel::setAmbientLightColor,
                onSetShadowSoftness = viewModel::setShadowSoftness,
                onUpdateMaterial = viewModel::updateSelectedMaterial,
                onApplyPresetMaterial = viewModel::applyMaterialPresetToSelected,
                onSelectTextureType = viewModel::selectTextureType,
                onAddPaintStroke = viewModel::addPaintStrokeToSelected,
                onClearPaintStrokes = viewModel::clearPaintStrokesOnSelected,
                onRandomizeTexture = viewModel::randomizeProceduralTexture,
                onExportTexturePng = { viewModel.shareCurrentTextureAsPng(context) },
                onImportPhotoTexture = onLaunchImportPhotoTexture,
                onImportPhotoTo3D = onLaunchImportPhotoTo3D
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
                isOnline = uiState.isOnline,
                apkSizeKb = uiState.installedApkSizeKb,
                onUpdateProjectName = viewModel::updateProjectName,
                onSelectFormat = viewModel::selectExportFormat,
                onRequestSaveFileToDevice = { _, fileName -> onLaunchSaveFile(fileName) },
                onShareExportFile = onLaunchShareFile,
                onRequestImportFile = onLaunchImportFile,
                onImportOnlineUrl = viewModel::importModelFromOnlineUrl,
                onSaveProjectToRoom = { viewModel.saveCurrentProjectToRoom() },
                onLoadProjectFromRoom = viewModel::loadProjectFromRoom,
                onDeleteProjectFromRoom = viewModel::deleteProjectFromRoom,
                onOpenLiveWebStudio = onOpenLiveWebStudio,
                onShareStandaloneWebHtml = onShareStandaloneWebHtml,
                onSaveAndroidApkFile = onSaveAndroidApkFile,
                onShareAndroidApkFile = onShareAndroidApkFile
            )
        }
        StudioTab.VIEWPORT -> Unit
    }
}

@Composable
private fun StudioTopHeaderBar(
    uiState: StudioUiState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onEnterFullScreen: () -> Unit,
    onQuickImportClick: () -> Unit,
    onQuickPhotoClick: () -> Unit,
    onQuickWebClick: () -> Unit,
    onQuickTextureClick: () -> Unit,
    onQuickExportClick: () -> Unit
) {
    Surface(
        color = StudioSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Compact Brand + Mesh Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan.copy(alpha = 0.18f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "PolyForge 3D",
                        tint = CyberCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "PolyForge 3D",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%dV • %dT",
                            uiState.engineeringStats.totalVertices,
                            uiState.engineeringStats.totalTriangles
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Horizontally Scrollable Action Bar so buttons never wrap or squish on narrow phones
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Quick Import File & Photo Pill (First so always immediately visible)
                Surface(
                    color = CyberCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                        .clickable(onClick = onQuickImportClick)
                        .testTag("quick_import_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Import File or Photo",
                            tint = CyberCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (uiState.isPersian) "وارد کردن فایل/عکس" else "Import",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IconButton(
                    onClick = onUndo,
                    enabled = uiState.canUndo,
                    modifier = Modifier.size(30.dp).testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (uiState.canUndo) Color.White else TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = uiState.canRedo,
                    modifier = Modifier.size(30.dp).testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (uiState.canRedo) Color.White else TextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Full-Screen Immersive Button
                IconButton(
                    onClick = onEnterFullScreen,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan.copy(alpha = 0.16f))
                        .testTag("fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Full Screen 3D",
                        tint = CyberCyan,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Quick Standalone Web / APK Pill
                Surface(
                    color = ManifoldEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, ManifoldEmerald, RoundedCornerShape(8.dp))
                        .clickable(onClick = onQuickWebClick)
                        .testTag("quick_web_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Web & APK",
                            tint = ManifoldEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (uiState.isPersian) "وب/APK" else "Web",
                            style = MaterialTheme.typography.labelSmall,
                            color = ManifoldEmerald,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = SculptAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, SculptAmber, RoundedCornerShape(8.dp))
                        .clickable(onClick = onQuickTextureClick)
                        .testTag("quick_texture_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Texture Studio",
                            tint = SculptAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (uiState.isPersian) "تکسچر" else "Tex",
                            style = MaterialTheme.typography.labelSmall,
                            color = SculptAmber,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = CyberCyan,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable(onClick = onQuickExportClick)
                        .testTag("quick_export_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export GLB/STL",
                            tint = Color(0xFF00242B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "GLB",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00242B),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
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
            modifier = Modifier.height(24.dp)
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
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .border(
                1.dp,
                if (active) CyberCyan else StudioBorder.copy(alpha = 0.75f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) CyberCyan else Color.White,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CyberCyan else StudioSurfaceElevated)
            .border(
                1.dp,
                if (selected) CyberCyan else StudioBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF00242B) else Color.White,
            modifier = Modifier.size(17.dp)
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
            .height(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) color.copy(alpha = 0.28f) else StudioSurfaceElevated)
            .border(
                1.dp,
                if (selected) color else StudioBorder,
                RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else TextSecondary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
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
        tonalElevation = 4.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.height(64.dp)
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
                        contentDescription = tab.titleEn,
                        modifier = Modifier.size(19.dp)
                    )
                },
                label = {
                    Text(
                        text = if (isPersian) tab.titleFa else tab.titleEn,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false
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
