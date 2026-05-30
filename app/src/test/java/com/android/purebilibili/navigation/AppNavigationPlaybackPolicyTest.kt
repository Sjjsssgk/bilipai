package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationPlaybackPolicyTest {

    @Test
    fun leavingVideoToHome_shouldStopPlaybackEagerly() {
        assertTrue(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.Home.route
            )
        )
    }

    @Test
    fun leavingVideoToAudioMode_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = ScreenRoutes.AudioMode.route
            )
        )
    }

    @Test
    fun switchingBetweenVideoRoutes_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = VideoRoute.route
            )
        )
    }

    @Test
    fun leavingVideoWithUnknownTargetRoute_shouldNotStopPlaybackEagerly() {
        assertFalse(
            shouldStopPlaybackEagerlyOnVideoRouteExit(
                fromRoute = VideoRoute.route,
                toRoute = null
            )
        )
    }

    @Test
    fun returningToHomeWithCardTransition_shouldDeferBottomBarReveal() {
        assertTrue(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                currentRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToMainHostWithCardTransition_shouldDeferBottomBarReveal() {
        assertTrue(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                currentRoute = "main_host",
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningToHomeWithCardTransitionDisabled_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                currentRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = false
            )
        )
    }

    @Test
    fun notReturningFromDetail_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = false,
                currentRoute = ScreenRoutes.Home.route,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningButStillOnNonHomeRoute_shouldNotDeferBottomBarReveal() {
        assertFalse(
            shouldDeferBottomBarRevealOnVideoReturn(
                isReturningFromDetail = true,
                currentRoute = VideoRoute.route,
                cardTransitionEnabled = true
            )
        )
    }
}
