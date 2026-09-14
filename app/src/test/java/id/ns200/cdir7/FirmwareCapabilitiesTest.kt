package id.ns200.cdir7

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirmwareCapabilitiesTest {
    @Test fun parsesProtocolV5() {
        val c=FirmwareCapabilities.parse("CAPS,5,30000,-300,800,32,16,4,12,FAN|DYNO|OTA".split(','))
        assertEquals(5,c.protocolVersion)
        assertEquals(30000,c.rpmMax)
        assertEquals(-30f,c.advanceMinDeg)
        assertEquals(80f,c.advanceMaxDeg)
        assertEquals(12,c.maxPulserPpr)
        assertTrue("FAN" in c.features)
    }

    @Test fun legacyFallbackStaysCompatible() {
        val c=FirmwareCapabilities.parse(listOf("CAPS","PRO","OTA"))
        assertEquals(3,c.protocolVersion)
        assertEquals(11500,c.rpmMax)
        assertTrue("PRO" in c.features)
    }

    @Test fun fanPolicyKeepsHysteresisAndFailsSafe() {
        val s=ThermalFanPolicy.normalize("AUTO",5100,5050)
        assertEquals(5300,s.onCdeg)
        assertEquals(5000,s.offCdeg)
        assertFalse(ThermalFanPolicy.output(s,4900,false,true))
        assertTrue(ThermalFanPolicy.output(s,6000,false,false))
        assertTrue(ThermalFanPolicy.output(s,2000,true,false))
    }
}
