package com.example.engine

import com.example.model.SceneNode3D
import java.io.File
import java.util.Base64
import java.util.Locale

/**
 * Generates Web-ready 3D files:
 * 1. Standalone Interactive HTML5 / WebGL 3D Studio & Viewer (.html) with full 5-tab studio UI
 *    (Viewport, Models Library, Mesh Sculpting, Texture & Paint Studio, Exporters), embedded scene,
 *    procedural/baked textures, touch/mouse orbit controls, and in-browser GLB/STL/OBJ/PLY/GLTF exporters.
 * 2. Standard Web glTF 2.0 (.gltf) JSON with embedded Base64 data URI buffer for Three.js / <model-viewer> / WebXR.
 */
object Web3DGenerator {

    fun exportToWebGltfEmbedded(
        nodes: List<SceneNode3D>,
        binaryChunkGenerator: (List<SceneNode3D>) -> Pair<ByteArray, String>
    ): String {
        val (glbBytes, _) = binaryChunkGenerator(nodes)
        if (glbBytes.size < 20) {
            return """{"asset":{"version":"2.0"}}"""
        }
        val jsonLen = java.nio.ByteBuffer.wrap(glbBytes, 12, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        val jsonStr = String(glbBytes, 20, jsonLen, Charsets.UTF_8).trim()
        val binOffset = 20 + jsonLen + 8
        val binBytes = if (glbBytes.size > binOffset) {
            glbBytes.copyOfRange(binOffset, glbBytes.size)
        } else {
            ByteArray(0)
        }
        val b64 = Base64.getEncoder().encodeToString(binBytes)
        return jsonStr.replace(
            """"byteLength": ${binBytes.size}""",
            """"byteLength": ${binBytes.size}, "uri": "data:application/octet-stream;base64,$b64""""
        )
    }

    /**
     * Generates a 100% self-contained, zero-dependency HTML5 + 3D Canvas/WebGL Web Application (.html)
     * with the complete 5-tab PolyForge 3D Studio UI (میز کار, مدل‌ها, ابزار مش, تکسچر, خروجی)
     * that embeds the user's current 3D models AND provides full in-browser 3D modeling, texture studio,
     * and real .GLB, .STL, .OBJ, .PLY, and .GLTF downloads directly inside any browser or WebView APK.
     */
    fun generateStandaloneWeb3DHtml(
        nodes: List<SceneNode3D>,
        projectName: String
    ): String {
        val sb = StringBuilder("[")
        var firstNode = true
        for (node in nodes) {
            if (!node.visible || node.vertices.isEmpty() || node.faces.isEmpty()) continue
            if (!firstNode) sb.append(",")
            firstNode = false

            val worldVerts = node.worldVertices()
            val localVerts = node.vertices
            val mat = node.material

            val vSb = StringBuilder("[")
            for (i in worldVerts.indices) {
                if (i > 0) vSb.append(",")
                val v = worldVerts[i]
                vSb.append("[").append(round3(v.x)).append(",").append(round3(v.y)).append(",").append(round3(v.z)).append("]")
            }
            vSb.append("]")

            val fSb = StringBuilder("[")
            val cSb = StringBuilder("[")
            var firstFace = true
            var hasColors = false
            for (f in node.faces) {
                if (f.v0 in worldVerts.indices && f.v1 in worldVerts.indices && f.v2 in worldVerts.indices) {
                    if (!firstFace) fSb.append(",")
                    fSb.append("[").append(f.v0).append(",").append(f.v1).append(",").append(f.v2).append("]")
                    if (mat.hasActiveTexture()) {
                        if (!firstFace) cSb.append(",")
                        hasColors = true
                        val loc0 = localVerts[f.v0]
                        val loc1 = localVerts[f.v1]
                        val loc2 = localVerts[f.v2]
                        val centroid = (loc0 + loc1 + loc2) / 3f
                        val normal = (loc1 - loc0).cross(loc2 - loc0).normalized()
                        val (u, v) = TextureEngine.computeUV(centroid, normal, mat.uvMappingMode)
                        val (col, _) = TextureEngine.sampleTextureAndRoughness(u, v, mat)
                        val hex = String.format(
                            Locale.US,
                            "\"#%02X%02X%02X\"",
                            (col.red * 255).toInt().coerceIn(0, 255),
                            (col.green * 255).toInt().coerceIn(0, 255),
                            (col.blue * 255).toInt().coerceIn(0, 255)
                        )
                        cSb.append(hex)
                    }
                    firstFace = false
                }
            }
            fSb.append("]")
            cSb.append("]")

            val baseHex = String.format(Locale.US, "#%06X", (mat.baseColorHex and 0xFFFFFF))
            val secHex = String.format(Locale.US, "#%06X", (mat.textureSecondaryHex and 0xFFFFFF))
            val safeName = node.name.replace("\"", "\\\"")
            val safeNameFa = node.nameFa.replace("\"", "\\\"")

            sb.append("{")
                .append("\"id\":\"n_").append(node.id.take(6)).append("\",")
                .append("\"name\":\"").append(safeName).append("\",")
                .append("\"nameFa\":\"").append(safeNameFa).append("\",")
                .append("\"color\":\"").append(baseHex).append("\",")
                .append("\"secColor\":\"").append(secHex).append("\",")
                .append("\"metallic\":").append(round3(mat.metallic)).append(",")
                .append("\"roughness\":").append(round3(mat.roughness)).append(",")
                .append("\"texType\":\"").append(mat.textureType.name).append("\",")
                .append("\"uvMode\":\"").append(mat.uvMappingMode.name).append("\",")
                .append("\"texScale\":").append(round3(mat.textureScale)).append(",")
                .append("\"pos\":[0,0,0],\"rot\":[0,0,0],\"scale\":[1,1,1],\"visible\":true,")
                .append("\"verts\":").append(vSb).append(",")
                .append("\"faces\":").append(fSb)
            if (hasColors) {
                sb.append(",\"faceColors\":").append(cSb)
            }
            sb.append("}")
        }
        sb.append("]")

        val embeddedSceneJson = sb.toString()
        return buildFullStudioHtml(projectName, embeddedSceneJson)
    }

    private fun loadBaseStudioTemplate(): String {
        val resStream = Web3DGenerator::class.java.getResourceAsStream("/web/index.html")
            ?: Web3DGenerator::class.java.classLoader?.getResourceAsStream("web/index.html")
        if (resStream != null) {
            runCatching {
                val text = resStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (text.length > 5000) return text
            }
        }

        val candidateFiles = listOf(
            File("index.html"),
            File("app/src/main/resources/web/index.html"),
            File("app/src/main/assets/web/index.html"),
            File("app/src/main/assets/index.html"),
            File("web/index.html")
        )
        for (f in candidateFiles) {
            if (f.exists() && f.length() > 5000L) {
                runCatching {
                    val text = f.readText(Charsets.UTF_8)
                    if (text.contains("PolyForge 3D Studio")) return text
                }
            }
        }
        return FALLBACK_STUDIO_HTML
    }

    private fun buildFullStudioHtml(projectName: String, embeddedSceneJson: String): String {
        val raw = loadBaseStudioTemplate()
        val safeProject = projectName.trim().ifBlank { "PolyForge_Model" }
        if (raw.contains("/*__POLYFORGE_EMBEDDED_NODES__*/")) {
            return raw
                .replace("/*__POLYFORGE_EMBEDDED_NODES__*/ null", "/*__POLYFORGE_EMBEDDED_NODES__*/ $embeddedSceneJson")
                .replace("value=\"PolyForge_Model\"", "value=\"${safeProject.replace("\"", "&quot;")}\"")
                .replace("PolyForge 3D Studio — استودیو مدلسازی سه‌بعدی", "PolyForge 3D Studio — $safeProject")
        }
        return FALLBACK_STUDIO_HTML
            .replace("__PROJECT_NAME__", safeProject)
            .replace("const EMBEDDED_SCENE = [];", "const EMBEDDED_SCENE = $embeddedSceneJson;")
    }

    private val FALLBACK_STUDIO_HTML = """<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover" />
<meta name="theme-color" content="#0B101E" />
<title>PolyForge 3D Studio — __PROJECT_NAME__</title>
<style>
  :root {
    --bg: #0B101E;
    --surface: #111827;
    --surface-elev: #1E293B;
    --border: #334155;
    --cyan: #00E5FF;
    --amber: #FF9100;
    --emerald: #10B981;
    --rose: #F43F5E;
    --text: #F8FAFC;
    --muted: #94A3B8;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; -webkit-user-select: none; }
  html, body {
    width: 100vw; height: 100vh; overflow: hidden;
    background: var(--bg); color: var(--text);
    font-family: system-ui, -apple-system, 'Segoe UI', Tahoma, sans-serif;
    display: flex; flex-direction: column;
  }
  .top-header {
    height: 48px; flex-shrink: 0; background: var(--surface);
    border-bottom: 1px solid var(--border); display: flex; align-items: center;
    justify-content: space-between; padding: 0 10px; gap: 6px; z-index: 30;
  }
  .brand { display: flex; align-items: center; gap: 7px; flex-shrink: 0; }
  .brand-badge {
    width: 28px; height: 28px; border-radius: 8px;
    background: rgba(0,229,255,0.16); border: 1px solid var(--cyan);
    display: grid; place-items: center; color: var(--cyan); font-weight: 800; font-size: 11px;
  }
  .brand-title { font-size: 12px; font-weight: 700; color: #fff; white-space: nowrap; }
  .brand-stats { font-size: 10px; color: var(--cyan); font-family: monospace; direction: ltr; white-space: nowrap; }
  .header-actions { display: flex; align-items: center; gap: 4px; overflow-x: auto; scrollbar-width: none; }
  button {
    background: var(--surface-elev); color: var(--text);
    border: 1px solid var(--border); border-radius: 8px;
    padding: 5px 8px; font-size: 11px; font-weight: 600; cursor: pointer;
    white-space: nowrap; flex-shrink: 0; transition: 0.15s ease;
  }
  button:hover, button.active { border-color: var(--cyan); color: var(--cyan); background: rgba(0,229,255,0.16); }
  button.btn-cyan { background: var(--cyan); color: #00242B; border-color: var(--cyan); font-weight: 700; }
  button.btn-amber { background: rgba(255,145,0,0.2); color: var(--amber); border-color: var(--amber); }
  button.btn-emerald { background: rgba(16,185,129,0.2); color: var(--emerald); border-color: var(--emerald); }
  .workspace-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; position: relative; }
  #canvasContainer {
    flex: 1; position: relative; min-height: 175px;
    background: radial-gradient(circle at 50% 45%, #17223B 0%, #0B101E 72%, #070A14 100%);
    overflow: hidden;
  }
  #viewport { width: 100%; height: 100%; display: block; touch-action: none; cursor: grab; }
  .canvas-top-strip {
    position: absolute; top: 8px; left: 8px; right: 8px;
    display: flex; gap: 5px; overflow-x: auto; scrollbar-width: none; z-index: 12;
  }
  .canvas-bottom-dock {
    position: absolute; bottom: 8px; left: 8px; right: 8px;
    display: flex; flex-direction: column; gap: 6px; z-index: 12; pointer-events: none;
  }
  .dock-bar {
    pointer-events: auto; align-self: center;
    background: rgba(17,24,39,0.92); backdrop-filter: blur(12px);
    border: 1px solid var(--border); border-radius: 12px;
    padding: 5px 8px; display: flex; align-items: center; gap: 5px;
    max-width: 100%; overflow-x: auto; scrollbar-width: none;
  }
  #tabPanelSheet {
    flex: 1.25; background: var(--surface); border-top: 1px solid var(--border);
    display: none; flex-direction: column; overflow: hidden; z-index: 20;
  }
  .sheet-header {
    height: 40px; padding: 0 12px; background: var(--surface-elev);
    border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between;
  }
  .sheet-content { flex: 1; overflow-y: auto; padding: 10px; display: flex; flex-direction: column; gap: 10px; }
  .section-card {
    background: rgba(30,41,59,0.55); border: 1px solid var(--border);
    border-radius: 12px; padding: 10px; display: flex; flex-direction: column; gap: 8px;
  }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
  .grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; }
  .bottom-nav {
    height: 58px; flex-shrink: 0; background: var(--surface);
    border-top: 1px solid var(--border); display: grid; grid-template-columns: repeat(5, 1fr); z-index: 30;
  }
  .nav-item {
    background: transparent; border: none; border-radius: 0;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    gap: 2px; color: var(--muted); padding: 4px 2px;
  }
  .nav-item.active { color: var(--cyan); background: rgba(0,229,255,0.08); border-top: 2px solid var(--cyan); }
  .hidden { display: none !important; }
</style>
</head>
<body>
<header id="topHeader" class="top-header">
  <div class="brand">
    <div class="brand-badge">3D</div>
    <div>
      <div class="brand-title">PolyForge 3D — __PROJECT_NAME__</div>
      <div id="statsText" class="brand-stats">0 Verts • 0 Tris</div>
    </div>
  </div>
  <div class="header-actions">
    <button onclick="toggleFull(true)">تمام‌صفحه</button>
    <button class="btn-amber" onclick="downloadSTL()">.STL</button>
    <button onclick="downloadOBJ()">.OBJ</button>
    <button class="btn-cyan" onclick="downloadGLB()">.GLB</button>
  </div>
</header>

<main class="workspace-body">
  <div id="canvasContainer">
    <canvas id="viewport"></canvas>
    <div id="chipsHud" class="canvas-top-strip">
      <button id="btnWire" onclick="toggleWire()">مش سیمی</button>
      <button id="btnSpin" onclick="toggleSpin()">چرخش ۳۶۰°</button>
      <button onclick="resetCam()">ایزومتریک</button>
      <button onclick="subdivideActive()">Subdivide+</button>
      <button onclick="twistActive()">پیچش (Twist)</button>
      <button onclick="scaleActive(1.18)">بزرگ‌تر (+)</button>
      <button onclick="scaleActive(0.85)">کوچک‌تر (-)</button>
    </div>
    <div id="bottomHud" class="canvas-bottom-dock">
      <div class="dock-bar">
        <button onclick="loadPresetModel('embedded')">مدل پروژه</button>
        <button onclick="spawnPrim('cube')">مکعب</button>
        <button onclick="spawnPrim('sphere')">کره</button>
        <button onclick="spawnPrim('cylinder')">استوانه</button>
        <button onclick="spawnPrim('torus')">حلقه</button>
        <button onclick="spawnPrim('gear')">چرخ‌دنده</button>
        <button onclick="spawnPrim('gem')">الماس</button>
      </div>
    </div>
  </div>

  <section id="tabPanelSheet">
    <div class="sheet-header">
      <span id="sheetTitleText" style="font-size:12px;font-weight:700;color:var(--cyan);">منوی ابزار</span>
      <button onclick="selectTab('VIEWPORT')">بستن پنل ✕</button>
    </div>
    <div class="sheet-content">
      <div id="panelLIBRARY" class="section-card">
        <div style="font-size:12px;font-weight:700;">اشکال پایه و مدل‌های آماده</div>
        <div class="grid-3">
          <button onclick="loadPresetModel('embedded');selectTab('VIEWPORT')">مدل پروژه</button>
          <button onclick="spawnPrim('cube');selectTab('VIEWPORT')">مکعب</button>
          <button onclick="spawnPrim('sphere');selectTab('VIEWPORT')">کره</button>
          <button onclick="spawnPrim('cylinder');selectTab('VIEWPORT')">استوانه</button>
          <button onclick="spawnPrim('torus');selectTab('VIEWPORT')">حلقه</button>
          <button onclick="spawnPrim('gear');selectTab('VIEWPORT')">چرخ‌دنده</button>
          <button onclick="spawnPrim('gem');selectTab('VIEWPORT')">الماس</button>
        </div>
      </div>
      <div id="panelSCULPT" class="section-card hidden">
        <div style="font-size:12px;font-weight:700;">ابزارهای مش و حجاری سه‌بعدی</div>
        <div class="grid-2">
          <button onclick="subdivideActive()">افزایش جزئیات (Subdivide)</button>
          <button onclick="twistActive()">پیچش (Twist)</button>
          <button onclick="scaleActive(1.18)">بزرگ‌نمایی مدل (+)</button>
          <button onclick="scaleActive(0.85)">کوچک‌نمایی مدل (-)</button>
        </div>
      </div>
      <div id="panelMATERIAL" class="section-card hidden">
        <div style="font-size:12px;font-weight:700;">استودیو تکسچر و رنگ PBR</div>
        <div class="grid-3">
          <button onclick="setTex('NONE')">ساده PBR</button>
          <button onclick="setTex('CHECKER')">شطرنجی</button>
          <button onclick="setTex('CARBON')">کربن</button>
          <button onclick="setTex('MARBLE')">مرمر</button>
          <button onclick="setTex('STRIPES')">نئون</button>
        </div>
        <div class="grid-3">
          <button style="background:#00E5FF;color:#000" onclick="setColor('#00E5FF')">فیروزه‌ای</button>
          <button style="background:#F59E0B;color:#000" onclick="setColor('#F59E0B')">طلایی</button>
          <button style="background:#10B981;color:#000" onclick="setColor('#10B981')">زمردی</button>
          <button style="background:#EF4444;color:#fff" onclick="setColor('#EF4444')">یاقوتی</button>
          <button style="background:#A855F7;color:#fff" onclick="setColor('#A855F7')">بنفش</button>
          <button style="background:#F8FAFC;color:#000" onclick="setColor('#F8FAFC')">نقره‌ای</button>
        </div>
        <div style="margin-top:10px;font-size:12px;font-weight:700;color:#00E5FF">نورپردازی پیشرفته (Advanced Light)</div>
        <div style="display:flex;flex-direction:column;gap:5px;margin-top:4px;">
          <label style="font-size:10px;display:flex;justify-content:space-between"><span>شدت نور (Intensity)</span><span id="lblLightInt">1.00x</span></label>
          <input type="range" min="0.2" max="2.5" step="0.05" value="1.0" oninput="lightIntensity=parseFloat(this.value);document.getElementById('lblLightInt').textContent=parseFloat(this.value).toFixed(2)+'x'" />
          <label style="font-size:10px;display:flex;justify-content:space-between"><span>نرمی سایه (Shadow Softness)</span><span id="lblShadSoft">50%</span></label>
          <input type="range" min="0.0" max="1.0" step="0.05" value="0.5" oninput="shadowSoftness=parseFloat(this.value);document.getElementById('lblShadSoft').textContent=Math.round(this.value*100)+'%'" />
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;margin-top:6px;">
          <span style="font-size:11px;">رنگ نور محیطی:</span>
          <input type="color" value="#1E293B" onchange="ambientLightColor=this.value" style="width:36px;height:24px;border:none;background:none;cursor:pointer;" />
        </div>
      </div>
      <div id="panelEXPORT" class="section-card hidden">
        <div style="font-size:12px;font-weight:700;">خروجی استاندارد سه‌بعدی</div>
        <div class="grid-2">
          <button class="btn-cyan" onclick="downloadGLB()">دانلود .GLB</button>
          <button class="btn-amber" onclick="downloadSTL()">دانلود .STL</button>
          <button class="btn-emerald" onclick="downloadOBJ()">دانلود .OBJ</button>
          <button onclick="resetCam();selectTab('VIEWPORT')">بازنشانی دوربین</button>
        </div>
      </div>
    </div>
  </section>
</main>

<nav id="bottomNav" class="bottom-nav">
  <button id="navVIEWPORT" class="nav-item active" onclick="selectTab('VIEWPORT')"><span>🧊</span><span style="font-size:10px;font-weight:700">میز کار</span></button>
  <button id="navLIBRARY" class="nav-item" onclick="selectTab('LIBRARY')"><span>📚</span><span style="font-size:10px;font-weight:700">مدل‌ها</span></button>
  <button id="navSCULPT" class="nav-item" onclick="selectTab('SCULPT')"><span>🛠</span><span style="font-size:10px;font-weight:700">ابزار مش</span></button>
  <button id="navMATERIAL" class="nav-item" onclick="selectTab('MATERIAL')"><span>🎨</span><span style="font-size:10px;font-weight:700">تکسچر</span></button>
  <button id="navEXPORT" class="nav-item" onclick="selectTab('EXPORT')"><span>📤</span><span style="font-size:10px;font-weight:700">خروجی</span></button>
</nav>

<script>
const EMBEDDED_SCENE = [];
let meshes = JSON.parse(JSON.stringify(EMBEDDED_SCENE));
let camYaw = -0.55, camPitch = 0.38, camDist = 4.6, panX = 0, panY = 0;
let showWire = false, autoSpin = false, texMode = 'NONE';
let lightIntensity = 1.0, shadowSoftness = 0.5, ambientLightColor = '#1E293B';

const canvas = document.getElementById('viewport');
const ctx = canvas.getContext('2d');

function resize() {
  const cont = document.getElementById('canvasContainer');
  const w = cont.clientWidth || window.innerWidth;
  const h = cont.clientHeight || window.innerHeight;
  canvas.width = w * window.devicePixelRatio;
  canvas.height = h * window.devicePixelRatio;
  ctx.setTransform(window.devicePixelRatio, 0, 0, window.devicePixelRatio, 0, 0);
}
window.addEventListener('resize', resize);
resize();

function selectTab(tab) {
  ['VIEWPORT','LIBRARY','SCULPT','MATERIAL','EXPORT'].forEach(t => {
    document.getElementById('nav' + t).classList.toggle('active', t === tab);
  });
  const sheet = document.getElementById('tabPanelSheet');
  if (tab === 'VIEWPORT') {
    sheet.style.display = 'none';
  } else {
    sheet.style.display = 'flex';
    ['LIBRARY','SCULPT','MATERIAL','EXPORT'].forEach(p => {
      document.getElementById('panel' + p).classList.toggle('hidden', p !== tab);
    });
  }
  setTimeout(resize, 30);
}

function toggleFull(full) {
  document.getElementById('topHeader').classList.toggle('hidden', full);
  document.getElementById('bottomNav').classList.toggle('hidden', full);
  setTimeout(resize, 30);
}
function toggleWire() { showWire = !showWire; document.getElementById('btnWire').classList.toggle('active', showWire); }
function toggleSpin() { autoSpin = !autoSpin; document.getElementById('btnSpin').classList.toggle('active', autoSpin); }
function resetCam() { camYaw = -0.55; camPitch = 0.38; camDist = 4.6; panX = 0; panY = 0; }
function setColor(hex) { meshes.forEach(m => { m.color = hex; delete m.faceColors; }); }
function setTex(mode) { texMode = mode; meshes.forEach(m => delete m.faceColors); }

function loadPresetModel(type) {
  if (type === 'embedded' && EMBEDDED_SCENE.length > 0) {
    meshes = JSON.parse(JSON.stringify(EMBEDDED_SCENE));
  }
}

function spawnPrim(kind) {
  const v = [], f = [];
  if (kind === 'cube') {
    const s = 0.9;
    [[-s,-s,-s],[s,-s,-s],[s,s,-s],[-s,s,-s],[-s,-s,s],[s,-s,s],[s,s,s],[-s,s,s]].forEach(p => v.push(p));
    [[0,1,2],[0,2,3],[4,6,5],[4,7,6],[0,4,5],[0,5,1],[2,6,7],[2,7,3],[0,3,7],[0,7,4],[1,5,6],[1,6,2]].forEach(t => f.push(t));
  } else if (kind === 'sphere' || kind === 'gem') {
    const stacks = kind === 'gem' ? 5 : 14, slices = kind === 'gem' ? 8 : 20, r = 1.05;
    for (let i = 0; i <= stacks; i++) {
      const phi = Math.PI * i / stacks;
      for (let j = 0; j <= slices; j++) {
        const th = 2 * Math.PI * j / slices;
        v.push([r*Math.sin(phi)*Math.cos(th), r*Math.cos(phi), r*Math.sin(phi)*Math.sin(th)]);
      }
    }
    for (let i = 0; i < stacks; i++) {
      for (let j = 0; j < slices; j++) {
        const a = i*(slices+1)+j, b = a+slices+1;
        f.push([a,b,a+1]); f.push([b,b+1,a+1]);
      }
    }
  } else if (kind === 'cylinder' || kind === 'gear') {
    const seg = kind === 'gear' ? 24 : 20, h = kind === 'gear' ? 0.45 : 1.0;
    v.push([0,h,0]); v.push([0,-h,0]);
    for (let i = 0; i < seg; i++) {
      const a = 2*Math.PI*i/seg;
      const r = (kind === 'gear' && i%2===0) ? 1.25 : 0.92;
      v.push([r*Math.cos(a), h, r*Math.sin(a)]);
      v.push([r*Math.cos(a), -h, r*Math.sin(a)]);
    }
    for (let i = 0; i < seg; i++) {
      const n = (i+1)%seg;
      const t0 = 2+i*2, b0 = 3+i*2, t1 = 2+n*2, b1 = 3+n*2;
      f.push([0,t1,t0]); f.push([1,b0,b1]);
      f.push([t0,t1,b1]); f.push([t0,b1,b0]);
    }
  } else if (kind === 'torus') {
    const R = 0.85, r = 0.35, uSeg = 22, vSeg = 12;
    for (let i = 0; i <= uSeg; i++) {
      const u = 2*Math.PI*i/uSeg;
      for (let j = 0; j <= vSeg; j++) {
        const vv = 2*Math.PI*j/vSeg;
        v.push([(R + r*Math.cos(vv))*Math.cos(u), r*Math.sin(vv), (R + r*Math.cos(vv))*Math.sin(u)]);
      }
    }
    for (let i = 0; i < uSeg; i++) {
      for (let j = 0; j < vSeg; j++) {
        const a = i*(vSeg+1)+j, b = (i+1)*(vSeg+1)+j;
        f.push([a,b,a+1]); f.push([b,b+1,a+1]);
      }
    }
  }
  meshes = [{ name: kind.toUpperCase(), color: '#00E5FF', secColor: '#0F172A', metallic: 0.4, roughness: 0.25, verts: v, faces: f }];
}

function subdivideActive() {
  meshes.forEach(m => {
    if (m.faces.length > 2500) return;
    const nv = m.verts.slice();
    const nf = [];
    const midCache = {};
    function mid(i1, i2) {
      const key = i1 < i2 ? i1+'_'+i2 : i2+'_'+i1;
      if (midCache[key] !== undefined) return midCache[key];
      const p1 = nv[i1], p2 = nv[i2];
      const idx = nv.length;
      nv.push([(p1[0]+p2[0])*0.5, (p1[1]+p2[1])*0.5, (p1[2]+p2[2])*0.5]);
      midCache[key] = idx;
      return idx;
    }
    m.faces.forEach(tri => {
      const a = tri[0], b = tri[1], c = tri[2];
      const ab = mid(a,b), bc = mid(b,c), ca = mid(c,a);
      nf.push([a,ab,ca], [b,bc,ab], [c,ca,bc], [ab,bc,ca]);
    });
    m.verts = nv; m.faces = nf; delete m.faceColors;
  });
}

function twistActive() {
  meshes.forEach(m => {
    m.verts = m.verts.map(p => {
      const ang = p[1] * 0.65;
      const c = Math.cos(ang), s = Math.sin(ang);
      return [p[0]*c - p[2]*s, p[1], p[0]*s + p[2]*c];
    });
  });
}

function scaleActive(factor) {
  meshes.forEach(m => {
    m.verts = m.verts.map(p => [p[0]*factor, p[1]*factor, p[2]*factor]);
  });
}

let dragging = false, lastX = 0, lastY = 0, pinchDist = 0;
canvas.addEventListener('pointerdown', e => { dragging = true; lastX = e.clientX; lastY = e.clientY; });
window.addEventListener('pointerup', () => dragging = false);
window.addEventListener('pointermove', e => {
  if (!dragging) return;
  const dx = e.clientX - lastX, dy = e.clientY - lastY;
  lastX = e.clientX; lastY = e.clientY;
  camYaw += dx * 0.008;
  camPitch = Math.max(-1.45, Math.min(1.45, camPitch + dy * 0.008));
});
canvas.addEventListener('wheel', e => {
  e.preventDefault();
  camDist = Math.max(1.8, Math.min(18, camDist + e.deltaY * 0.003));
}, { passive: false });
canvas.addEventListener('touchstart', e => {
  if (e.touches.length === 2) {
    pinchDist = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY);
  }
});
canvas.addEventListener('touchmove', e => {
  if (e.touches.length === 2) {
    const d = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY);
    if (pinchDist > 0) camDist = Math.max(1.8, Math.min(18, camDist * (pinchDist / d)));
    pinchDist = d;
  }
});

function hexToRgb(hex) {
  const n = parseInt((hex || '#00E5FF').replace('#',''), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

function render() {
  const cont = document.getElementById('canvasContainer');
  const w = cont.clientWidth || window.innerWidth;
  const h = cont.clientHeight || window.innerHeight;
  ctx.clearRect(0, 0, w, h);
  if (autoSpin) camYaw += 0.012;

  const cy = Math.cos(camYaw), sy = Math.sin(camYaw);
  const cp = Math.cos(camPitch), sp = Math.sin(camPitch);
  const scale = Math.min(w, h) * 0.56 * 3.8;
  const cx = w * 0.5 + panX, centerY = h * 0.5 + panY;

  function proj(x, y, z) {
    const x1 = x * cy + z * sy;
    const z1 = -x * sy + z * cy;
    const y2 = y * cp - z1 * sp;
    const z2 = y * sp + z1 * cp + camDist;
    if (z2 < 0.2) return null;
    const f = scale / z2;
    return [cx + x1 * f, centerY - y2 * f, z2];
  }

  const gy = -1.0;
  for (let i = -8; i <= 8; i++) {
    const a = proj(-8, gy, i), b = proj(8, gy, i);
    if (a && b) {
      ctx.strokeStyle = i === 0 ? 'rgba(239,68,68,0.75)' : 'rgba(51,65,85,0.38)';
      ctx.lineWidth = i === 0 ? 2 : 1;
      ctx.beginPath(); ctx.moveTo(a[0], a[1]); ctx.lineTo(b[0], b[1]); ctx.stroke();
    }
    const c = proj(i, gy, -8), d = proj(i, gy, 8);
    if (c && d) {
      ctx.strokeStyle = i === 0 ? 'rgba(59,130,246,0.75)' : 'rgba(51,65,85,0.38)';
      ctx.lineWidth = i === 0 ? 2 : 1;
      ctx.beginPath(); ctx.moveTo(c[0], c[1]); ctx.lineTo(d[0], d[1]); ctx.stroke();
    }
  }

  // Soft Contact Shadows on Ground
  const ambRgb = hexToRgb(ambientLightColor || '#1E293B');
  meshes.forEach(m => {
    const sc = proj(0, gy + 0.005, 0);
    const spread = 0.85 + shadowSoftness * 0.45;
    const se = proj(spread, gy + 0.005, 0);
    if (sc && se) {
      const rad = Math.max(12, Math.abs(sc[0] - se[0]));
      const alpha = Math.max(0.06, (0.40 - shadowSoftness * 0.22));
      ctx.beginPath();
      ctx.ellipse(sc[0], sc[1], rad, rad * (0.38 + shadowSoftness * 0.12), 0, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(0,0,0,${'$'}{alpha.toFixed(2)})`;
      ctx.fill();
    }
  });

  const tris = [];
  let vCount = 0, tCount = 0;

  meshes.forEach(m => {
    vCount += m.verts.length;
    tCount += m.faces.length;
    const pVerts = m.verts.map(v => proj(v[0], v[1], v[2]));
    const baseRgb = hexToRgb(m.color);

    m.faces.forEach((f, fIdx) => {
      const s0 = pVerts[f[0]], s1 = pVerts[f[1]], s2 = pVerts[f[2]];
      if (!s0 || !s1 || !s2) return;
      const v0 = m.verts[f[0]], v1 = m.verts[f[1]], v2 = m.verts[f[2]];
      const ux = v1[0]-v0[0], uy = v1[1]-v0[1], uz = v1[2]-v0[2];
      const vx = v2[0]-v0[0], vy = v2[1]-v0[1], vz = v2[2]-v0[2];
      let nx = uy*vz - uz*vy, ny = uz*vx - ux*vz, nz = ux*vy - uy*vx;
      const len = Math.hypot(nx, ny, nz) || 1;
      nx /= len; ny /= len; nz /= len;

      let rgb = baseRgb;
      if (m.faceColors && m.faceColors[fIdx]) {
        rgb = hexToRgb(m.faceColors[fIdx]);
      } else if (texMode !== 'NONE') {
        const cx3 = (v0[0]+v1[0]+v2[0])/3, cy3 = (v0[1]+v1[1]+v2[1])/3, cz3 = (v0[2]+v1[2]+v2[2])/3;
        if (texMode === 'CHECKER') {
          const chk = (Math.floor(cx3*4)+Math.floor(cy3*4)+Math.floor(cz3*4)) & 1;
          if (chk) rgb = [20, 30, 48];
        } else if (texMode === 'CARBON') {
          const c = Math.sin(cx3*18)*Math.cos(cz3*18) > 0 ? 0.45 : 0.2;
          rgb = [baseRgb[0]*c, baseRgb[1]*c, baseRgb[2]*c];
        } else if (texMode === 'MARBLE') {
          const wv = 0.5 + 0.5 * Math.sin((cx3+cy3)*6 + Math.sin(cz3*5)*2);
          rgb = [baseRgb[0]*wv + 240*(1-wv), baseRgb[1]*wv + 240*(1-wv), baseRgb[2]*wv + 245*(1-wv)];
        } else if (texMode === 'STRIPES') {
          if (Math.sin(cy3*14) > 0.3) rgb = [0, 229, 255];
        }
      }

      const light = Math.max(0.18, Math.abs(nx*0.55 + ny*0.72 + nz*0.42)) * lightIntensity;
      const spec = Math.pow(Math.max(0, ny*0.7 + nz*0.6), 16) * 55 * lightIntensity;
      const ambW = 0.30;
      const r = Math.min(255, Math.max(0, Math.round(rgb[0]*light + ambRgb[0]*ambW + spec)));
      const g = Math.min(255, Math.max(0, Math.round(rgb[1]*light + ambRgb[1]*ambW + spec)));
      const b = Math.min(255, Math.max(0, Math.round(rgb[2]*light + ambRgb[2]*ambW + spec)));

      tris.push({
        s0, s1, s2,
        z: (s0[2]+s1[2]+s2[2])/3,
        fill: `rgb(${'$'}{r},${'$'}{g},${'$'}{b})`
      });
    });
  });

  tris.sort((a,b) => b.z - a.z);
  tris.forEach(t => {
    ctx.beginPath();
    ctx.moveTo(t.s0[0], t.s0[1]);
    ctx.lineTo(t.s1[0], t.s1[1]);
    ctx.lineTo(t.s2[0], t.s2[1]);
    ctx.closePath();
    ctx.fillStyle = t.fill;
    ctx.fill();
    if (showWire) {
      ctx.strokeStyle = 'rgba(0,229,255,0.55)';
      ctx.lineWidth = 0.9;
      ctx.stroke();
    }
  });

  document.getElementById('statsText').textContent = vCount + ' Verts • ' + tCount + ' Tris';
  requestAnimationFrame(render);
}
requestAnimationFrame(render);

function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1500);
}

function downloadOBJ() {
  let out = '# PolyForge 3D Web Export\n';
  let offset = 1;
  meshes.forEach(m => {
    out += 'o ' + (m.name || 'Mesh') + '\n';
    m.verts.forEach(v => { out += `v ${'$'}{v[0].toFixed(5)} ${'$'}{v[1].toFixed(5)} ${'$'}{v[2].toFixed(5)}\n`; });
    m.faces.forEach(f => { out += `f ${'$'}{f[0]+offset} ${'$'}{f[1]+offset} ${'$'}{f[2]+offset}\n`; });
    offset += m.verts.length;
  });
  triggerDownload(new Blob([out], {type: 'model/obj'}), '__PROJECT_NAME__.obj');
}

function downloadSTL() {
  let totalTris = 0;
  meshes.forEach(m => totalTris += m.faces.length);
  const buf = new ArrayBuffer(84 + totalTris * 50);
  const dv = new DataView(buf);
  dv.setUint32(80, totalTris, true);
  let pos = 84;
  const scaleMm = 20.0;
  meshes.forEach(m => {
    m.faces.forEach(f => {
      const v0 = m.verts[f[0]], v1 = m.verts[f[1]], v2 = m.verts[f[2]];
      [0,0,1, v0[0]*scaleMm,v0[1]*scaleMm,v0[2]*scaleMm, v1[0]*scaleMm,v1[1]*scaleMm,v1[2]*scaleMm, v2[0]*scaleMm,v2[1]*scaleMm,v2[2]*scaleMm].forEach(val => {
        dv.setFloat32(pos, val, true); pos += 4;
      });
      dv.setUint16(pos, 0, true); pos += 2;
    });
  });
  triggerDownload(new Blob([buf], {type: 'model/stl'}), '__PROJECT_NAME__.stl');
}

function downloadGLB() {
  const allPos = [];
  let minX=1e9, minY=1e9, minZ=1e9, maxX=-1e9, maxY=-1e9, maxZ=-1e9;
  meshes.forEach(m => {
    m.faces.forEach(f => {
      [m.verts[f[0]], m.verts[f[1]], m.verts[f[2]]].forEach(v => {
        allPos.push(v[0], v[1], v[2]);
        if (v[0]<minX) minX=v[0]; if (v[0]>maxX) maxX=v[0];
        if (v[1]<minY) minY=v[1]; if (v[1]>maxY) maxY=v[1];
        if (v[2]<minZ) minZ=v[2]; if (v[2]>maxZ) maxZ=v[2];
      });
    });
  });
  const floatArr = new Float32Array(allPos);
  const binBytes = new Uint8Array(floatArr.buffer);
  const gltf = {
    asset: { version: "2.0", generator: "PolyForge 3D Web Studio" },
    scene: 0, scenes: [{ nodes: [0] }], nodes: [{ mesh: 0, name: "__PROJECT_NAME__" }],
    meshes: [{ primitives: [{ attributes: { POSITION: 0 }, mode: 4 }] }],
    buffers: [{ byteLength: binBytes.byteLength }],
    bufferViews: [{ buffer: 0, byteOffset: 0, byteLength: binBytes.byteLength, target: 34962 }],
    accessors: [{ bufferView: 0, componentType: 5126, count: allPos.length / 3, type: "VEC3", min: [minX,minY,minZ], max: [maxX,maxY,maxZ] }]
  };
  let jsonStr = JSON.stringify(gltf);
  while (jsonStr.length % 4 !== 0) jsonStr += ' ';
  const jsonBytes = new TextEncoder().encode(jsonStr);
  const binPad = (4 - (binBytes.byteLength % 4)) % 4;
  const totalLen = 12 + 8 + jsonBytes.byteLength + 8 + binBytes.byteLength + binPad;
  const out = new ArrayBuffer(totalLen);
  const dv = new DataView(out);
  dv.setUint32(0, 0x46546C67, true);
  dv.setUint32(4, 2, true);
  dv.setUint32(8, totalLen, true);
  dv.setUint32(12, jsonBytes.byteLength, true);
  dv.setUint32(16, 0x4E4F534A, true);
  new Uint8Array(out, 20, jsonBytes.byteLength).set(jsonBytes);
  const binStart = 20 + jsonBytes.byteLength;
  dv.setUint32(binStart, binBytes.byteLength + binPad, true);
  dv.setUint32(binStart + 4, 0x004E4942, true);
  new Uint8Array(out, binStart + 8, binBytes.byteLength).set(binBytes);
  triggerDownload(new Blob([out], {type: 'model/gltf-binary'}), '__PROJECT_NAME__.glb');
}
</script>
</body>
</html>"""

    private fun round3(v: Float): Double =
        kotlin.math.round(v * 1000.0) / 1000.0
}
