package com.android.purebilibili.feature.audio.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class ListenVideoLayoutPolicyTest {

    @Test
    fun `compact widths use single column library list`() {
        assertEquals(ListenVideoLayout.COMPACT_LIST, resolveListenVideoLayout(393))
        assertEquals(ListenVideoLayout.COMPACT_LIST, resolveListenVideoLayout(599))
    }

    @Test
    fun `wide widths use adaptive music grid`() {
        assertEquals(ListenVideoLayout.WIDE_GRID, resolveListenVideoLayout(600))
        assertEquals(ListenVideoLayout.WIDE_GRID, resolveListenVideoLayout(1_024))
    }
}
