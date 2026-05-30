package com.android.purebilibili.navigation

import androidx.lifecycle.Lifecycle
import com.android.purebilibili.navigation3.BiliPaiNavKey

internal fun shouldStopPlaybackEagerlyOnVideoRouteExit(
    fromRoute: String?,
    toRoute: String?
): Boolean {
    if (toRoute.isNullOrBlank()) return false
    return isVideoDetailRoute(fromRoute) &&
        !isVideoDetailRoute(toRoute) &&
        toRoute != ScreenRoutes.AudioMode.route
}

internal fun shouldDeferBottomBarRevealOnVideoReturn(
    isReturningFromDetail: Boolean,
    currentRoute: String?,
    cardTransitionEnabled: Boolean
): Boolean {
    // 仅在共享缩放路径下延迟显示底栏；card 关闭（横向滑动）时首页连同底栏整体滑入，不应延后弹出。
    if (!cardTransitionEnabled) return false
    if (!isReturningFromDetail) return false
    return currentRoute in setOf(ScreenRoutes.Home.route, "main_host")
}

internal fun shouldClearReturningStateWhenDisposingVideoDestination(
    stillInVideoRoute: Boolean
): Boolean {
    return stillInVideoRoute
}

internal fun isVideoCardReturnTargetRoute(route: String?): Boolean {
    val routeBase = route?.substringBefore("?") ?: return false
    return routeBase == "main_host" ||
        routeBase == ScreenRoutes.Home.route ||
        routeBase == ScreenRoutes.History.route ||
        routeBase == ScreenRoutes.Favorite.route ||
        routeBase == ScreenRoutes.WatchLater.route ||
        routeBase == ScreenRoutes.Search.route ||
        routeBase == ScreenRoutes.Dynamic.route ||
        routeBase.startsWith("dynamic_detail/") ||
        routeBase == ScreenRoutes.Partition.route ||
        routeBase.startsWith("category/") ||
        routeBase.startsWith("season_series_detail/") ||
        routeBase.startsWith("space/")
}

internal fun isVideoDetailRoute(route: String?): Boolean {
    return route?.startsWith("${VideoRoute.base}/") == true
}

internal fun shouldEnableVideoDetailSharedTransition(
    cardTransitionEnabled: Boolean
): Boolean {
    return cardTransitionEnabled
}

internal fun shouldShareAudioModeViewModelWithPreviousEntry(
    previousRoute: String?,
    previousLifecycleState: Lifecycle.State?
): Boolean {
    return previousLifecycleState?.isAtLeast(Lifecycle.State.CREATED) == true &&
        isVideoDetailRoute(previousRoute)
}

internal fun shouldNavigateAudioModeBackToCurrentVideo(
    previousVideoBvid: String?,
    currentVideoBvid: String
): Boolean {
    val normalizedCurrentBvid = currentVideoBvid.trim()
    if (normalizedCurrentBvid.isEmpty()) return false
    return previousVideoBvid?.trim() != normalizedCurrentBvid
}

internal data class AudioModeInitialLoadRequest(
    val bvid: String,
    val cid: Long,
    val resumePositionMs: Long
)

internal fun resolveAudioModeInitialLoadRequest(
    key: BiliPaiNavKey.AudioMode,
    hasDisplayState: Boolean
): AudioModeInitialLoadRequest? {
    if (hasDisplayState) return null
    val sourceBvid = key.sourceBvid.trim()
    if (sourceBvid.isEmpty()) return null
    return AudioModeInitialLoadRequest(
        bvid = sourceBvid,
        cid = key.sourceCid.coerceAtLeast(0L),
        resumePositionMs = key.sourceResumePositionMs.coerceAtLeast(0L)
    )
}
