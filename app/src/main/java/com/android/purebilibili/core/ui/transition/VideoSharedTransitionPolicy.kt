package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

internal enum class VideoSharedTransitionProfile {
    COVER_ONLY,
    COVER_AND_METADATA
}

internal const val VIDEO_SHARED_COVER_ASPECT_RATIO = 16f / 10f
private const val HOME_SOURCE_ROUTE = "home"
private const val HOME_SHARED_TRANSITION_DURATION_MILLIS = 360
private const val HOME_DETAIL_REVEAL_DELAY_MILLIS = 40
private const val HOME_DETAIL_REVEAL_DURATION_MILLIS = 220
private const val HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP = 14
private const val HOME_DETAIL_REVEAL_INITIAL_SCALE = 0.985f
private const val HOME_SHARED_TRANSITION_CARD_CORNER_DP = 16
private const val HOME_SHARED_TRANSITION_PLAYER_CORNER_DP = 12
private val VIDEO_CARD_IOS_LIKE_EASE_OUT = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

internal data class VideoSharedTransitionOwnership(
    val useCardShellSharedBounds: Boolean,
    val useCoverSharedBounds: Boolean,
    val useTitleSharedBounds: Boolean,
    val useAvatarSharedBounds: Boolean,
    val useUpNameSharedBounds: Boolean,
    val useStatsSharedBounds: Boolean
)

internal data class VideoSharedTransitionMotionSpec(
    val enabled: Boolean,
    val durationMillis: Int,
    val contentDelayMillis: Int,
    val contentDurationMillis: Int,
    val contentSlideOffsetDp: Int,
    val contentInitialScale: Float,
    val easing: Easing
)

internal data class VideoSharedCornerSpec(
    val enabled: Boolean,
    val startCornerDp: Int,
    val endCornerDp: Int
)

internal fun resolveVideoSharedTransitionProfile(): VideoSharedTransitionProfile {
    return VideoSharedTransitionProfile.COVER_AND_METADATA
}

internal fun resolveVideoCardSharedTransitionEasing(): Easing {
    return VIDEO_CARD_IOS_LIKE_EASE_OUT
}

private fun resolveVideoSharedTransitionProfile(sourceRoute: String?): VideoSharedTransitionProfile {
    return if (sourceRoute?.substringBefore("?") == HOME_SOURCE_ROUTE) {
        VideoSharedTransitionProfile.COVER_ONLY
    } else {
        VideoSharedTransitionProfile.COVER_AND_METADATA
    }
}

internal fun shouldEnableVideoCoverSharedTransition(
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return transitionEnabled &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope
}

internal fun shouldEnableVideoMetadataSharedTransition(
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean,
    useCardContainerSharedBounds: Boolean = false,
    profile: VideoSharedTransitionProfile = resolveVideoSharedTransitionProfile()
): Boolean {
    if (!coverSharedEnabled) return false
    // 卡片容器已经承载整体放大/回收时，标题、UP、统计等不要再各自抢独立 sharedBounds。
    if (useCardContainerSharedBounds) return false
    // Keep metadata linked during quick return to avoid cover-only snapback.
    if (isQuickReturnLimited && profile == VideoSharedTransitionProfile.COVER_ONLY) return false
    return profile == VideoSharedTransitionProfile.COVER_AND_METADATA
}

internal fun resolveVideoSharedTransitionOwnership(
    sourceRoute: String?,
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean
): VideoSharedTransitionOwnership {
    if (!coverSharedEnabled) {
        return VideoSharedTransitionOwnership(
            useCardShellSharedBounds = false,
            useCoverSharedBounds = false,
            useTitleSharedBounds = false,
            useAvatarSharedBounds = false,
            useUpNameSharedBounds = false,
            useStatsSharedBounds = false
        )
    }

    val isHomeSource = sourceRoute?.substringBefore("?") == HOME_SOURCE_ROUTE
    if (isHomeSource) {
        // 首页：子元素匹配模型。不再用整卡 shell（详情侧无对应 key、且会与子元素共享嵌套抢变换），
        // 改为封面（↔播放器封面）、标题、头像、UP 名各自独立 1:1 morph；播放量/统计、关注按钮等跟随缩放渐隐。
        return VideoSharedTransitionOwnership(
            useCardShellSharedBounds = false,
            useCoverSharedBounds = true,
            useTitleSharedBounds = true,
            useAvatarSharedBounds = true,
            useUpNameSharedBounds = true,
            useStatsSharedBounds = false
        )
    }

    // 非首页：维持原「整卡 shell + 元数据」行为不变，封面由 shell 承载、不独立共享。
    val metadataShared = shouldEnableVideoMetadataSharedTransition(
        coverSharedEnabled = true,
        isQuickReturnLimited = isQuickReturnLimited,
        profile = resolveVideoSharedTransitionProfile(sourceRoute)
    )
    return VideoSharedTransitionOwnership(
        useCardShellSharedBounds = true,
        useCoverSharedBounds = false,
        useTitleSharedBounds = metadataShared,
        useAvatarSharedBounds = metadataShared,
        useUpNameSharedBounds = metadataShared,
        useStatsSharedBounds = metadataShared
    )
}

internal fun resolveVideoCardSharedTransitionMotionSpec(
    sourceRoute: String?,
    transitionEnabled: Boolean
): VideoSharedTransitionMotionSpec {
    val enabled = transitionEnabled &&
        !sourceRoute?.substringBefore("?").isNullOrBlank()
    if (!enabled) {
        return VideoSharedTransitionMotionSpec(
            enabled = false,
            durationMillis = 0,
            contentDelayMillis = 0,
            contentDurationMillis = 0,
            contentSlideOffsetDp = 0,
            contentInitialScale = 1f,
            easing = VIDEO_CARD_IOS_LIKE_EASE_OUT
        )
    }

    return VideoSharedTransitionMotionSpec(
        enabled = true,
        durationMillis = HOME_SHARED_TRANSITION_DURATION_MILLIS,
        contentDelayMillis = HOME_DETAIL_REVEAL_DELAY_MILLIS,
        contentDurationMillis = HOME_DETAIL_REVEAL_DURATION_MILLIS,
        contentSlideOffsetDp = HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP,
        contentInitialScale = HOME_DETAIL_REVEAL_INITIAL_SCALE,
        easing = VIDEO_CARD_IOS_LIKE_EASE_OUT
    )
}

internal fun resolveHomeVideoSharedTransitionCornerSpec(
    sourceRoute: String?,
    transitionEnabled: Boolean
): VideoSharedCornerSpec {
    val enabled = transitionEnabled &&
        !sourceRoute?.substringBefore("?").isNullOrBlank()
    return if (enabled) {
        VideoSharedCornerSpec(
            enabled = true,
            startCornerDp = HOME_SHARED_TRANSITION_CARD_CORNER_DP,
            endCornerDp = HOME_SHARED_TRANSITION_PLAYER_CORNER_DP
        )
    } else {
        VideoSharedCornerSpec(
            enabled = false,
            startCornerDp = 0,
            endCornerDp = 0
        )
    }
}
