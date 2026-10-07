package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExportLogEntity
import com.example.data.ProjectEntity
import com.example.engine.ExportPayload
import com.example.engine.PrimitiveGenerator
import com.example.model.ExportFormat3D
import com.example.model.PbrMaterial
import com.example.model.SceneNode3D
import com.example.model.StudioLightingPreset
import com.example.model.ViewportShadingMode
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ManifoldEmerald
import com.example.ui.theme.SculptAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialAndLightPanel(
    isPersian: Boolean,
    selectedNode: SceneNode3D?,
    shadingMode: ViewportShadingMode,
    lightingPreset: StudioLightingPreset,
    onSetShadingMode: (ViewportShadingMode) -> Unit,
    onSetLightingPreset: (StudioLightingPreset) -> Unit,
    onUpdateMaterial: ((PbrMaterial) -> PbrMaterial) -> Unit,
    onApplyPresetMaterial: (PbrMaterial, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val curatedMaterials = remember { PrimitiveGenerator.curatedMaterials }
    val currentMat = selectedNode?.material ?: PbrMaterial()

    val quickSwatches = remember {
        listOf(
            0xFF00E5FFL, 0xFF38BDF8L, 0xFF3B82F6L, 0xFF818CF8L,
            0xFFA855F7L, 0xFFEC4899L, 0xFFEF4444L, 0xFFFB923CL,
            0xFFF59E0BL, 0xFFFACC15L, 0xFF10B981L, 0xFF2DD4BFL,
            0xFFF8FAFCL, 0xFF94A3B8L, 0xFF475569L, 0xFF1E293BL
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("material_light_panel"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lighting & Shading Mode Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LightMode, contentDescription = null, tint = SculptAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "نورپردازی استودیو و حالت رندر (Studio Shader)" else "Viewport Shading & Studio Lighting",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isPersian) "حالت نمایش مش در محیط سه‌بعدی:" else "Viewport Shading Mode:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViewportShadingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = shadingMode == mode,
                                onClick = { onSetShadingMode(mode) },
                                label = { Text(if (isPersian) mode.labelFa else mode.labelEn) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("shading_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian) "محیط نورپردازی (HDRI Studio Rig):" else "Studio Lighting Rig:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioLightingPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = lightingPreset == preset,
                                onClick = { onSetLightingPreset(preset) },
                                label = { Text(if (isPersian) preset.labelFa else preset.labelEn) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SculptAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = SculptAmber
                                ),
                                modifier = Modifier.testTag("light_preset_${preset.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Custom PBR Sliders & Color Swatches
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian)
                                "تنظیمات دقیق متریال PBR (${selectedNode?.nameFa ?: "قطعه انتخابی"})"
                            else
                                "PBR Material Editor (${selectedNode?.name ?: "Selected"})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isPersian) "انتخاب رنگ پایه (Albedo Base Color):" else "Base Color Swatches:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        quickSwatches.forEach { hex ->
                            val isSelectedColor = currentMat.baseColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (isSelectedColor) 3.dp else 1.dp,
                                        color = if (isSelectedColor) Color.White else StudioBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateMaterial { it.copy(baseColorHex = hex) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelectedColor) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF090D16),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metallic Slider
                    PbrParameterSlider(
                        label = if (isPersian) "ضریب فلزی بودن (Metallic)" else "Metallic Factor",
                        value = currentMat.metallic,
                        onValueChange = { v -> onUpdateMaterial { it.copy(metallic = v) } },
                        accent = CyberCyan
                    )

                    // Roughness Slider
                    PbrParameterSlider(
                        label = if (isPersian) "زبری و ماتی سطح (Roughness)" else "Roughness Factor",
                        value = currentMat.roughness,
                        onValueChange = { v -> onUpdateMaterial { it.copy(roughness = v) } },
                        accent = SculptAmber
                    )

                    // Emission Strength Slider
                    PbrParameterSlider(
                        label = if (isPersian) "درخشش نئونی (Emissive Glow)" else "Emissive Glow",
                        value = currentMat.emissionStrength,
                        onValueChange = { v ->
                            onUpdateMaterial {
                                it.copy(
                                    emissionStrength = v,
                                    emissionHex = if (v > 0.02f) it.baseColorHex else 0xFF000000
                                )
                            }
                        },
                        accent = ManifoldEmerald
                    )

                    // Opacity Slider
                    PbrParameterSlider(
                        label = if (isPersian) "شفافیت شیشه (Opacity / Alpha)" else "Surface Opacity",
                        value = currentMat.opacity,
                        valueRange = 0.2f..1.0f,
                        onValueChange = { v -> onUpdateMaterial { it.copy(opacity = v) } },
                        accent = Color(0xFFA855F7)
                    )
                }
            }
        }

        // 16 Curated PBR Presets
        item {
            Text(
                text = if (isPersian) "۱۶ متریال مهندسی و استودیویی آماده (PBR Presets)" else "16 Curated PBR Material Presets",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                curatedMaterials.forEachIndexed { idx, mat ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                            .clickable { onApplyPresetMaterial(mat, false) }
                            .testTag("mat_preset_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(mat.baseColorHex))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPersian) mat.nameFa else mat.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = String.format(Locale.US, "M: %.0f%% • R: %.0f%%", mat.metallic * 100, mat.roughness * 100),
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
}

@Composable
private fun PbrParameterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    accent: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White)
            Text(
                text = String.format(Locale.US, "%d%%", (value * 100).toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            )
        )
    }
}

@Composable
fun ExportAndProjectsPanel(
    isPersian: Boolean,
    projectName: String,
    selectedFormat: ExportFormat3D,
    exportPayload: ExportPayload?,
    savedProjects: List<ProjectEntity>,
    recentExports: List<ExportLogEntity>,
    onUpdateProjectName: (String) -> Unit,
    onSelectFormat: (ExportFormat3D) -> Unit,
    onRequestSaveFileToDevice: (ExportFormat3D, String) -> Unit,
    onShareExportFile: () -> Unit,
    onRequestImportFile: () -> Unit,
    onSaveProjectToRoom: () -> Unit,
    onLoadProjectFromRoom: (ProjectEntity) -> Unit,
    onDeleteProjectFromRoom: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("export_projects_panel"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Project Name & Quick Save/Import Header
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isPersian) "نام فایل و پروژه سه‌بعدی" else "Project & Export Filename",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = onUpdateProjectName,
                        label = { Text(if (isPersian) "نام مدل (بدون پسوند)" else "Model Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSaveProjectToRoom,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ManifoldEmerald,
                                contentColor = Color(0xFF002417)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_project_db_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "ذخیره پروژه در برنامه" else "Save Project", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onRequestImportFile,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_3d_file_btn")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "ورود فایل STL/OBJ/PLY" else "Import 3D File")
                        }
                    }
                }
            }
        }

        // Format Selection (GLB, Binary STL, ASCII STL, OBJ, PLY)
        item {
            Text(
                text = if (isPersian) "انتخاب فرمت خروجی سه‌بعدی واقعی (Real 3D Encoders)" else "Select 3D Export Format",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExportFormat3D.entries.forEach { format ->
                    val isSelected = selectedFormat == format
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF083344) else StudioSurfaceElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CyberCyan else StudioBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectFormat(format) }
                            .testTag("format_option_${format.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isSelected) CyberCyan else StudioSurface,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = ".${format.ext.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (isSelected) Color(0xFF002229) else CyberCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isPersian) format.titleFa else format.titleEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = format.subtitleFa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Actions & Real Binary/ASCII File Inspector
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val fileName = exportPayload?.fileName ?: "${projectName}.${selectedFormat.ext}"
                    val byteCount = exportPayload?.bytes?.size ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = CyberCyan
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Size: %,d bytes (%.1f KB) • %d Vertices • %d Triangles",
                                    byteCount,
                                    byteCount / 1024f,
                                    exportPayload?.vertexCount ?: 0,
                                    exportPayload?.triangleCount ?: 0
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onRequestSaveFileToDevice(selectedFormat, fileName) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF00242B)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("export_download_device_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "ذخیره فایل .${selectedFormat.ext.uppercase()}" else "Save .${selectedFormat.ext.uppercase()}",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onShareExportFile,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SculptAmber,
                                contentColor = Color(0xFF241100)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("export_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "اشتراک‌گذاری مستقیم" else "Share 3D File",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = ManifoldEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian)
                                "بازرس ساختار باینری / متنی فایل تولیدشده (Live File Header Inspector):"
                            else
                                "Generated 3D File Structure Preview:",
                            style = MaterialTheme.typography.labelMedium,
                            color = ManifoldEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioObsidian)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = exportPayload?.headerPreview ?: "Ready to encode...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5F3FC),
                            maxLines = 12
                        )
                    }
                }
            }
        }

        // Saved Projects in Room Database
        item {
            Text(
                text = if (isPersian) "پروژه‌های ذخیره‌شده در پایگاه داده (${savedProjects.size})" else "Saved Studio Projects (${savedProjects.size})",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        items(savedProjects, key = { it.id }) { proj ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                    .testTag("saved_project_${proj.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(proj.accentHex).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(proj.accentHex))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = proj.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${proj.objectCount} Parts • ${proj.triangleCount} Tris • ${proj.vertexCount} Verts",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onLoadProjectFromRoom(proj) }) {
                            Text(if (isPersian) "باز کردن" else "Load")
                        }
                        IconButton(onClick = { onDeleteProjectFromRoom(proj.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete project",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }

        if (recentExports.isNotEmpty()) {
            item {
                Text(
                    text = if (isPersian) "تاریخچه خروجی‌های اخیر" else "Recent Export History",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
            items(recentExports.take(5), key = { it.id }) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${log.fileName} (${log.formatName})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${log.byteSize / 1024} KB • ${log.triangleCount} Tris",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                }
            }
        }
    }
}
