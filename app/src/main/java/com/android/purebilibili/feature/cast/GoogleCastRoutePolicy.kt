package com.android.purebilibili.feature.cast

import com.android.purebilibili.core.plugin.PluginInfo
import com.android.purebilibili.feature.plugin.GOOGLE_CAST_PLUGIN_ID

internal fun isGoogleCastPluginEnabled(plugins: List<PluginInfo>): Boolean {
    return plugins.any { it.plugin.id == GOOGLE_CAST_PLUGIN_ID && it.enabled }
}

internal fun toGoogleCastRouteInfo(
    routeId: String,
    name: String,
    description: String?,
    deviceType: Int,
    isDefaultOrBluetooth: Boolean,
    supportsCastCategory: Boolean
): GoogleCastRouteInfo? {
    if (isDefaultOrBluetooth) return null
    if (!supportsCastCategory) return null
    return GoogleCastRouteInfo(
        routeId = routeId,
        name = name.ifBlank { "Unknown Device" },
        description = description,
        deviceType = deviceType
    )
}

internal fun resolveVisibleGoogleCastRoutes(
    pluginEnabled: Boolean,
    routes: List<GoogleCastRouteInfo>
): List<GoogleCastRouteInfo> {
    if (!pluginEnabled) return emptyList()
    return routes.distinctBy { it.routeId }
}
