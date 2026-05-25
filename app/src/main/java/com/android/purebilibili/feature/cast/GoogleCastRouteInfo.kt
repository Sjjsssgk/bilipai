package com.android.purebilibili.feature.cast

data class GoogleCastRouteInfo(
    val routeId: String,
    val name: String,
    val description: String? = null,
    val deviceType: Int = 0
)
