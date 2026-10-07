package com.example

import com.example.engine.FileExporterImporter
import com.example.engine.MeshModifiers
import com.example.engine.PrimitiveGenerator
import com.example.engine.PrimitiveType3D
import com.example.model.ExportFormat3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun binaryStlExport_matchesExactSpecByteLength() {
        val gear = PrimitiveGenerator.createPrimitive(PrimitiveType3D.GEAR)
        val payload = FileExporterImporter.generateExport(listOf(gear), ExportFormat3D.STL_BINARY, "spur_gear")

        val expectedBytes = 80 + 4 + (gear.faces.size * 50)
        assertEquals(expectedBytes, payload.bytes.size)

        // Verify roundtrip import of Binary STL
        val imported = FileExporterImporter.import3DFile("spur_gear.stl", payload.bytes)
        assertEquals(1, imported.size)
        assertEquals(gear.faces.size, imported.first().faces.size)
    }

    @Test
    fun objAndPlyExport_roundtripCorrectly() {
        val pyramid = PrimitiveGenerator.createPrimitive(PrimitiveType3D.PYRAMID)
        val objPayload = FileExporterImporter.generateExport(listOf(pyramid), ExportFormat3D.OBJ, "pyramid")
        val importedObj = FileExporterImporter.import3DFile("pyramid.obj", objPayload.bytes)
        assertEquals(1, importedObj.size)
        assertEquals(pyramid.faces.size, importedObj.first().faces.size)
    }

    @Test
    fun subdivisionAndEngineeringStats_increaseDetailValidly() {
        val ico = PrimitiveGenerator.createPrimitive(PrimitiveType3D.ICOSPHERE)
        val initialTris = ico.faces.size
        val subdivided = MeshModifiers.subdivideMesh(ico, smooth = true)

        assertEquals(initialTris * 4, subdivided.faces.size)
        val stats = MeshModifiers.computeEngineeringStats(listOf(subdivided))
        assertTrue(stats.widthMm > 0f)
        assertTrue(stats.volumeCm3 > 0f)
    }
}
