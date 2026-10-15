package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ManifoldEmerald
import com.example.ui.theme.SculptAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import java.io.File

private class WebViewExportBridge(private val context: Context) {
    @JavascriptInterface
    fun saveBase64File(base64OrDataUrl: String, fileName: String, mimeType: String) {
        runCatching {
            val cleanBase64 = base64OrDataUrl.substringAfter("base64,", base64OrDataUrl)
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "PolyForge_Export.glb" }
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, safeName)
            outFile.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType.ifBlank { "application/octet-stream" }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, safeName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "ذخیره و اشتراک‌گذاری ($safeName)")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { err ->
            Toast.makeText(context, "خطا در ذخیره فایل: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebStudioPreviewDialog(
    isPersian: Boolean,
    projectName: String,
    htmlContent: String,
    apkSizeKb: Long = 0L,
    onDismiss: () -> Unit,
    onSaveWebHtmlFile: (String) -> Unit,
    onShareWebHtmlFile: () -> Unit,
    onSaveAndroidApkFile: (String) -> Unit = {},
    onShareAndroidApkFile: () -> Unit = {}
) {
    val context = LocalContext.current
    // Use a non-state reference box so opening the file chooser never triggers recomposition or reloads WebView
    val filePathCallbackRef = remember { arrayOfNulls<ValueCallback<Array<Uri>>>(1) }

    val webFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val callback = filePathCallbackRef[0]
        filePathCallbackRef[0] = null
        if (uri != null) {
            callback?.onReceiveValue(arrayOf(uri))
        } else {
            callback?.onReceiveValue(null)
        }
    }

    val baseDensity = LocalDensity.current
    val fixedDensity = remember(baseDensity.density) {
        Density(density = baseDensity.density, fontScale = 1f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalDensity provides fixedDensity
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioObsidian)
            ) {
                // Top bar for the Web 3D Studio Preview & HTML/APK File Exporter
                Surface(
                    color = StudioSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Web & APK Studio",
                                    tint = ManifoldEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isPersian)
                                            "نسخه وب (.HTML) و فایل نصبی اندروید (.APK)"
                                        else
                                            "Web 3D Studio (.HTML) & Android (.APK)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isPersian)
                                            "۱۰۰٪ کامل با تمام منوها، وارد کردن فایل/عکس و خروجی سه‌بعدی"
                                        else
                                            "100% Complete Studio with File/Photo Import & 3D Exporters",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ManifoldEmerald
                                    )
                                }
                            }

                            Surface(
                                color = StudioSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                                    .clickable(onClick = onDismiss)
                                    .testTag("close_web_preview_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPersian) "بستن" else "Close",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 1: Web HTML File Save & Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSaveWebHtmlFile("${projectName}_web3d.html") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ManifoldEmerald,
                                    contentColor = Color(0xFF002417)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("save_web_html_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isPersian) "ذخیره فایل وب (.HTML)" else "Save .HTML",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onShareWebHtmlFile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SculptAmber,
                                    contentColor = Color(0xFF241100)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("share_web_html_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isPersian) "ارسال فایل وب (.HTML)" else "Share .HTML",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Row 2: Android .APK File Save & Share
                        val apkSizeLabel = if (apkSizeKb > 0) " (${apkSizeKb / 1024} MB)" else ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSaveAndroidApkFile("PolyForge_3D_Studio_v2.0.apk") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = Color(0xFF00242B)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("save_android_apk_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isPersian) "ذخیره فایل اندروید .APK$apkSizeLabel" else "Save .APK$apkSizeLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = onShareAndroidApkFile,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF082F49)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("share_android_apk_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isPersian) "ارسال نصبی اندروید (.APK)" else "Share .APK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Live Interactive HTML5 + 3D Canvas WebView
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.textZoom = 100
                                addJavascriptInterface(WebViewExportBridge(context), "AndroidBridge")
                                webViewClient = WebViewClient()
                                webChromeClient = object : WebChromeClient() {
                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallbackParam: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        filePathCallbackRef[0]?.onReceiveValue(null)
                                        filePathCallbackRef[0] = filePathCallbackParam
                                        webFilePickerLauncher.launch(arrayOf("*/*"))
                                        return true
                                    }
                                }
                                tag = htmlContent
                                loadDataWithBaseURL(
                                    "https://polyforge3d.local/",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        update = { webView ->
                            if (webView.tag != htmlContent) {
                                webView.tag = htmlContent
                                webView.loadDataWithBaseURL(
                                    "https://polyforge3d.local/",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
