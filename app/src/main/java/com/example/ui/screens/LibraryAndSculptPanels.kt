package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.Axis
import com.example.engine.PrimitiveGenerator
import com.example.engine.PrimitiveType3D
import com.example.engine.ReadyModelPreset
import com.example.model.MeshEngineeringStats
import com.example.model.SceneNode3D
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ManifoldEmerald
import com.example.ui.theme.SculptAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryPanel(
    isPersian: Boolean,
    onAddPrimitive: (PrimitiveType3D) -> Unit,
    onLoadPreset: (ReadyModelPreset, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember { PrimitiveGenerator.getReadyModelPresets() }
    val primitives = remember { PrimitiveType3D.entries }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_panel_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Studio Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(168.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = "PolyForge 3D Studio Hero Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xCC0B101E),
                                        Color(0xF50B101E)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isPersian) "کتابخانه جامع مهندسی و مدل‌های آماده" else "CAD & 3D Asset Library",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian)
                                "۱۲ شکل پارامتریک پایه + ۸ مدل چندبخشی حرفه‌ای آماده خروجی GLB و STL"
                            else
                                "12 Parametric Primitives + 8 Multi-Part Studio Templates for GLB & STL",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 1: 12 Parametric Primitives
        item {
            Text(
                text = if (isPersian) "افزودن سریع اشکال پایه و قطعات مکانیکی (Primitives)" else "Parametric 3D Primitives",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPersian)
                    "با لمس هر شکل، مش سه‌بعدی استاندارد آن مستقیماً به مرکز صحنه اضافه می‌شود:"
                else
                    "Tap any primitive to spawn a manifold triangle mesh into the active scene:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 3
            ) {
                for (prim in primitives) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                            .clickable { onAddPrimitive(prim) }
                            .testTag("add_prim_${prim.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(prim.defaultColorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = prim.titleEn,
                                    tint = Color(prim.defaultColorHex),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isPersian) prim.titleFa else prim.titleEn,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = prim.categoryFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Section 2: 8 Ready-Made Complete 3D Models
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPersian) "مدل‌های سه‌بعدی آماده حرفه‌ای (Multi-Part Studio Models)" else "Ready-Made 3D Studio Assemblies",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = if (isPersian)
                    "مدل‌های کاملاً واقعی و قابل ویرایش تفکیک‌شده به قطعات مجزا با متریال PBR:"
                else
                    "Editable multi-node 3D assemblies with pre-configured PBR materials:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        items(presets, key = { it.id }) { preset ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(preset.accentHex).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .testTag("preset_card_${preset.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(preset.accentHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = preset.titleEn,
                                    tint = Color(preset.accentHex)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isPersian) preset.titleFa else preset.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = preset.titleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(preset.accentHex)
                                )
                            }
                        }

                        Surface(
                            color = Color(preset.accentHex).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = preset.badgeFa,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(preset.accentHex),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = preset.subtitleFa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onLoadPreset(preset, false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(preset.accentHex),
                                contentColor = Color(0xFF090D16)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_preset_${preset.id}")
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "بارگذاری مدل در صحنه" else "Open Assembly")
                        }

                        OutlinedButton(
                            onClick = { onLoadPreset(preset, true) },
                            modifier = Modifier.testTag("append_preset_${preset.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "ادغام با صحنه" else "Append")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SculptAndModifiersPanel(
    isPersian: Boolean,
    selectedNode: SceneNode3D?,
    engineeringStats: MeshEngineeringStats,
    onSubdivide: (Boolean) -> Unit,
    onExtrude: (Float) -> Unit,
    onSmooth: () -> Unit,
    onTwist: (Float, Axis) -> Unit,
    onTaper: (Float) -> Unit,
    onInflate: (Float) -> Unit,
    onVoxelDecimate: () -> Unit,
    onMirror: (Axis) -> Unit,
    onDropToFloor: () -> Unit,
    onBooleanWeldAll: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("sculpt_modifiers_panel"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Target Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersian) "قطعه انتخاب‌شده جهت اسکلپت و تغییر مش:" else "Active Target Mesh:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedNode?.let { if (isPersian) it.nameFa else it.name }
                                ?: (if (isPersian) "هیچ قطعه‌ای انتخاب نشده است" else "No object selected"),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        if (selectedNode != null) {
                            Text(
                                text = "${selectedNode.vertices.size} Vertices • ${selectedNode.faces.size} Triangles",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    if (selectedNode != null) {
                        FilledTonalButton(
                            onClick = onDuplicate,
                            modifier = Modifier.testTag("sculpt_duplicate_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "تکثیر قطعه" else "Duplicate")
                        }
                    }
                }
            }
        }

        // Topology & Subdivision Modifiers
        item {
            Text(
                text = if (isPersian) "۱. ابزارهای توپولوژی، اکسترود و سطح‌سازی (Mesh Topology)" else "1. Mesh Topology & Subdivision",
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
                ModifierActionCard(
                    title = if (isPersian) "تقسیم سطوح نرم (Subdivide Smooth)" else "Smooth Subdivide",
                    subtitle = if (isPersian) "افزایش ۴ برابری مثلث‌ها + نرم‌سازی" else "4x triangles + Catmull smooth",
                    icon = Icons.Default.GridOn,
                    accent = CyberCyan,
                    tag = "mod_subdivide_smooth",
                    onClick = { onSubdivide(true) },
                    modifier = Modifier.weight(1f)
                )
                ModifierActionCard(
                    title = if (isPersian) "اکسترود سطوح (Extrude Faces)" else "Extrude Top Faces",
                    subtitle = if (isPersian) "برجسته‌سازی وجوه رو به بالا" else "Push outer faces along normal",
                    icon = Icons.Default.OpenInFull,
                    accent = SculptAmber,
                    tag = "mod_extrude",
                    onClick = { onExtrude(0.32f) },
                    modifier = Modifier.weight(1f)
                )
                ModifierActionCard(
                    title = if (isPersian) "نرم‌سازی لاپلاسین (Smooth Mesh)" else "Laplacian Smooth",
                    subtitle = if (isPersian) "حذف زاویه‌های تیز و صیقل مش" else "Relax vertex positions",
                    icon = Icons.Default.AutoFixHigh,
                    accent = ManifoldEmerald,
                    tag = "mod_smooth",
                    onClick = onSmooth,
                    modifier = Modifier.weight(1f)
                )
                ModifierActionCard(
                    title = if (isPersian) "کاهش پلی‌گان (Voxel Decimate)" else "Voxel Decimate",
                    subtitle = if (isPersian) "تبدیل به مش سبک Low-Poly" else "Optimize & merge close vertices",
                    icon = Icons.Default.Compress,
                    accent = Color(0xFFA855F7),
                    tag = "mod_decimate",
                    onClick = onVoxelDecimate,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Parametric Deformers & Sculpting
        item {
            Text(
                text = if (isPersian) "۲. تغییر شکل پارامتریک و اسکلپت حجمی (Deformers & Sculpt)" else "2. Parametric Deformers & Sculpting",
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
                ModifierActionCard(
                    title = if (isPersian) "پیچش محوری (+۴۵ درجه)" else "Twist Y (+45°)",
                    subtitle = if (isPersian) "چرخش مارپیچی رئوس حول محور Y" else "Helical vertex twist along Y",
                    icon = Icons.Default.Rotate90DegreesCcw,
                    accent = CyberCyan,
                    tag = "mod_twist_pos",
                    onClick = { onTwist(45f, Axis.Y) },
                    modifier = Modifier.weight(1f)
                )
                ModifierActionCard(
                    title = if (isPersian) "مخروطی‌سازی (Taper Top)" else "Taper Profile",
                    subtitle = if (isPersian) "باریک‌سازی تدریجی مقطع بالایی" else "Linear cross-section taper",
                    icon = Icons.Default.Compress,
                    accent = SculptAmber,
                    tag = "mod_taper",
                    onClick = { onTaper(0.65f) },
                    modifier = Modifier.weight(1f)
                )
                ModifierActionCard(
                    title = if (isPersian) "تورم حجمی (Sculpt Inflate)" else "Sculpt Inflate (+)",
                    subtitle = if (isPersian) "حجیم‌سازی مش در جهت بردار نرمال" else "Displace vertices outward",
                    icon = Icons.Default.Expand,
                    accent = ManifoldEmerald,
                    tag = "mod_inflate",
                    onClick = { onInflate(0.10f) },
                    modifier = Modifier.weight(1f)
                )
                ModifierActionCard(
                    title = if (isPersian) "فشرده‌سازی حجمی (Sculpt Pinch)" else "Sculpt Deflate (-)",
                    subtitle = if (isPersian) "لاغر و فشرده‌سازی سطوح مش" else "Contract vertices inward",
                    icon = Icons.Default.Compress,
                    accent = Color(0xFFF43F5E),
                    tag = "mod_deflate",
                    onClick = { onInflate(-0.09f) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // CAD Precision & Boolean Operations
        item {
            Text(
                text = if (isPersian) "۳. ابزارهای مهندسی CAD، قرینه‌سازی و آماده‌سازی پرینت" else "3. CAD Symmetry, Weld & Print Bed Prep",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModifierActionCard(
                    title = if (isPersian) "قرینه‌سازی محور X" else "Mirror X-Axis",
                    subtitle = if (isPersian) "ایجاد قرینه دقیق با اصلاح نرمال" else "Clone & flip across X",
                    icon = Icons.Default.Flip,
                    accent = CyberCyan,
                    tag = "mod_mirror_x",
                    onClick = { onMirror(Axis.X) },
                    modifier = Modifier.weight(1f)
                )
                ModifierActionCard(
                    title = if (isPersian) "تراز روی کف پرینتر (Y=0)" else "Drop to Print Bed",
                    subtitle = if (isPersian) "نشاندن پایین‌ترین نقطه روی زمین" else "Snap min Y to build plate",
                    icon = Icons.Default.VerticalAlignBottom,
                    accent = ManifoldEmerald,
                    tag = "mod_drop_floor",
                    onClick = onDropToFloor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onBooleanWeldAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SculptAmber,
                    contentColor = Color(0xFF1F1200)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("mod_boolean_weld_all")
            ) {
                Icon(Icons.Default.CallMerge, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPersian)
                        "ادغام کل قطعات صحنه در یک مش واحد (Boolean Scene Weld)"
                    else
                        "Weld All Scene Parts into Single Manifold Mesh",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3D Print & Engineering Diagnostics Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ManifoldEmerald.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = ManifoldEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "آنالیز مهندسی و پرینت سه‌بعدی (STL Slicer Doctor)" else "3D Printing & CAD Diagnostics",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = ManifoldEmerald.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ManifoldEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (engineeringStats.isManifoldWatertight) "Manifold OK" else "Checked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ManifoldEmerald
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadgeBox(
                            label = if (isPersian) "ابعاد (mm)" else "Dimensions (mm)",
                            value = String.format(
                                Locale.US,
                                "%.0f×%.0f×%.0f",
                                engineeringStats.widthMm,
                                engineeringStats.heightMm,
                                engineeringStats.depthMm
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgeBox(
                            label = if (isPersian) "حجم و مساحت" else "Volume / Area",
                            value = String.format(
                                Locale.US,
                                "%.1f cm³ | %.0f cm²",
                                engineeringStats.volumeCm3,
                                engineeringStats.surfaceAreaCm2
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadgeBox(
                            label = if (isPersian) "مصرف فیلامنت PLA" else "Est. PLA Weight",
                            value = String.format(Locale.US, "%.1f g (~%d min)", engineeringStats.estimatedPlaGrams, engineeringStats.estimatedPrintMinutes),
                            modifier = Modifier.weight(1f)
                        )
                        StatBadgeBox(
                            label = if (isPersian) "مصرف رزین SLA" else "Est. SLA Resin",
                            value = String.format(Locale.US, "%.1f g (Watertight)", engineeringStats.estimatedResinGrams),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
        modifier = modifier
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatBadgeBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurfaceElevated)
            .padding(10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
            color = CyberCyan
        )
    }
}
