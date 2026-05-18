package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedScrollStatePersistenceStructureTest {

    @Test
    fun `home category grid states are saveable per category`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")

        assertTrue(source.contains("rememberSaveable("))
        assertTrue(source.contains("category.name"))
        assertTrue(source.contains("saver = LazyGridState.Saver"))
        assertTrue(source.contains("LazyGridState()"))
        assertFalse(source.contains("gridStates[category] = rememberLazyGridState()"))
    }

    @Test
    fun `home skin atmosphere is fixed in header instead of pager backdrop`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val headerCallSource = source
            .substringAfter("iOSHomeHeader(")
            .substringBefore("AnimatedVisibility(")

        assertTrue(source.contains("val uiSkinState by rememberUiSkinState(context)"))
        assertTrue(source.contains("val homeUiSkinDecoration = rememberHomeUiSkinDecoration(uiSkinState)"))
        assertTrue(headerCallSource.contains("uiSkinDecoration = homeUiSkinDecoration"))
        assertFalse(source.contains("import com.android.purebilibili.feature.home.components.HomeSkinAtmosphere"))
        assertFalse(source.contains("HomeSkinAtmosphere(\n                        decoration = homeUiSkinDecoration"))
    }

    @Test
    fun `home skin feed atmosphere is drawn once behind grid container`() {
        val screenSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val pageSource = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt")
        val pageCallSource = screenSource
            .substringAfter("HomeCategoryPageContent(")
            .substringBefore("firstGridItemModifier = Modifier")
        val pageFunctionSource = pageSource
            .substringAfter("internal fun HomeCategoryPageContent(")
            .substringBefore("@Composable\nprivate fun PopularSubCategorySegmentedControl")
        val gridContainerSource = pageFunctionSource
            .substringAfter("val feedAtmosphereImagePath = resolveHomeFeedSkinAtmosphereImagePath(uiSkinDecoration)")
            .substringBefore("LazyVerticalGrid(")
        val videoItemSource = pageFunctionSource
            .substringAfter("categoryState.videos.forEachIndexed")

        assertTrue(pageCallSource.contains("uiSkinDecoration = homeUiSkinDecoration"))
        assertTrue(pageFunctionSource.contains("uiSkinDecoration: HomeUiSkinDecoration? = null"))
        assertTrue(pageFunctionSource.contains("val feedAtmosphereImagePath = resolveHomeFeedSkinAtmosphereImagePath(uiSkinDecoration)"))
        assertTrue(gridContainerSource.contains("AsyncImage("))
        assertTrue(gridContainerSource.contains("model = File(feedAtmosphereImagePath)"))
        assertFalse(videoItemSource.contains("resolveHomeFeedSkinAtmosphereImagePath(uiSkinDecoration)"))
        assertFalse(videoItemSource.contains("model = File(feedAtmosphereImagePath)"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
