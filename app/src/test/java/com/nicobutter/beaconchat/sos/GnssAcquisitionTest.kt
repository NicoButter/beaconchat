package com.nicobutter.beaconchat.sos

import org.junit.Assert.*
import org.junit.Test

class GnssAcquisitionTest {
    private val acquisition = GnssAcquisition()
    private fun sample(at: Long, mock: Boolean = false) = GpsSample(-51.6, -69.2, 12.5, at, mock)

    @Test fun timeoutWithoutFixHasNoCoordinatesAndNeverInventsZero() {
        val token = acquisition.start(100_000, true, true, null, 30_000)!!
        assertFalse(acquisition.timeout(token, 129_999))
        assertTrue(acquisition.timeout(token, 130_000))
        assertEquals(GnssStatus.NO_POSITION, acquisition.snapshot(130_000).status)
        assertNull(acquisition.snapshot(130_000).position)
    }
    @Test fun deniedOrApproximateOnlyPermissionDoesNotExposeCachedPosition() {
        assertNull(acquisition.start(100_000, false, true, sample(90_000), 30_000))
        assertEquals(GnssStatus.PERMISSION_REQUIRED, acquisition.snapshot(100_000).status)
        assertNull(acquisition.snapshot(100_000).position)
    }
    @Test fun gpsDisabledDistinguishesCachedPositionFromNoPosition() {
        assertNull(acquisition.start(100_000, true, false, sample(99_999), 30_000))
        assertEquals(GnssStatus.LAST_KNOWN, acquisition.snapshot(100_000).status)
        assertEquals(PositionValidity.LAST_KNOWN, acquisition.snapshot(100_000).position!!.validity)
        assertNull(acquisition.start(100_000, true, false, null, 30_000))
        assertEquals(GnssStatus.NO_POSITION, acquisition.snapshot(100_000).status)
    }
    @Test fun cachedEvenAgeZeroIsNeverPresentedAsNewFix() {
        val token = acquisition.start(100_000, true, true, sample(100_000), 1000)!!
        assertEquals(PositionValidity.LAST_KNOWN, acquisition.snapshot(100_000).position!!.validity)
        acquisition.timeout(token, 101_000)
        assertEquals(PositionValidity.LAST_KNOWN, acquisition.snapshot(101_000).position!!.validity)
    }
    @Test fun freshUpdateRecordsAccuracySourceAndMonotonicAge() {
        val token = acquisition.start(100_000, true, true, null, 30_000)!!
        assertTrue(acquisition.location(token, sample(101_000), 102_000))
        val state = acquisition.snapshot(102_000)
        val position = state.position!!
        assertEquals(GnssStatus.CURRENT, state.status)
        assertEquals(1000L, position.ageMillis)
        assertEquals(12.5, position.accuracyMeters!!, 0.0)
        assertEquals(PositionSource.GNSS, position.source)
        assertFalse(acquisition.searching)
    }
    @Test fun staleCallbackRemainsFallbackUntilTimeoutAndIsExplicitlyStale() {
        val token = acquisition.start(400_000, true, true, null, 30_000)!!
        assertFalse(acquisition.location(token, sample(1), 400_001))
        assertEquals(GnssStatus.SEARCHING, acquisition.snapshot(400_001).status)
        acquisition.timeout(token, 430_000)
        assertEquals(PositionValidity.STALE, acquisition.snapshot(430_000).position!!.validity)
        assertEquals(429_999L, acquisition.snapshot(430_000).position!!.ageMillis)
    }
    @Test fun currentFixAgesIntoLastKnownAndStaleWithoutMovingCoordinates() {
        val token = acquisition.start(100_000, true, true, null, 30_000)!!
        acquisition.location(token, sample(100_000), 100_001)
        assertEquals(PositionValidity.CURRENT, acquisition.snapshot(130_000).position!!.validity)
        val older = acquisition.snapshot(130_001)
        assertEquals(GnssStatus.LAST_KNOWN, older.status)
        assertEquals(PositionValidity.LAST_KNOWN, older.position!!.validity)
        assertTrue(older.detail.contains("no es ubicación actual"))
        assertEquals(PositionValidity.STALE, acquisition.snapshot(400_001).position!!.validity)
    }
    @Test fun oldAndCancelledCallbacksCannotCompleteNewRequest() {
        val old = acquisition.start(100_000, true, true, null, 30_000)!!
        acquisition.cancel()
        assertFalse(acquisition.location(old, sample(100_000), 100_001))
        assertNull(acquisition.snapshot(100_001).position)
        val current = acquisition.start(100_001, true, true, null, 30_000)!!
        assertNotEquals(old, current)
        assertFalse(acquisition.location(old, sample(100_001), 100_002))
        assertFalse(acquisition.timeout(old, 140_000))
        assertTrue(acquisition.location(current, sample(100_002), 100_003))
    }
    @Test fun fixAtOrAfterDeadlineCannotBecomeCurrent() {
        val token = acquisition.start(100_000, true, true, null, 1000)!!
        assertFalse(acquisition.location(token, sample(101_000), 101_000))
        assertEquals(GnssStatus.NO_POSITION, acquisition.snapshot(101_000).status)
    }
    @Test fun futureMonotonicTimestampIsRejectedForCacheAndCallback() {
        val token = acquisition.start(100_000, true, true, sample(100_001), 30_000)!!
        assertNull(acquisition.snapshot(100_000).position)
        assertFalse(acquisition.location(token, sample(100_002), 100_001))
        assertTrue(acquisition.searching)
    }
    @Test fun permissionRevocationAndCancellationDiscardSensitiveState() {
        val token = acquisition.start(100_000, true, true, null, 30_000)!!
        acquisition.location(token, sample(100_000), 100_001)
        acquisition.error("Permiso revocado")
        assertNull(acquisition.snapshot(100_001).position)
        acquisition.cancel()
        assertEquals(GnssStatus.CANCELLED, acquisition.snapshot(100_001).status)
    }
    @Test fun mockLocationIsLabelledMockNotReclassifiedAsSafeSyntheticData() {
        val token = acquisition.start(100_000, true, true, null, 30_000)!!
        acquisition.location(token, sample(100_000, true), 100_001)
        val position = acquisition.snapshot(100_001).position!!
        assertEquals(PositionSource.MOCK_GPS, position.source)
        assertFalse(SosMeshPolicy.permitted(incident(position)))
    }
    @Test fun timeoutAndSampleConfigurationRejectInvalidValues() {
        listOf(-1L, 0L, 120_001L, Long.MAX_VALUE).forEach { timeout ->
            rejectsSos { acquisition.start(100_000, true, true, null, timeout) }
        }
        rejectsSos { GpsSample(Double.NaN, 0.0, 10.0, 1) }
        rejectsSos { GpsSample(0.0, 0.0, -1.0, 1) }
        rejectsSos { GpsSample(0.0, 0.0, 10.0, -1) }
        val token = acquisition.start(0, true, true, null, 120_000)!!
        assertTrue(acquisition.timeout(token, 120_000))
    }
    @Test fun providerDisabledMidRequestKeepsOnlyLabelledFallback() {
        acquisition.start(100_000, true, true, sample(99_000), 30_000)
        acquisition.unavailable(101_000, "GPS apagado")
        assertFalse(acquisition.searching)
        assertEquals(GnssStatus.LAST_KNOWN, acquisition.snapshot(101_000).status)
        assertEquals(2000L, acquisition.snapshot(101_000).position!!.ageMillis)
    }
}
