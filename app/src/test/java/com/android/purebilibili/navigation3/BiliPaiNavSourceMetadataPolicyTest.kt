package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiNavSourceMetadataPolicyTest {

    @Test
    fun clickedVisibleCardIsSharedTransitionReady() {
        val metadata = BiliPaiNavSourceMetadata(
            sourceKey = "home:BV1",
            sourceRoute = "home",
            clickedBoundsRecorded = true,
            cardFullyVisible = true
        )

        assertTrue(metadata.sharedTransitionEntryReady)
        assertTrue(metadata.sharedTransitionReady)
        assertEquals("home", metadata.sourceRoute)
        assertEquals("home:BV1", metadata.sourceKey)
    }

    @Test
    fun missingBoundsOrInvisibleCardUsesFallbackMotion() {
        assertFalse(
            BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = false,
                cardFullyVisible = true
            ).sharedTransitionEntryReady
        )
        assertFalse(
            BiliPaiNavSourceMetadata(
                sourceKey = "home:BV1",
                sourceRoute = "home",
                clickedBoundsRecorded = false,
                cardFullyVisible = true
            ).sharedTransitionReady
        )
        val partiallyVisibleCard = BiliPaiNavSourceMetadata(
            sourceKey = "home:BV1",
            sourceRoute = "home",
            clickedBoundsRecorded = true,
            cardFullyVisible = false
        )
        assertTrue(partiallyVisibleCard.sharedTransitionEntryReady)
        assertFalse(partiallyVisibleCard.sharedTransitionReady)
    }

    @Test
    fun resolverNormalizesSourceRouteAndKeepsSharedReadyInputsTogether() {
        val metadata = resolveBiliPaiNavSourceMetadata(
            sourceKey = "home:BV1",
            sourceRoute = "home?from=tab",
            clickedBoundsRecorded = true,
            cardFullyVisible = true
        )

        assertEquals("home", metadata.sourceRoute)
        assertEquals("home:BV1", metadata.sourceKey)
        assertTrue(metadata.sharedTransitionEntryReady)
        assertTrue(metadata.sharedTransitionReady)
    }
}
