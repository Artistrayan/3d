package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.FileExporterImporter
import com.example.engine.PrimitiveGenerator
import com.example.engine.PrimitiveType3D
import com.example.model.ExportFormat3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PolyForge 3D", appName)
    }

    @Test
    fun glbExport_hasValidGlTF2BinaryHeader() {
        val cube = PrimitiveGenerator.createPrimitive(PrimitiveType3D.CUBE)
        val payload = FileExporterImporter.generateExport(listOf(cube), ExportFormat3D.GLB, "test_cube")

        assertTrue(payload.bytes.size > 100)
        val buf = ByteBuffer.wrap(payload.bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.int
        val version = buf.int
        val totalLen = buf.int

        assertEquals(0x46546C67, magic) // "glTF"
        assertEquals(2, version)
        assertEquals(payload.bytes.size, totalLen)
    }
}
