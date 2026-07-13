package com.android.purebilibili.navigation3

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigationevent.NavigationEventTransitionState
import com.android.purebilibili.core.ui.ProvideAnimatedVisibilityScope
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.motion.gentleEnterTween
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.core.ui.transition.LocalPredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.PREDICTIVE_BACK_BACKGROUND_CANCEL_DURATION_MS
import com.android.purebilibili.core.ui.transition.PredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS
import com.android.purebilibili.core.ui.transition.VIDEO_CARD_TRANSITION_BACKGROUND_FORWARD_DURATION_MS
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackCommitBlurDurationMs
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackGestureBlurProgress
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionBackgroundGestureBlurProgress
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionBackgroundReturnDurationMs
import com.android.purebilibili.core.ui.transition.isVideoCardTransitionBackgroundGesturePhase
import com.android.purebilibili.core.ui.transition.shouldApplyPredictiveBackGestureBlur
import com.android.purebilibili.core.ui.transition.shouldShowVideoCardTransitionNavBackdrop
import com.android.purebilibili.core.ui.transition.VideoCardTransitionNavBackdrop
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationHandler
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiAutoPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackAnimationHandler
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackExitDirection
import kotlinx.coroutines.launch

@Composable
internal fun BiliPaiNavDisplayHost(
    backStack: List<BiliPaiNavKey>,
    cardTransitionEnabled: Boolean = true,
    predictiveBackEnabled: Boolean = true,
    predictiveBackAnimationStyle: BiliPaiPredictiveBackAnimationStyle = BiliPaiPredictiveBackAnimationStyle.SCALE,
    predictiveBackExitDirectionOverride: String = "auto",
    sourceMetadata: BiliPaiNavSourceMetadata,
    onBack: () -> Unit,
    onNativeVideoBackProgress: (currentKey: BiliPaiNavKey?, targetKey: BiliPaiNavKey?, progress: Float) -> Unit = { _, _, _ -> },
    onNativeVideoBackCancelled: (currentKey: BiliPaiNavKey?, targetKey: BiliPaiNavKey?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    visibleBottomBarRoutes: Set<String> = emptySet(),
    activeMainHostRoute: String? = null,
    isLightBackground: Boolean = false,
    content: @Composable (BiliPaiNavKey) -> Unit
) {
    val safeBackStack = remember(backStack) {
        backStack.ifEmpty { listOf(BiliPaiNavKey.MainHost) }
    }
    val application = LocalContext.current.applicationContext as Application
    var navigationEventState: NavigationEventState<SceneInfo<BiliPaiNavKey>>? = null
    val navigationScope = rememberCoroutineScope()
    val videoCardTransitionBackgroundProgress = remember { Animatable(0f) }
    val predictiveBackBackgroundProgress = remember { Animatable(0f) }
    var videoCardTransitionBackgroundPhase by remember {
        mutableStateOf(VideoCardTransitionBackgroundPhase.IDLE)
    }
    var videoCardReturnGestureInProgress by remember { mutableStateOf(false) }
    var videoCardBackgroundGestureRestoreInProgress by remember { mutableStateOf(false) }
    var videoCardGestureStartBlurProgress by remember { mutableFloatStateOf(1f) }
    var videoCardGestureProgress by remember { mutableStateOf<Float?>(null) }
    val videoCardBackgroundProgressProvider = remember(videoCardTransitionBackgroundProgress) {
        {
            val backProgress = videoCardGestureProgress
            if (videoCardReturnGestureInProgress && backProgress != null) {
                resolveVideoCardTransitionBackgroundGestureBlurProgress(
                    phase = VideoCardTransitionBackgroundPhase.OPENING,
                    currentBlurProgress = videoCardGestureStartBlurProgress,
                    backProgress = backProgress,
                )
            } else {
                videoCardTransitionBackgroundProgress.value
            }
        }
    }
    // 系统减弱动画(省电/无障碍/开发者选项)时，过渡背景降级为仅 scrim，跳过全屏 GPU 实时模糊。
    val transitionBackgroundMotionTier =
        if (rememberSystemReduceMotion()) MotionTier.Reduced else MotionTier.Normal
    var previousVideoCardTransitionBackStack by remember {
        mutableStateOf(safeBackStack)
    }
    LaunchedEffect(
        safeBackStack,
        cardTransitionEnabled,
        sourceMetadata.sourceRoute
    ) {
        val previousTop = previousVideoCardTransitionBackStack.lastOrNull()
        val currentTop = safeBackStack.lastOrNull()
        val hasVideoCardTransitionSource = isVideoCardReturnTargetRoute(sourceMetadata.sourceRoute)
        val openedVideoDetail = currentTop is BiliPaiNavKey.VideoDetail &&
            previousTop !is BiliPaiNavKey.VideoDetail &&
            hasVideoCardTransitionSource
        val returnedFromVideoDetail = previousTop is BiliPaiNavKey.VideoDetail &&
            currentTop !is BiliPaiNavKey.VideoDetail &&
            hasVideoCardTransitionSource
        previousVideoCardTransitionBackStack = safeBackStack

        if (!cardTransitionEnabled || !hasVideoCardTransitionSource) {
            videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE
            videoCardReturnGestureInProgress = false
            videoCardGestureProgress = null
            videoCardTransitionBackgroundProgress.snapTo(0f)
            return@LaunchedEffect
        }

        when {
            openedVideoDetail -> {
                videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING
                videoCardTransitionBackgroundProgress.snapTo(0f)
                videoCardTransitionBackgroundProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = gentleEnterTween(
                        durationMillis = VIDEO_CARD_TRANSITION_BACKGROUND_FORWARD_DURATION_MS,
                    )
                )
                // 详情页覆盖期间保持 blur-only 状态，避免返回 pop 后先清晰一帧再补模糊。
                videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.HELD
            }

            returnedFromVideoDetail -> {
                // pop 后首页层已含共享元素回收中的卡片；任何 route 级 blur/scrim 都会让落位封面发糊/闪烁。
                // 手势阶段已在详情页覆盖下消解虚化(见 performBack 最终进度捕获 + progressProvider)。
                // performBack 通常已在 pop 前归零；若仍有残留(非 performBack 路径或竞态)，用与剩余
                // 虚化成比例的短 animateTo 收尾，避免 OPENING 快速返回周边首页模糊硬切。
                videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE
                videoCardReturnGestureInProgress = false
                videoCardGestureProgress = null
                val remainingBlur = videoCardTransitionBackgroundProgress.value
                if (remainingBlur > 0.001f) {
                    videoCardTransitionBackgroundProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = resolveVideoCardTransitionBackgroundReturnDurationMs(
                                startProgress = remainingBlur,
                            ).coerceAtMost(VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS),
                            easing = FastOutLinearInEasing,
                        ),
                    )
                } else {
                    videoCardTransitionBackgroundProgress.snapTo(0f)
                }
            }

            currentTop !is BiliPaiNavKey.VideoDetail -> {
                videoCardTransitionBackgroundProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
                        easing = FastOutLinearInEasing
                    )
                )
                videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE
            }
        }
    }
    val popRouteTransition = remember(cardTransitionEnabled, sourceMetadata, safeBackStack) {
        resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = cardTransitionEnabled,
            sourceMetadata = sourceMetadata,
            fromKey = safeBackStack.lastOrNull(),
            toKey = safeBackStack.getOrNull(safeBackStack.lastIndex - 1)
        )
    }
    val autoPredictiveBackExitDirection = remember(popRouteTransition, sourceMetadata.cardSourceDirection) {
        resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = popRouteTransition,
            cardSourceDirection = sourceMetadata.cardSourceDirection,
        )
    }
    val predictiveBackExitDirection = remember(
        autoPredictiveBackExitDirection,
        predictiveBackExitDirectionOverride,
    ) {
        resolveBiliPaiPredictiveBackExitDirection(
            storageValue = predictiveBackExitDirectionOverride,
            autoDerived = autoPredictiveBackExitDirection,
        )
    }
    val predictiveBackHandler: BiliPaiPredictiveBackAnimationHandler = remember(
        popRouteTransition,
        predictiveBackEnabled,
        predictiveBackAnimationStyle,
        predictiveBackExitDirection,
    ) {
        resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = popRouteTransition,
            predictiveBackEnabled = predictiveBackEnabled,
            style = predictiveBackAnimationStyle,
            exitDirection = predictiveBackExitDirection,
        )
    }
    val performBack: (() -> Unit) -> Unit = { commitTransitionCallBack ->
        navigationScope.launch {
            val finalVideoCardGestureBackProgress =
                (navigationEventState?.transitionState as? NavigationEventTransitionState.InProgress)
                    ?.latestEvent
                    ?.progress
            val shouldCaptureVideoCardGestureBlur = cardTransitionEnabled &&
                isVideoCardTransitionBackgroundGesturePhase(videoCardTransitionBackgroundPhase) &&
                safeBackStack.lastOrNull() is BiliPaiNavKey.VideoDetail &&
                isVideoCardReturnTargetRoute(sourceMetadata.sourceRoute) &&
                finalVideoCardGestureBackProgress != null
            if (shouldCaptureVideoCardGestureBlur) {
                videoCardTransitionBackgroundProgress.snapTo(
                    resolveVideoCardTransitionBackgroundGestureBlurProgress(
                        phase = VideoCardTransitionBackgroundPhase.OPENING,
                        currentBlurProgress = videoCardGestureStartBlurProgress,
                        backProgress = finalVideoCardGestureBackProgress,
                    )
                )
            }
            val shouldFadePredictiveBlur = shouldApplyPredictiveBackGestureBlur(
                routeTransition = popRouteTransition,
                predictiveBackEnabled = predictiveBackEnabled,
                gestureReturningVideoCard = false,
                motionTier = transitionBackgroundMotionTier,
            ) && predictiveBackBackgroundProgress.value > 0f
            val predictiveBlurFadeJob = if (shouldFadePredictiveBlur) {
                launch {
                    predictiveBackBackgroundProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = resolvePredictiveBackCommitBlurDurationMs(
                                predictiveBackBackgroundProgress.value
                            ),
                            easing = FastOutLinearInEasing,
                        ),
                    )
                }
            } else {
                null
            }
            predictiveBackHandler.onBackPressed(
                transitionState = navigationEventState?.transitionState,
                currentPageKey = safeBackStack.lastOrNull(),
            )
            predictiveBlurFadeJob?.join()
            // pop 之前把视频卡片背景模糊归零：详情页此刻仍覆盖首页，置 0 不可见；
            // 避免依赖 pop 后 LaunchedEffect(合成之后)才 snapTo(0)，从而消除落位首帧
            // 首页/封面残留满模糊。
            // - HELD：正常/手势/实体返回键，覆盖低 progress 手势提交与无手势返回；
            // - OPENING：进入未完成即快速返回(<前向 ramp)，此前只能在 pop 后硬切，
            //   现同样在覆盖层下提前归零，落位即清晰，消除快速返回的落位模糊/硬切。
            val isVideoCardActiveReturn = cardTransitionEnabled &&
                (
                    videoCardTransitionBackgroundPhase == VideoCardTransitionBackgroundPhase.HELD ||
                        videoCardTransitionBackgroundPhase == VideoCardTransitionBackgroundPhase.OPENING
                    ) &&
                safeBackStack.lastOrNull() is BiliPaiNavKey.VideoDetail &&
                isVideoCardReturnTargetRoute(sourceMetadata.sourceRoute)
            if (isVideoCardActiveReturn && videoCardTransitionBackgroundProgress.value > 0f) {
                videoCardTransitionBackgroundProgress.snapTo(0f)
            }
            videoCardReturnGestureInProgress = false
            videoCardGestureProgress = null
            commitTransitionCallBack()
            onBack()
            predictiveBackBackgroundProgress.snapTo(0f)
        }
    }
    val scopedContent: @Composable (BiliPaiNavKey) -> Unit = remember(
        content,
        application,
        safeBackStack,
        videoCardTransitionBackgroundProgress,
        videoCardBackgroundProgressProvider,
        predictiveBackBackgroundProgress,
        transitionBackgroundMotionTier,
        isLightBackground,
    ) {
        { key ->
            val entryRoute = key.toLegacyRoute()
            Box(modifier = Modifier.fillMaxSize()) {
                ProvideAnimatedVisibilityScope(
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                ) {
                    CompositionLocalProvider(
                        LocalVideoCardSharedElementSourceRoute provides entryRoute,
                        LocalVideoCardTransitionBackgroundState provides VideoCardTransitionBackgroundState(
                            progressProvider = videoCardBackgroundProgressProvider,
                            phaseProvider = {
                                videoCardTransitionBackgroundPhase
                            },
                            isReturnGestureInProgressProvider = {
                                videoCardReturnGestureInProgress
                            },
                            isGestureRestoreInProgressProvider = {
                                videoCardBackgroundGestureRestoreInProgress
                            },
                            motionTierProvider = {
                                transitionBackgroundMotionTier
                            },
                            isLightBackgroundProvider = {
                                isLightBackground
                            },
                        ),
                        LocalPredictiveBackBackgroundState provides PredictiveBackBackgroundState(
                            progressProvider = {
                                predictiveBackBackgroundProgress.value
                            },
                            targetKeyProvider = {
                                safeBackStack.getOrNull(safeBackStack.lastIndex - 1)
                            },
                            motionTierProvider = {
                                transitionBackgroundMotionTier
                            },
                            isLightBackgroundProvider = {
                                isLightBackground
                            },
                        ),
                    ) {
                        ProvideNavigation3ViewModelApplicationExtras(application) {
                            content(key)
                        }
                    }
                }
            }
        }
    }
    val entryProvider = remember(sourceMetadata, cardTransitionEnabled, visibleBottomBarRoutes, activeMainHostRoute, scopedContent) {
        biliPaiNavEntryProvider(
            sourceMetadata = sourceMetadata,
            cardTransitionEnabled = cardTransitionEnabled,
            visibleBottomBarRoutes = visibleBottomBarRoutes,
            activeMainHostRoute = activeMainHostRoute,
            content = scopedContent
        )
    }
    val entries = rememberDecoratedNavEntries(
        backStack = safeBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            NavEntryDecorator(
                onPop = { key ->
                    predictiveBackHandler.onPagePop(
                        contentPageKey = key,
                        animationScope = navigationScope,
                    )
                }
            ) { entry ->
                with(predictiveBackHandler) {
                    Box(
                        modifier = Modifier.predictiveBackAnimationDecorator(
                            transitionState = navigationEventState?.transitionState,
                            contentPageKey = entry.contentKey,
                            currentPageKey = safeBackStack.lastOrNull(),
                        )
                    ) {
                        entry.Content()
                    }
                }
            }
        ),
        entryProvider = entryProvider
    )
    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = sharedTransitionScope,
        onBack = { performBack { } }
    )
    val scene = sceneState.currentScene
    val currentInfo = SceneInfo(scene)
    val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
    navigationEventState = rememberNavigationEventState(
        currentInfo = currentInfo,
        backInfo = previousSceneInfos
    )
    val currentBackKey = safeBackStack.lastOrNull()
    val targetBackKey = safeBackStack.getOrNull(safeBackStack.lastIndex - 1)
    val transitionState = navigationEventState.transitionState
    val inProgressState = transitionState as? NavigationEventTransitionState.InProgress
    val nativeVideoBackProgress = inProgressState?.latestEvent?.progress
    SideEffect {
        if (nativeVideoBackProgress != null) {
            onNativeVideoBackProgress(currentBackKey, targetBackKey, nativeVideoBackProgress)
        }
    }

    // 预测式返回手势进行中(video → card 返回目标)时，让首页全屏高斯模糊随手势进度实时消退，
    // 与共享元素 morph 同步，避免手势"吃掉"过渡时间轴后、提交返回时封面才在满模糊下补一段收尾。
    // OPENING 阶段同样启用：以当前开场虚化进度为起点线性消退，避免进入未完成即手势返回时背景"卡住"。
    val gestureReturningVideoCard = cardTransitionEnabled &&
        isVideoCardTransitionBackgroundGesturePhase(videoCardTransitionBackgroundPhase) &&
        currentBackKey is BiliPaiNavKey.VideoDetail &&
        targetBackKey != null &&
        isVideoCardReturnTargetRoute(sourceMetadata.sourceRoute)
    SideEffect {
        val gestureActive = gestureReturningVideoCard && nativeVideoBackProgress != null
        if (gestureActive && !videoCardReturnGestureInProgress) {
            videoCardGestureStartBlurProgress = videoCardTransitionBackgroundProgress.value
        }
        videoCardGestureProgress = nativeVideoBackProgress.takeIf { gestureActive }
        videoCardReturnGestureInProgress = gestureActive
    }

    val predictiveBackGestureBlurEnabled = shouldApplyPredictiveBackGestureBlur(
        routeTransition = popRouteTransition,
        predictiveBackEnabled = predictiveBackEnabled,
        gestureReturningVideoCard = gestureReturningVideoCard,
        motionTier = transitionBackgroundMotionTier,
    )
    val gesturePredictiveBlurTarget: Float? =
        if (predictiveBackGestureBlurEnabled && nativeVideoBackProgress != null) {
            resolvePredictiveBackGestureBlurProgress(
                backProgress = nativeVideoBackProgress,
                routeTransition = popRouteTransition,
            )
        } else {
            null
        }
    LaunchedEffect(gesturePredictiveBlurTarget) {
        val target = gesturePredictiveBlurTarget ?: return@LaunchedEffect
        predictiveBackBackgroundProgress.snapTo(target)
    }

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = performBack,
        onBackCancelled = { commitTransition ->
            onNativeVideoBackCancelled(currentBackKey, targetBackKey)
            val cancelledVideoCardBlur = videoCardBackgroundProgressProvider()
            videoCardReturnGestureInProgress = false
            videoCardGestureProgress = null
            // 手势取消且详情页仍在栈顶(HELD/OPENING)：把随手势消退的背景虚化平滑复原到满值，
            // 与详情页回弹到全屏保持一致，避免下次手势从残留的中间虚化值起跳。
            if (isVideoCardTransitionBackgroundGesturePhase(videoCardTransitionBackgroundPhase) &&
                cancelledVideoCardBlur < 1f
            ) {
                navigationScope.launch {
                    videoCardBackgroundGestureRestoreInProgress = true
                    try {
                        videoCardTransitionBackgroundProgress.snapTo(cancelledVideoCardBlur)
                        videoCardTransitionBackgroundProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
                                easing = FastOutLinearInEasing
                            )
                        )
                        videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.HELD
                    } finally {
                        videoCardBackgroundGestureRestoreInProgress = false
                    }
                }
            }
            if (predictiveBackGestureBlurEnabled &&
                predictiveBackBackgroundProgress.value > 0f
            ) {
                navigationScope.launch {
                    predictiveBackBackgroundProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = PREDICTIVE_BACK_BACKGROUND_CANCEL_DURATION_MS,
                            easing = FastOutLinearInEasing,
                        ),
                    )
                }
            }
            commitTransition()
        },
    )

    val showVideoCardNavBackdrop = shouldShowVideoCardTransitionNavBackdrop(
        cardTransitionEnabled = cardTransitionEnabled,
        phase = videoCardTransitionBackgroundPhase,
        isVideoDetailOnStack = currentBackKey is BiliPaiNavKey.VideoDetail,
    )

    Box(modifier = modifier.fillMaxSize()) {
        VideoCardTransitionNavBackdrop(
            visible = showVideoCardNavBackdrop,
            progressProvider = videoCardBackgroundProgressProvider,
            phase = videoCardTransitionBackgroundPhase,
            isLightBackground = isLightBackground,
        )
        NavDisplay(
            sceneState = sceneState,
            navigationEventState = navigationEventState,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
            sizeTransform = null,
            transitionEffects = NavDisplayTransitionEffects(blockInputDuringTransition = true),
            transitionSpec = {
                with(predictiveBackHandler) {
                    onTransitionSpec()
                }
            },
            popTransitionSpec = {
                with(predictiveBackHandler) {
                    onPopTransitionSpec()
                }
            },
            predictivePopTransitionSpec = { swipeEdge ->
                with(predictiveBackHandler) {
                    onPredictivePopTransitionSpec(swipeEdge = swipeEdge)
                }
            },
        )
    }
}

@Composable
private fun ProvideNavigation3ViewModelApplicationExtras(
    application: Application,
    content: @Composable () -> Unit
) {
    val navEntryOwner = LocalViewModelStoreOwner.current
    if (navEntryOwner == null) {
        content()
        return
    }

    val patchedOwner = remember(navEntryOwner, application) {
        buildNavigation3ViewModelStoreOwner(navEntryOwner, application)
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides patchedOwner) {
        content()
    }
}

private fun buildNavigation3ViewModelStoreOwner(
    navEntryOwner: ViewModelStoreOwner,
    application: Application
): ViewModelStoreOwner {
    val defaultFactoryOwner = navEntryOwner as? HasDefaultViewModelProviderFactory
    val defaultCreationExtras = defaultFactoryOwner?.defaultViewModelCreationExtras
        ?: CreationExtras.Empty
    val patchedCreationExtras = MutableCreationExtras(defaultCreationExtras).apply {
        set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, application)
    }

    return object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = navEntryOwner.viewModelStore
        override val defaultViewModelProviderFactory =
            defaultFactoryOwner?.defaultViewModelProviderFactory
                ?: ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        override val defaultViewModelCreationExtras: CreationExtras = patchedCreationExtras
    }
}
