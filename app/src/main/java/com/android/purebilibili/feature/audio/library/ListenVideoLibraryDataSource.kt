package com.android.purebilibili.feature.audio.library

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavoriteData
import com.android.purebilibili.data.model.response.FavoriteResourceData
import com.android.purebilibili.data.repository.FavoriteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

private const val LISTEN_VIDEO_PAGE_SIZE = 40

internal data class ListenVideoCollectedFoldersPage(
    val folders: List<FavFolder>,
    val hasMore: Boolean
)

internal data class ListenVideoIndexResult(
    val resources: List<FavoriteData>,
    val failedFolderIds: Set<Long>
)

internal interface ListenVideoLibraryDataSource {
    suspend fun ownedFolders(mid: Long): Result<List<FavFolder>>

    suspend fun collectedFolders(mid: Long, page: Int): Result<ListenVideoCollectedFoldersPage>

    suspend fun folderPage(mediaId: Long, page: Int): Result<FavoriteResourceData>

    suspend fun albumPage(seasonId: Long, page: Int): Result<FavoriteResourceData>
}

internal class BilibiliListenVideoLibraryDataSource : ListenVideoLibraryDataSource {
    override suspend fun ownedFolders(mid: Long): Result<List<FavFolder>> {
        return FavoriteRepository.getFavFolders(mid)
    }

    override suspend fun collectedFolders(
        mid: Long,
        page: Int
    ): Result<ListenVideoCollectedFoldersPage> {
        return FavoriteRepository.getCollectedFavFolders(
            mid = mid,
            pn = page,
            ps = LISTEN_VIDEO_PAGE_SIZE,
            platform = "web"
        ).map { result ->
            ListenVideoCollectedFoldersPage(
                folders = result.folders,
                hasMore = if (result.totalCount > 0) {
                    page * LISTEN_VIDEO_PAGE_SIZE < result.totalCount
                } else {
                    result.folders.size >= LISTEN_VIDEO_PAGE_SIZE
                }
            )
        }
    }

    override suspend fun folderPage(
        mediaId: Long,
        page: Int
    ): Result<FavoriteResourceData> {
        return FavoriteRepository.getFavoriteList(
            mediaId = mediaId,
            pn = page,
            ps = LISTEN_VIDEO_PAGE_SIZE
        )
    }

    override suspend fun albumPage(
        seasonId: Long,
        page: Int
    ): Result<FavoriteResourceData> {
        return FavoriteRepository.getFavoriteSeasonList(seasonId = seasonId, pn = page)
    }
}

internal class ListenVideoLibraryLoader(
    private val source: ListenVideoLibraryDataSource,
    maxConcurrentFolders: Int = 3
) {
    private val folderSemaphore = Semaphore(maxConcurrentFolders.coerceAtLeast(1))

    suspend fun loadCollectedFolders(mid: Long): Result<List<FavFolder>> {
        return loadPages { page -> source.collectedFolders(mid, page) }
    }

    suspend fun loadFolder(mediaId: Long): Result<List<FavoriteData>> {
        return loadResourcePages { page -> source.folderPage(mediaId, page) }
    }

    suspend fun loadAlbum(seasonId: Long): Result<List<FavoriteData>> {
        return loadResourcePages { page -> source.albumPage(seasonId, page) }
    }

    suspend fun indexFolders(
        folders: List<FavFolder>,
        onFolderIndexed: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): ListenVideoIndexResult = supervisorScope {
        val validFolders = folders.filter { it.id > 0L }
        val completedCount = AtomicInteger(0)
        val results = validFolders.map { folder ->
            async {
                folderSemaphore.withPermit {
                    val result = folder.id to loadFolder(folder.id)
                    onFolderIndexed(completedCount.incrementAndGet(), validFolders.size)
                    result
                }
            }
        }.awaitAll()

        ListenVideoIndexResult(
            resources = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
                .distinctBy { resource ->
                    if (resource.type == 21) {
                        "album:${resource.season_id.takeIf { it > 0L } ?: resource.id}"
                    } else {
                        "track:${resource.bvid.ifBlank { resource.bv_id }}"
                    }
                },
            failedFolderIds = results.mapNotNull { (folderId, result) ->
                folderId.takeIf { result.isFailure }
            }.toSet()
        )
    }

    private suspend fun loadPages(
        request: suspend (Int) -> Result<ListenVideoCollectedFoldersPage>
    ): Result<List<FavFolder>> {
        return try {
            val folders = mutableListOf<FavFolder>()
            var page = 1
            do {
                val response = request(page).getOrThrow()
                folders += response.folders
                page += 1
            } while (response.hasMore)
            Result.success(folders.distinctBy { it.id })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private suspend fun loadResourcePages(
        request: suspend (Int) -> Result<FavoriteResourceData>
    ): Result<List<FavoriteData>> {
        return try {
            val resources = mutableListOf<FavoriteData>()
            var page = 1
            do {
                val response = request(page).getOrThrow()
                resources += response.medias.orEmpty()
                page += 1
            } while (response.has_more)
            Result.success(resources)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }
}
