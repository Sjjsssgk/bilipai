package com.android.purebilibili.feature.cast

import android.content.Context
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GoogleCastRouteManager {

    private val _routes = MutableStateFlow<List<GoogleCastRouteInfo>>(emptyList())
    val routes: StateFlow<List<GoogleCastRouteInfo>> = _routes.asStateFlow()

    private var mediaRouter: MediaRouter? = null
    private var selector: MediaRouteSelector? = null
    private var callback: MediaRouter.Callback? = null

    val isDiscovering: Boolean get() = mediaRouter != null

    private val castControlCategory: String by lazy {
        CastMediaControlIntent.categoryForCast(
            CastReceiverPolicy.resolveReceiverApplicationId()
        )
    }

    fun startDiscovery(context: Context) {
        if (isDiscovering) return
        val router = MediaRouter.getInstance(context)
        val sel = MediaRouteSelector.Builder()
            .addControlCategory(castControlCategory)
            .build()
        val cb = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }

            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }

            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }
        }
        router.addCallback(sel, cb, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        mediaRouter = router
        selector = sel
        callback = cb
        updateRoutes(router)
    }

    fun stopDiscovery() {
        val router = mediaRouter ?: return
        val cb = callback ?: return
        router.removeCallback(cb)
        mediaRouter = null
        selector = null
        callback = null
        _routes.value = emptyList()
    }

    private fun updateRoutes(router: MediaRouter) {
        val castRoutes = router.routes.mapNotNull { route ->
            toGoogleCastRouteInfo(
                routeId = route.id,
                name = route.name,
                description = route.description,
                deviceType = route.deviceType,
                isDefaultOrBluetooth = route.isDefaultOrBluetooth,
                supportsCastCategory = route.supportsControlCategory(castControlCategory)
            )
        }
        _routes.value = castRoutes
    }
}
