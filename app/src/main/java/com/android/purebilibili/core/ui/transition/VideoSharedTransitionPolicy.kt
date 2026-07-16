package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import kotlin.math.roundToInt

internal enum class VideoSharedTransitionProfile {
    COVER_ONLY,
    COVER_AND_METADATA
}

internal enum class VideoSharedTransitionPlaybackIntent {
    ImmediatePlayback,
    CoverFirst
}

internal fun resolveVideoSharedTransitionPlaybackIntent(
    clickToPlayEnabled: Boolean,
    forceImmediatePlayback: Boolean = false
): VideoSharedTransitionPlaybackIntent {
    return if (!forceImmediatePlayback && !clickToPlayEnabled) {
        VideoSharedTransitionPlaybackIntent.CoverFirst
    } else {
        VideoSharedTransitionPlaybackIntent.ImmediatePlayback
    }
}

internal fun shouldFadePlayerSurfaceOnDetailReturn(
    isLeaving: Boolean,
    playbackIntent: VideoSharedTransitionPlaybackIntent
): Boolean {
    // CoverFirst 本身已是封面主导；Immediate 返回时要淡出 surface 露出 handoff 封面。
    return isLeaving && playbackIntent == VideoSharedTransitionPlaybackIntent.ImmediatePlayback
}

internal fun shouldUseDetailReturnCoverCrossfade(
    isLeaving: Boolean,
    playbackIntent: VideoSharedTransitionPlaybackIntent
): Boolean {
    // 任意播放意图在返回时都叠封面，保证 morph 过程看得见封面而非黑底。
    return isLeaving
}

internal enum class VideoSharedTransitionTargetMode {
    InlineCover,
    InlinePlayer,
    LandscapeFullscreen,
    PortraitFullscreen
}

internal const val VIDEO_SHARED_COVER_ASPECT_RATIO = 16f / 10f
private const val HOME_SOURCE_ROUTE = "home"
internal const val VIDEO_SHARED_TRANSITION_FAST_DURATION_MILLIS = 320
internal const val VIDEO_SHARED_TRANSITION_STANDARD_DURATION_MILLIS = 400
internal const val VIDEO_SHARED_TRANSITION_SLOW_DURATION_MILLIS = 520
internal const val VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS = 280
internal const val VIDEO_SHARED_TRANSITION_CUSTOM_MAX_MILLIS = 900
internal const val VIDEO_SHARED_TRANSITION_CUSTOM_DEFAULT_MILLIS =
    VIDEO_SHARED_TRANSITION_STANDARD_DURATION_MILLIS
private const val HOME_DETAIL_REVEAL_DELAY_MILLIS = 40
private const val HOME_DETAIL_REVEAL_MIN_DURATION_MILLIS = 220
private const val HOME_DETAIL_REVEAL_MAX_DURATION_MILLIS = 360
private const val HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP = 14
private const val HOME_DETAIL_REVEAL_INITIAL_SCALE = 0.985f
private const val VIDEO_METADATA_SHARED_BOUNDS_RATIO = 0.72f
private const val VIDEO_METADATA_SHARED_BOUNDS_MIN_MILLIS = 200
private const val VIDEO_METADATA_SHARED_BOUNDS_MAX_MILLIS = 360
private const val HOME_SHARED_TRANSITION_CARD_CORNER_DP = 16
private const val HOME_SHARED_TRANSITION_PLAYER_CORNER_DP = 12
private const val DEFAULT_VIDEO_CARD_CORNER_DP = 12
private const val DEFAULT_VIDEO_PLAYER_CORNER_DP = 12
private const val DYNAMIC_VIDEO_CARD_CORNER_DP = 10
private const val WATCH_LATER_VIDEO_CARD_CORNER_DP = 8
private const val VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO = 0.79f
// 收回保留 Hero 的速度感，但提高阻尼以减少落点越界和二次弹跳。
private const val VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO = 0.86f
private const val VIDEO_CARD_HERO_SPRING_REFERENCE_STIFFNESS = 250f
private const val VIDEO_CARD_HERO_SPRING_REFERENCE_DURATION_MILLIS = 400f
private const val VIDEO_CARD_HERO_SPRING_MIN_STIFFNESS = 50f
private const val VIDEO_CARD_HERO_SPRING_MAX_STIFFNESS = 500f
// Hero 默认 timing function；仅用于透明度，避免 spring 的轻微越界污染 alpha。
private val VIDEO_CARD_ALPHA_EASING = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val VIDEO_CARD_ROUTE_SHEET_EASING = CubicBezierEasing(0.30f, 0.45f, 0.35f, 1.00f)

enum class VideoSharedTransitionSpeed(val value: Int, val label: String) {
    FAST(0, "快速"),
    STANDARD(1, "标准"),
    SLOW(2, "慢速"),
    CUSTOM(3, "自定义");

    companion object {
        fun fromValue(value: Int): VideoSharedTransitionSpeed =
            entries.find { it.value == value } ?: STANDARD
    }
}

internal data class VideoSharedTransitionSpeedSettings(
    val speed: VideoSharedTransitionSpeed = VideoSharedTransitionSpeed.STANDARD,
    val customDurationMillis: Int = VIDEO_SHARED_TRANSITION_CUSTOM_DEFAULT_MILLIS
)

internal data class VideoSharedTransitionOwnership(
    val useCoverSharedBounds: Boolean,
    val useMetadataSharedBounds: Boolean,
    val useCardContainerSharedBounds: Boolean = false
)

internal data class VideoSharedTransitionMotionSpec(
    val enabled: Boolean,
    val durationMillis: Int,
    val fullscreenDurationMillis: Int,
    val contentDelayMillis: Int,
    val contentDurationMillis: Int,
    val contentSlideOffsetDp: Int,
    val contentInitialScale: Float,
    val enterSpatialDampingRatio: Float,
    val returnSpatialDampingRatio: Float,
    val spatialStiffness: Float,
    val enterAlphaEasing: Easing,
    val returnAlphaEasing: Easing
)

internal enum class VideoSharedTransitionDirection {
    ENTER,
    RETURN
}

internal data class VideoSharedCornerSpec(
    val enabled: Boolean,
    val startCornerDp: Int,
    val endCornerDp: Int
)

internal data class VideoSharedTransitionVisualSpec(
    val targetMode: VideoSharedTransitionTargetMode,
    val sourceCornerDp: Int,
    val targetCornerDp: Int,
    val fillTargetViewport: Boolean,
    val useCoverSharedBounds: Boolean,
    val suppressCoverFade: Boolean
)

internal fun resolveVideoSharedTransitionProfile(): VideoSharedTransitionProfile {
    return VideoSharedTransitionProfile.COVER_AND_METADATA
}

internal fun resolveVideoCardSharedTransitionEnterEasing(): Easing = VIDEO_CARD_ROUTE_SHEET_EASING

internal fun resolveVideoCardSharedTransitionReturnEasing(): Easing = VIDEO_CARD_ROUTE_SHEET_EASING

internal fun resolveVideoSharedTransitionSpatialStiffness(durationMillis: Int): Float {
    val safeDurationMillis = durationMillis.coerceAtLeast(1).toFloat()
    val durationRatio = VIDEO_CARD_HERO_SPRING_REFERENCE_DURATION_MILLIS / safeDurationMillis
    return (VIDEO_CARD_HERO_SPRING_REFERENCE_STIFFNESS * durationRatio * durationRatio)
        .coerceIn(
            VIDEO_CARD_HERO_SPRING_MIN_STIFFNESS,
            VIDEO_CARD_HERO_SPRING_MAX_STIFFNESS
        )
}

internal fun resolveVideoSharedCoverCacheKey(
    videoIdentity: String,
    useLowQualityCover: Boolean = false,
): String {
    val qualityTag = if (useLowQualityCover) "s" else "n"
    return "cover_${videoIdentity.trim()}_${qualityTag}"
}

internal fun resolveVideoSharedTransitionDirection(
    initialBounds: Rect,
    targetBounds: Rect
): VideoSharedTransitionDirection {
    val initialArea = initialBounds.width * initialBounds.height
    val targetArea = targetBounds.width * targetBounds.height
    return if (targetArea < initialArea) {
        VideoSharedTransitionDirection.RETURN
    } else {
        VideoSharedTransitionDirection.ENTER
    }
}

internal fun resolveVideoSharedTransitionEasing(
    motion: VideoSharedTransitionMotionSpec,
    initialBounds: Rect,
    targetBounds: Rect
): Easing {
    return when (resolveVideoSharedTransitionDirection(initialBounds, targetBounds)) {
        VideoSharedTransitionDirection.ENTER -> motion.enterAlphaEasing
        VideoSharedTransitionDirection.RETURN -> motion.returnAlphaEasing
    }
}

internal fun normalizeVideoSharedTransitionCustomDurationMillis(durationMillis: Int): Int {
    return durationMillis.coerceIn(
        VIDEO_SHARED_TRANSITION_CUSTOM_MIN_MILLIS,
        VIDEO_SHARED_TRANSITION_CUSTOM_MAX_MILLIS
    )
}

internal fun resolveVideoSharedTransitionDurationMillis(
    speedSettings: VideoSharedTransitionSpeedSettings
): Int {
    return when (speedSettings.speed) {
        VideoSharedTransitionSpeed.FAST -> VIDEO_SHARED_TRANSITION_FAST_DURATION_MILLIS
        VideoSharedTransitionSpeed.STANDARD -> VIDEO_SHARED_TRANSITION_STANDARD_DURATION_MILLIS
        VideoSharedTransitionSpeed.SLOW -> VIDEO_SHARED_TRANSITION_SLOW_DURATION_MILLIS
        VideoSharedTransitionSpeed.CUSTOM ->
            normalizeVideoSharedTransitionCustomDurationMillis(speedSettings.customDurationMillis)
    }
}

internal fun resolveVideoSharedTransitionFullscreenDurationMillis(durationMillis: Int): Int {
    return durationMillis + 80
}

internal fun resolveVideoSharedTransitionContentDurationMillis(durationMillis: Int): Int {
    return (durationMillis * 0.6f).roundToInt().coerceIn(
        HOME_DETAIL_REVEAL_MIN_DURATION_MILLIS,
        HOME_DETAIL_REVEAL_MAX_DURATION_MILLIS
    )
}

internal fun resolveVideoSharedTransitionSourceCornerDp(
    sourceRoute: String?,
    fallbackCornerDp: Int = DEFAULT_VIDEO_CARD_CORNER_DP
): Int {
    return when (sourceRoute?.substringBefore("?")) {
        "dynamic",
        "dynamic_detail" -> DYNAMIC_VIDEO_CARD_CORNER_DP
        "watch_later" -> WATCH_LATER_VIDEO_CARD_CORNER_DP
        else -> fallbackCornerDp
    }.coerceAtLeast(0)
}

internal fun resolveVideoSharedTransitionVisualSpec(
    sourceRoute: String?,
    sourceCornerDp: Int = resolveVideoSharedTransitionSourceCornerDp(sourceRoute),
    playbackIntent: VideoSharedTransitionPlaybackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
    fullscreen: Boolean = false,
    autoPortrait: Boolean = false,
    initialVertical: Boolean = false,
    isVerticalVideo: Boolean = false,
    isReturning: Boolean = false,
    playerCornerDp: Int = DEFAULT_VIDEO_PLAYER_CORNER_DP
): VideoSharedTransitionVisualSpec {
    val normalizedSourceRoute = sourceRoute?.substringBefore("?")?.takeIf { it.isNotBlank() }
    val safeSourceCornerDp = sourceCornerDp.coerceAtLeast(0)
    val shouldPreferPortraitTarget = initialVertical || (autoPortrait && isVerticalVideo)
    val targetMode = when {
        isReturning -> VideoSharedTransitionTargetMode.InlineCover
        shouldPreferPortraitTarget -> VideoSharedTransitionTargetMode.PortraitFullscreen
        playbackIntent == VideoSharedTransitionPlaybackIntent.CoverFirst ->
            VideoSharedTransitionTargetMode.InlineCover
        fullscreen -> VideoSharedTransitionTargetMode.LandscapeFullscreen
        else -> VideoSharedTransitionTargetMode.InlinePlayer
    }
    val targetCornerDp = when {
        isReturning -> safeSourceCornerDp
        targetMode == VideoSharedTransitionTargetMode.LandscapeFullscreen -> 0
        targetMode == VideoSharedTransitionTargetMode.PortraitFullscreen -> 0
        else -> playerCornerDp.coerceAtLeast(0)
    }

    return VideoSharedTransitionVisualSpec(
        targetMode = targetMode,
        sourceCornerDp = safeSourceCornerDp,
        targetCornerDp = targetCornerDp,
        fillTargetViewport = targetMode == VideoSharedTransitionTargetMode.LandscapeFullscreen ||
            targetMode == VideoSharedTransitionTargetMode.PortraitFullscreen,
        useCoverSharedBounds = normalizedSourceRoute != null,
        suppressCoverFade = isReturning
    )
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

internal fun shouldUseVideoCardShellSharedBounds(
    sourceRoute: String?,
    transitionEnabled: Boolean
): Boolean {
    if (!transitionEnabled) return false
    return !sourceRoute?.substringBefore("?").isNullOrBlank()
}

internal fun shouldUseHomeVideoCardShellContainerTransform(
    sourceRoute: String?,
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    return shouldUseVideoCardShellContainerTransform(
        sourceRoute = sourceRoute,
        transitionEnabled = transitionEnabled,
        hasSharedTransitionScope = hasSharedTransitionScope,
        hasAnimatedVisibilityScope = hasAnimatedVisibilityScope
    )
}

internal fun shouldUseVideoCardShellContainerTransform(
    sourceRoute: String?,
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean
): Boolean {
    if (!transitionEnabled || !hasSharedTransitionScope || !hasAnimatedVisibilityScope) return false
    val normalizedSourceRoute = sourceRoute?.substringBefore("?")
    return isVideoCardReturnTargetRoute(normalizedSourceRoute)
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
    // Home 源也启用 metadata sharedBounds，标题/头像/UP名独立共享
    return true
}

internal fun resolveVideoSharedTransitionOwnership(
    sourceRoute: String?,
    coverSharedEnabled: Boolean,
    isQuickReturnLimited: Boolean,
    transitionEnabled: Boolean = true
): VideoSharedTransitionOwnership {
    if (!coverSharedEnabled) {
        return VideoSharedTransitionOwnership(
            useCoverSharedBounds = false,
            useMetadataSharedBounds = false,
            useCardContainerSharedBounds = false
        )
    }

    val useCardContainerSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = sourceRoute,
        transitionEnabled = transitionEnabled
    )
    return VideoSharedTransitionOwnership(
        useCoverSharedBounds = true,
        useMetadataSharedBounds = shouldEnableVideoMetadataSharedTransition(
            coverSharedEnabled = true,
            isQuickReturnLimited = isQuickReturnLimited,
            useCardContainerSharedBounds = useCardContainerSharedBounds,
            profile = resolveVideoSharedTransitionProfile(sourceRoute)
        ),
        useCardContainerSharedBounds = useCardContainerSharedBounds
    )
}

internal fun resolveVideoCardSharedTransitionMotionSpec(
    sourceRoute: String?,
    transitionEnabled: Boolean,
    speedSettings: VideoSharedTransitionSpeedSettings = VideoSharedTransitionSpeedSettings(),
    isQuickReturn: Boolean = false,
): VideoSharedTransitionMotionSpec {
    val enabled = transitionEnabled &&
        !sourceRoute?.substringBefore("?").isNullOrBlank()
    if (!enabled) {
        return VideoSharedTransitionMotionSpec(
            enabled = false,
            durationMillis = 0,
            fullscreenDurationMillis = 0,
            contentDelayMillis = 0,
            contentDurationMillis = 0,
            contentSlideOffsetDp = 0,
            contentInitialScale = 1f,
            enterSpatialDampingRatio = VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO,
            returnSpatialDampingRatio = VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO,
            spatialStiffness = VIDEO_CARD_HERO_SPRING_REFERENCE_STIFFNESS,
            enterAlphaEasing = VIDEO_CARD_ALPHA_EASING,
            returnAlphaEasing = VIDEO_CARD_ALPHA_EASING
        )
    }
    val durationMillis = resolveVideoSharedTransitionDurationMillis(speedSettings)

    return VideoSharedTransitionMotionSpec(
        enabled = true,
        durationMillis = durationMillis,
        fullscreenDurationMillis = resolveVideoSharedTransitionFullscreenDurationMillis(durationMillis),
        contentDelayMillis = if (isQuickReturn) 0 else HOME_DETAIL_REVEAL_DELAY_MILLIS,
        contentDurationMillis = resolveVideoSharedTransitionContentDurationMillis(durationMillis),
        contentSlideOffsetDp = HOME_DETAIL_REVEAL_SLIDE_OFFSET_DP,
        contentInitialScale = HOME_DETAIL_REVEAL_INITIAL_SCALE,
        enterSpatialDampingRatio = VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO,
        returnSpatialDampingRatio = VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO,
        spatialStiffness = resolveVideoSharedTransitionSpatialStiffness(durationMillis),
        enterAlphaEasing = VIDEO_CARD_ALPHA_EASING,
        returnAlphaEasing = VIDEO_CARD_ALPHA_EASING
    )
}

internal fun resolveVideoMetadataSharedTransitionMotionSpec(
    transitionEnabled: Boolean,
    speedSettings: VideoSharedTransitionSpeedSettings = VideoSharedTransitionSpeedSettings()
): VideoSharedTransitionMotionSpec {
    val durationMillis = if (transitionEnabled) {
        resolveVideoSharedTransitionDurationMillis(speedSettings)
    } else {
        0
    }
    return VideoSharedTransitionMotionSpec(
        enabled = transitionEnabled,
        durationMillis = durationMillis,
        fullscreenDurationMillis = if (transitionEnabled) {
            resolveVideoSharedTransitionFullscreenDurationMillis(durationMillis)
        } else {
            0
        },
        contentDelayMillis = 0,
        contentDurationMillis = durationMillis,
        contentSlideOffsetDp = 0,
        contentInitialScale = 1f,
        enterSpatialDampingRatio = VIDEO_CARD_HERO_ENTER_SPRING_DAMPING_RATIO,
        returnSpatialDampingRatio = VIDEO_CARD_RETURN_SPRING_DAMPING_RATIO,
        spatialStiffness = resolveVideoSharedTransitionSpatialStiffness(durationMillis),
        enterAlphaEasing = VIDEO_CARD_ALPHA_EASING,
        returnAlphaEasing = VIDEO_CARD_ALPHA_EASING
    )
}

internal fun videoSharedElementBoundsTransformSpec(
    motion: VideoSharedTransitionMotionSpec,
    initialBounds: Rect,
    targetBounds: Rect,
    durationMillis: Int = motion.durationMillis
): FiniteAnimationSpec<Rect> {
    val dampingRatio = when (resolveVideoSharedTransitionDirection(initialBounds, targetBounds)) {
        VideoSharedTransitionDirection.ENTER -> motion.enterSpatialDampingRatio
        VideoSharedTransitionDirection.RETURN -> motion.returnSpatialDampingRatio
    }
    return spring(
        dampingRatio = dampingRatio,
        stiffness = resolveVideoSharedTransitionSpatialStiffness(durationMillis)
    )
}

internal fun videoMetadataSharedElementBoundsTransformSpec(
    motion: VideoSharedTransitionMotionSpec,
    initialBounds: Rect,
    targetBounds: Rect
): FiniteAnimationSpec<Rect> {
    val dampingRatio = when (resolveVideoSharedTransitionDirection(initialBounds, targetBounds)) {
        VideoSharedTransitionDirection.ENTER -> motion.enterSpatialDampingRatio
        VideoSharedTransitionDirection.RETURN -> motion.returnSpatialDampingRatio
    }
    return spring(
        dampingRatio = dampingRatio,
        stiffness = motion.spatialStiffness
    )
}

internal fun resolveVideoMetadataSharedBoundsDurationMillis(
    motion: VideoSharedTransitionMotionSpec
): Int {
    if (!motion.enabled) return 0
    return (motion.durationMillis * VIDEO_METADATA_SHARED_BOUNDS_RATIO)
        .roundToInt()
        .coerceIn(
            VIDEO_METADATA_SHARED_BOUNDS_MIN_MILLIS,
            VIDEO_METADATA_SHARED_BOUNDS_MAX_MILLIS
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
