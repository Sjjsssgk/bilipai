package com.android.purebilibili.feature.cast

import androidx.mediarouter.media.MediaRouter
import com.android.purebilibili.core.plugin.PluginInfo
import com.android.purebilibili.feature.plugin.GOOGLE_CAST_PLUGIN_ID
import com.android.purebilibili.feature.plugin.GoogleCastPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleCastRoutePolicyTest {

    private val enabledGcPluginInfo = PluginInfo(
        plugin = GoogleCastPlugin(),
        enabled = true
    )

    private val disabledGcPluginInfo = PluginInfo(
        plugin = GoogleCastPlugin(),
        enabled = false
    )

    // --- isGoogleCastPluginEnabled ---

    @Test
    fun `isGoogleCastPluginEnabled returns false for empty plugin list`() {
        assertTrue(!isGoogleCastPluginEnabled(emptyList()))
    }

    @Test
    fun `isGoogleCastPluginEnabled returns true when google cast plugin is enabled`() {
        val plugins = listOf(enabledGcPluginInfo)
        assertTrue(isGoogleCastPluginEnabled(plugins))
    }

    @Test
    fun `isGoogleCastPluginEnabled returns false when google cast plugin is disabled`() {
        val plugins = listOf(disabledGcPluginInfo)
        assertTrue(!isGoogleCastPluginEnabled(plugins))
    }

    @Test
    fun `isGoogleCastPluginEnabled returns false when google cast plugin not in list`() {
        // A PluginInfo with a different plugin id
        val otherPlugin = PluginInfo(
            plugin = object : com.android.purebilibili.core.plugin.Plugin {
                override val id = "other_plugin"
                override val name = "Other"
                override val description = ""
                override val version = "1.0"
                override val author = ""
                override val icon = com.android.purebilibili.feature.plugin.GoogleCastPlugin().icon
                override val capabilityManifest = com.android.purebilibili.core.plugin.PluginCapabilityManifest(
                    pluginId = id,
                    displayName = name,
                    version = version,
                    apiVersion = 1,
                    entryClassName = "",
                    capabilities = emptySet()
                )
                override suspend fun onEnable() {}
                override suspend fun onDisable() {}
            },
            enabled = true
        )
        assertTrue(!isGoogleCastPluginEnabled(listOf(otherPlugin)))
    }

    // --- toGoogleCastRouteInfo ---

    @Test
    fun `toGoogleCastRouteInfo returns null for default route`() {
        val result = toGoogleCastRouteInfo(
            routeId = "default_route",
            name = "Phone",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = true,
            supportsCastCategory = true
        )
        assertNull(result)
    }

    @Test
    fun `toGoogleCastRouteInfo returns null for bluetooth route`() {
        val result = toGoogleCastRouteInfo(
            routeId = "bt_route",
            name = "Headphones",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = true,
            supportsCastCategory = true
        )
        assertNull(result)
    }

    @Test
    fun `toGoogleCastRouteInfo returns null when route does not support cast category`() {
        val result = toGoogleCastRouteInfo(
            routeId = "speaker_route",
            name = "Speaker",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = false,
            supportsCastCategory = false
        )
        assertNull(result)
    }

    @Test
    fun `toGoogleCastRouteInfo maps valid cast route correctly`() {
        val result = toGoogleCastRouteInfo(
            routeId = "cast_route_1",
            name = "Living Room TV",
            description = "Chromecast with Google TV",
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_TV,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertEquals("cast_route_1", result!!.routeId)
        assertEquals("Living Room TV", result.name)
        assertEquals("Chromecast with Google TV", result.description)
        assertEquals(MediaRouter.RouteInfo.DEVICE_TYPE_TV, result.deviceType)
    }

    @Test
    fun `toGoogleCastRouteInfo uses fallback name for blank name`() {
        val result = toGoogleCastRouteInfo(
            routeId = "cast_route_2",
            name = "   ",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_TV,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertEquals("Unknown Device", result!!.name)
    }

    @Test
    fun `toGoogleCastRouteInfo uses fallback name for empty name`() {
        val result = toGoogleCastRouteInfo(
            routeId = "cast_route_3",
            name = "",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertEquals("Unknown Device", result!!.name)
    }

    @Test
    fun `toGoogleCastRouteInfo includes description as null when not provided`() {
        val result = toGoogleCastRouteInfo(
            routeId = "cast_route_4",
            name = "Bedroom",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertNull(result!!.description)
    }

    // --- resolveVisibleGoogleCastRoutes ---

    @Test
    fun `resolveVisibleGoogleCastRoutes returns empty when plugin is disabled`() {
        val routes = listOf(
            GoogleCastRouteInfo("id1", "TV 1", null, MediaRouter.RouteInfo.DEVICE_TYPE_TV)
        )
        val result = resolveVisibleGoogleCastRoutes(pluginEnabled = false, routes)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `resolveVisibleGoogleCastRoutes returns routes when plugin is enabled`() {
        val routes = listOf(
            GoogleCastRouteInfo("id1", "TV 1", null, MediaRouter.RouteInfo.DEVICE_TYPE_TV)
        )
        val result = resolveVisibleGoogleCastRoutes(pluginEnabled = true, routes)
        assertEquals(1, result.size)
        assertEquals("id1", result.first().routeId)
    }

    @Test
    fun `resolveVisibleGoogleCastRoutes deduplicates by route id`() {
        val routes = listOf(
            GoogleCastRouteInfo("id1", "TV 1", null, MediaRouter.RouteInfo.DEVICE_TYPE_TV),
            GoogleCastRouteInfo("id1", "TV 1 duplicate", "desc", MediaRouter.RouteInfo.DEVICE_TYPE_TV),
            GoogleCastRouteInfo("id2", "TV 2", null, MediaRouter.RouteInfo.DEVICE_TYPE_TV)
        )
        val result = resolveVisibleGoogleCastRoutes(pluginEnabled = true, routes)
        assertEquals(2, result.size)
        assertEquals("id1", result[0].routeId)
        assertEquals("TV 1", result[0].name)
        assertEquals("id2", result[1].routeId)
    }
}
