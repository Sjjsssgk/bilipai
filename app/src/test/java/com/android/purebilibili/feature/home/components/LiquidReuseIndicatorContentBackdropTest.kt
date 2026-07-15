package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import top.yukonga.miuix.kmp.blur.Backdrop

class LiquidReuseIndicatorContentBackdropTest {

    private object PageBackdrop : Backdrop {
        override val isCoordinatesDependent: Boolean = false
        override fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdrop(
            density: androidx.compose.ui.unit.Density,
            coordinates: androidx.compose.ui.layout.LayoutCoordinates?,
            layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?,
            downscaleFactor: Int,
        ) = Unit
    }

    private object ExportBackdrop : Backdrop {
        override val isCoordinatesDependent: Boolean = false
        override fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdrop(
            density: androidx.compose.ui.unit.Density,
            coordinates: androidx.compose.ui.layout.LayoutCoordinates?,
            layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?,
            downscaleFactor: Int,
        ) = Unit
    }

    private object Combined : Backdrop {
        override val isCoordinatesDependent: Boolean = false
        override fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdrop(
            density: androidx.compose.ui.unit.Density,
            coordinates: androidx.compose.ui.layout.LayoutCoordinates?,
            layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?,
            downscaleFactor: Int,
        ) = Unit
    }

    private object CoordinateDependentPageBackdrop : Backdrop {
        override val isCoordinatesDependent: Boolean = true
        override fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdrop(
            density: androidx.compose.ui.unit.Density,
            coordinates: androidx.compose.ui.layout.LayoutCoordinates?,
            layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?,
            downscaleFactor: Int,
        ) = Unit
    }

    @Test
    fun inContentReuseFallsBackWhenPageBackdropDoesNotOverlap() {
        assertSame(
            ExportBackdrop,
            resolveInContentLiquidSamplingBackdrop(
                pageBackdrop = CoordinateDependentPageBackdrop,
                fallbackBackdrop = ExportBackdrop,
            )
        )
    }

    @Test
    fun inContentReuseKeepsCoordinateIndependentBackdrop() {
        assertSame(
            PageBackdrop,
            resolveInContentLiquidSamplingBackdrop(
                pageBackdrop = PageBackdrop,
                fallbackBackdrop = ExportBackdrop,
            )
        )
    }

    @Test
    fun inContentReuseUsesStableFallbackWithoutPageBackdrop() {
        assertSame(
            ExportBackdrop,
            resolveInContentLiquidSamplingBackdrop(
                pageBackdrop = null,
                fallbackBackdrop = ExportBackdrop,
            )
        )
    }

    @Test
    fun prefersCombinedWhenPageExportAndCombinedProvided() {
        val result = resolveLiquidReuseIndicatorContentBackdrop(
            pageBackdrop = PageBackdrop,
            exportBackdrop = ExportBackdrop,
            useCombined = true,
            combinedBackdrop = Combined,
        )
        assertSame(Combined, result)
    }

    @Test
    fun prefersExportWhenCombinedNotRequested() {
        // TopBar / v9.9.7 path: BILIPAI samples export capture (surface-filled), not page.
        val result = resolveLiquidReuseIndicatorContentBackdrop(
            pageBackdrop = PageBackdrop,
            exportBackdrop = ExportBackdrop,
            useCombined = false,
            combinedBackdrop = Combined,
        )
        assertSame(ExportBackdrop, result)
    }

    @Test
    fun fallsBackToExportWhenCombinedMissing() {
        val result = resolveLiquidReuseIndicatorContentBackdrop(
            pageBackdrop = null,
            exportBackdrop = ExportBackdrop,
            useCombined = true,
            combinedBackdrop = null,
        )
        assertSame(ExportBackdrop, result)
    }

    @Test
    fun returnsNullWhenNoSampleSource() {
        val result = resolveLiquidReuseIndicatorContentBackdrop(
            pageBackdrop = null,
            exportBackdrop = null,
            useCombined = true,
            combinedBackdrop = null,
        )
        assertNull(result)
    }

    @Test
    fun prefersPageOnlyWhenExportMissing() {
        val result = resolveLiquidReuseIndicatorContentBackdrop(
            pageBackdrop = PageBackdrop,
            exportBackdrop = null,
            useCombined = false,
            combinedBackdrop = null,
        )
        assertSame(PageBackdrop, result)
    }
}
