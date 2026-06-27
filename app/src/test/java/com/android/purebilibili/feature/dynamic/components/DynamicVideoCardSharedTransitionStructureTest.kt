package com.android.purebilibili.feature.dynamic.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DynamicVideoCardSharedTransitionStructureTest {

    @Test
    fun dynamicVideoCard_usesSeparateCoverAndTitleSharedElements() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/components/VideoCards.kt")
            .readText()

        assertTrue(source.contains("videoCoverSharedElementKey(archive.bvid"))
        assertTrue(source.contains("videoTitleSharedElementKey("))
        assertTrue(source.contains("sourceRoute = sourceRoute"))
        assertTrue(source.contains("VideoCardLargeCover("))
        assertTrue(source.contains("titleModifier = titleModifier"))
    }

    @Test
    fun dynamicVideoCard_recordsCoverBoundsForReturnTarget() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/components/VideoCards.kt")
            .readText()

        assertTrue(source.contains("val coverBoundsRef = remember"))
        assertTrue(source.contains("coverBoundsRef.value?.let { bounds ->"))
        assertTrue(source.contains("modifier = coverModifier.onGloballyPositioned"))
    }

    @Test
    fun dynamicVideoCard_obeysGlobalSharedTransitionSwitch() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/components/VideoCards.kt")
            .readText()

        assertTrue(source.contains("val sharedTransitionEnabled = LocalSharedTransitionEnabled.current"))
        assertTrue(source.contains("val sharedElementReady = sharedTransitionEnabled &&"))
        assertTrue(source.contains("transitionEnabled = sharedTransitionEnabled"))
    }
}
