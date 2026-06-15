package com.android.purebilibili.feature.search

internal sealed interface SearchResultNavigationTarget {
    data class Video(val bvid: String) : SearchResultNavigationTarget

    data class Web(
        val url: String,
        val title: String
    ) : SearchResultNavigationTarget

    data class LiveRoom(
        val roomId: Long,
        val title: String,
        val uname: String
    ) : SearchResultNavigationTarget

    data class Space(val mid: Long) : SearchResultNavigationTarget

    data object None : SearchResultNavigationTarget
}

internal fun resolveVideoSearchNavigationTarget(
    bvid: String,
    contentType: String,
    navigationUrl: String,
    title: String
): SearchResultNavigationTarget {
    val normalizedBvid = bvid.trim()
    if (normalizedBvid.isNotEmpty()) {
        return SearchResultNavigationTarget.Video(normalizedBvid)
    }

    val normalizedUrl = navigationUrl.trim()
    val isSupportedWebUrl = normalizedUrl.startsWith("https://") ||
        normalizedUrl.startsWith("http://")
    return if (contentType.equals("ketang", ignoreCase = true) && isSupportedWebUrl) {
        SearchResultNavigationTarget.Web(
            url = normalizedUrl,
            title = title.trim().ifBlank { "课堂" }
        )
    } else {
        SearchResultNavigationTarget.None
    }
}

internal fun resolveLiveUserSearchNavigationTarget(
    roomId: Long,
    uid: Long,
    isLive: Boolean,
    title: String,
    uname: String
): SearchResultNavigationTarget {
    return if (isLive && roomId > 0L) {
        SearchResultNavigationTarget.LiveRoom(
            roomId = roomId,
            title = title.ifBlank { uname },
            uname = uname
        )
    } else if (uid > 0L) {
        SearchResultNavigationTarget.Space(mid = uid)
    } else {
        SearchResultNavigationTarget.None
    }
}

internal fun resolvePhotoSearchNavigationTarget(): SearchResultNavigationTarget {
    return SearchResultNavigationTarget.None
}
