package com.android.purebilibili.feature.audio.library

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavoriteData
import com.android.purebilibili.data.model.response.FavoriteResourceData
import com.android.purebilibili.data.model.response.Upper
import com.android.purebilibili.feature.audio.viewmodel.ListenVideoViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListenVideoLibraryLoaderTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `folder loader follows pagination until has more is false`() = runTest {
        val source = FakeListenVideoDataSource(
            folderPages = mapOf(
                1L to mapOf(
                    1 to Result.success(page("BV1", hasMore = true)),
                    2 to Result.success(page("BV2", hasMore = false))
                )
            )
        )

        val result = ListenVideoLibraryLoader(source).loadFolder(1L)

        assertEquals(listOf("BV1", "BV2"), result.getOrThrow().map { it.bvid })
        assertEquals(listOf(1, 2), source.requestedFolderPages)
    }

    @Test
    fun `index keeps successful folders when one folder fails`() = runTest {
        val source = FakeListenVideoDataSource(
            folderPages = mapOf(
                1L to mapOf(1 to Result.success(page("BV1", hasMore = false))),
                2L to mapOf(1 to Result.failure(IOException("offline")))
            )
        )

        val result = ListenVideoLibraryLoader(source, maxConcurrentFolders = 3).indexFolders(
            listOf(FavFolder(id = 1L, title = "A"), FavFolder(id = 2L, title = "B"))
        )

        assertEquals(listOf("BV1"), result.resources.map { it.bvid })
        assertEquals(setOf(2L), result.failedFolderIds)
    }

    @Test
    fun `index progress counts completed folders even when they finish out of order`() = runTest {
        val source = GatedListenVideoDataSource()
        val progress = mutableListOf<Int>()
        val result = async {
            ListenVideoLibraryLoader(source, maxConcurrentFolders = 2).indexFolders(
                folders = listOf(FavFolder(id = 1L), FavFolder(id = 2L)),
                onFolderIndexed = { completed, _ -> progress += completed }
            )
        }
        runCurrent()

        source.secondGate.complete(Unit)
        runCurrent()
        source.firstGate.complete(Unit)
        result.await()

        assertEquals(listOf(1, 2), progress)
    }

    @Test
    fun `new refresh cancels old generation and only latest result is published`() = runTest {
        val source = RestartingListenVideoDataSource()
        val viewModel = ListenVideoViewModel(source, currentMid = { 42L })

        viewModel.refresh()
        runCurrent()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.generation)
        assertEquals(listOf("Latest"), viewModel.uiState.value.playlists.map { it.title })
        assertTrue(source.firstRequestCancelled)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `logged out refresh skips remote requests`() = runTest {
        val source = FakeListenVideoDataSource()
        val viewModel = ListenVideoViewModel(source, currentMid = { 0L })

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertEquals(0, source.ownedFolderRequests)
    }

    private fun page(bvid: String, hasMore: Boolean): FavoriteResourceData {
        return FavoriteResourceData(
            medias = listOf(
                FavoriteData(
                    id = bvid.hashCode().toLong(),
                    bvid = bvid,
                    title = bvid,
                    upper = Upper(mid = 7L, name = "Artist")
                )
            ),
            has_more = hasMore
        )
    }
}

private open class FakeListenVideoDataSource(
    private val folderPages: Map<Long, Map<Int, Result<FavoriteResourceData>>> = emptyMap()
) : ListenVideoLibraryDataSource {
    var ownedFolderRequests = 0
        private set
    val requestedFolderPages = mutableListOf<Int>()

    override suspend fun ownedFolders(mid: Long): Result<List<FavFolder>> {
        ownedFolderRequests += 1
        return Result.success(emptyList())
    }

    override suspend fun collectedFolders(mid: Long, page: Int): Result<ListenVideoCollectedFoldersPage> {
        return Result.success(ListenVideoCollectedFoldersPage(emptyList(), hasMore = false))
    }

    override suspend fun folderPage(mediaId: Long, page: Int): Result<FavoriteResourceData> {
        requestedFolderPages += page
        return folderPages[mediaId]?.get(page)
            ?: Result.failure(IllegalStateException("missing folder page $mediaId/$page"))
    }

    override suspend fun albumPage(seasonId: Long, page: Int): Result<FavoriteResourceData> {
        return Result.failure(IllegalStateException("unused"))
    }
}

private class RestartingListenVideoDataSource : ListenVideoLibraryDataSource {
    private var ownedCalls = 0
    var firstRequestCancelled = false
        private set

    override suspend fun ownedFolders(mid: Long): Result<List<FavFolder>> {
        ownedCalls += 1
        if (ownedCalls == 1) {
            try {
                awaitCancellation()
            } finally {
                firstRequestCancelled = true
            }
        }
        return Result.success(listOf(FavFolder(id = 1L, title = "Latest")))
    }

    override suspend fun collectedFolders(mid: Long, page: Int): Result<ListenVideoCollectedFoldersPage> {
        return Result.success(ListenVideoCollectedFoldersPage(emptyList(), hasMore = false))
    }

    override suspend fun folderPage(mediaId: Long, page: Int): Result<FavoriteResourceData> {
        return Result.success(FavoriteResourceData())
    }

    override suspend fun albumPage(seasonId: Long, page: Int): Result<FavoriteResourceData> {
        return Result.success(FavoriteResourceData())
    }
}

private class GatedListenVideoDataSource : ListenVideoLibraryDataSource {
    val firstGate = CompletableDeferred<Unit>()
    val secondGate = CompletableDeferred<Unit>()

    override suspend fun ownedFolders(mid: Long): Result<List<FavFolder>> = Result.success(emptyList())

    override suspend fun collectedFolders(mid: Long, page: Int): Result<ListenVideoCollectedFoldersPage> {
        return Result.success(ListenVideoCollectedFoldersPage(emptyList(), hasMore = false))
    }

    override suspend fun folderPage(mediaId: Long, page: Int): Result<FavoriteResourceData> {
        if (mediaId == 1L) firstGate.await() else secondGate.await()
        return Result.success(FavoriteResourceData(has_more = false))
    }

    override suspend fun albumPage(seasonId: Long, page: Int): Result<FavoriteResourceData> {
        return Result.failure(IllegalStateException("unused"))
    }
}
