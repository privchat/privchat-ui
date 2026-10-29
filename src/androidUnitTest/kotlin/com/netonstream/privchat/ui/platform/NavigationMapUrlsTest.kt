package com.netonstream.privchat.ui.platform

import com.netonstream.privchat.ui.utils.CoordinateConverter
import java.net.URLDecoder
import kotlin.test.*

class NavigationMapUrlsTest {
    private fun url(id: String, crs: String? = "wgs84", lat: Double = 39.9, lng: Double = 116.4,
                    label: String? = "Destination", ios: Boolean = false, key: String? = null) =
        NavigationMapUrls.url(ios, id, lat, lng, crs, label, key)

    private fun query(url: String): Map<String, String> = url.substringAfter('?').split('&').associate {
        it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8")
    }

    @Test fun onlyExactNativeProvidersAndPackagesAreAllowed() {
        assertEquals(mapOf("google" to "com.google.android.apps.maps", "amap" to "com.autonavi.minimap",
            "baidu" to "com.baidu.BaiduMap", "tencent" to "com.tencent.map"), NavigationMapUrls.androidPackages)
        assertEquals(setOf("maps", "comgooglemaps", "iosamap", "baidumap", "qqmap"), NavigationMapUrls.iosSchemes.values.toSet())
        for (id in listOf("browser", "https", "android:com.other.app", "AMAP", "com.autonavi.minimap", "")) {
            assertNull(url(id)); assertNull(url(id, ios = true))
        }
    }

    @Test fun invalidCoordinatesFailBeforeAnyUrlIsBuilt() {
        for (id in NavigationMapUrls.androidPackages.keys) for (ios in listOf(false, true)) {
            for (lat in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -90.1, 90.1))
                assertNull(url(id, lat = lat, ios = ios, key = "unit-test-key"))
            for (lng in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -180.1, 180.1))
                assertNull(url(id, lng = lng, ios = ios, key = "unit-test-key"))
        }
    }

    @Test fun zeroAndBoundaryCoordinatesAreValid() {
        for ((lat, lng) in listOf(0.0 to 0.0, -90.0 to -180.0, 90.0 to 180.0)) {
            assertTrue(NavigationMapUrls.validPoint(lat, lng))
            assertNotNull(url("google", lat = lat, lng = lng))
        }
    }

    @Test fun unknownCrsFailsClosedAndDocumentedAliasesAreSupported() {
        for (crs in listOf("unknown", "", "gcj02&dev=1", "EPSG:3857", " wgs84 ")) {
            assertNull(url("google", crs)); assertNull(url("amap", crs)); assertNull(url("baidu", crs))
        }
        assertEquals("wgs84", NavigationMapUrls.normalizedCrs(null))
        assertEquals("wgs84", NavigationMapUrls.normalizedCrs("WGS-84"))
        assertEquals("gcj02", NavigationMapUrls.normalizedCrs("GCJ-02"))
        assertEquals("bd09ll", NavigationMapUrls.normalizedCrs("bd-09"))
    }

    @Test fun googleReceivesWgs84NotGcj02() {
        val expected = CoordinateConverter.gcj02ToWgs84(39.9, 116.4)
        assertEquals("google.navigation:q=${expected.first},${expected.second}&mode=d", url("google", "gcj02"))
        val ios = query(assertNotNull(url("google", "gcj02", ios = true)))
        assertEquals("${expected.first},${expected.second}", ios["daddr"])
        assertEquals("driving", ios["directionsmode"])
    }

    @Test fun appleNativePointUsesWgs84AndBd09IsConvertedViaGcj02() {
        val gcj = CoordinateConverter.bd09ToGcj02(39.9, 116.4)
        val expected = CoordinateConverter.gcj02ToWgs84(gcj.first, gcj.second)
        assertEquals(expected, NavigationMapUrls.wgsPoint(39.9, 116.4, "bd09"))
        assertEquals(0.0 to 0.0, NavigationMapUrls.wgsPoint(0.0, 0.0, "wgs84"))
        assertNull(NavigationMapUrls.wgsPoint(Double.NaN, 0.0, "wgs84"))
    }

    @Test fun amapWgs84RequestsProviderOffsetExactlyOnce() {
        for (ios in listOf(false, true)) {
            val uri = assertNotNull(url("amap", ios = ios))
            assertTrue(uri.startsWith(if (ios) "iosamap://navi?" else "androidamap://navi?"))
            val q = query(uri)
            assertEquals("39.9", q["lat"]); assertEquals("116.4", q["lon"])
            assertEquals("1", q["dev"])
        }
    }

    @Test fun amapGcj02IsNotOffsetAgainAndBd09IsConverted() {
        val gcj = query(assertNotNull(url("amap", "gcj02")))
        assertEquals("39.9", gcj["lat"]); assertEquals("116.4", gcj["lon"]); assertEquals("0", gcj["dev"])
        val expected = CoordinateConverter.bd09ToGcj02(39.9, 116.4)
        val bd = query(assertNotNull(url("amap", "bd09")))
        assertEquals(expected.first.toString(), bd["lat"])
        assertEquals(expected.second.toString(), bd["lon"]); assertEquals("0", bd["dev"])
    }

    @Test fun baiduPreservesCoordinatesWithAnExplicitSourceCrs() {
        for ((source, expected) in listOf("wgs84" to "wgs84", "gcj02" to "gcj02", "bd09" to "bd09ll")) {
            for (ios in listOf(false, true)) {
                val uri = assertNotNull(url("baidu", source, ios = ios))
                assertTrue(uri.startsWith("baidumap://map/direction?"))
                val q = query(uri)
                assertEquals("name:Destination|latlng:39.9,116.4", q["destination"])
                assertEquals("我的位置", q["origin"])
                assertEquals("driving", q["mode"])
                assertEquals(expected, q["coord_type"])
                assertEquals(setOf("origin", "destination", "coord_type", "mode", "src"), q.keys)
                assertEquals(if (ios) "ios.privchat.app" else "andr.privchat.app", q["src"])
            }
        }
    }

    @Test fun labelsAreUtf8EncodedWithoutQueryOrFragmentInjection() {
        val label = "北京 A&B +/#?=|%🚗"
        assertEquals(label, URLDecoder.decode(NavigationMapUrls.encode(label), "UTF-8"))
        for (id in listOf("amap", "tencent")) {
            val uri = assertNotNull(url(id, label = label, key = "unit-test-key"))
            assertFalse(uri.contains('#')); assertFalse(uri.contains(' ')); assertFalse(uri.contains("A&B"))
            val nameKey = if (id == "amap") "poiname" else "to"
            assertEquals(label, query(uri)[nameKey])
        }
    }

    @Test fun baiduDestinationLabelIsEncodedWithoutChangingCoordinates() {
        val label = "北京 A&B +/#?=:%🚗"
        for (ios in listOf(false, true)) {
            val uri = assertNotNull(url("baidu", label = label, ios = ios))
            assertFalse(uri.contains('#')); assertFalse(uri.contains(' ')); assertFalse(uri.contains("A&B"))
            val q = query(uri)
            assertEquals("name:$label|latlng:39.9,116.4", q["destination"])
            assertEquals(5, q.size)
        }
    }

    @Test fun baiduMissingOrGrammarDelimiterLabelsKeepCoordinateOnlyDestination() {
        for (label in listOf(null, "", "  ", "POI|latlng:0,0", "北京 A&B +/#?=|%🚗")) {
            for (ios in listOf(false, true)) {
                val q = query(assertNotNull(url("baidu", label = label, ios = ios)))
                assertEquals("39.9,116.4", q["destination"])
                assertEquals("wgs84", q["coord_type"])
                assertEquals(5, q.size)
            }
        }
    }

    @Test fun tencentRequiresARealHostKeyAndUsesGcj02() {
        for (key in listOf(null, "", " \n")) assertNull(url("tencent", key = key))
        val expected = CoordinateConverter.wgs84ToGcj02(39.9, 116.4)
        val q = query(assertNotNull(url("tencent", key = "unit-test-key")))
        assertEquals("${expected.first},${expected.second}", q["tocoord"])
        assertEquals("CurrentLocation", q["fromcoord"])
        assertEquals("unit-test-key", q["referer"])
        assertEquals("39.9,116.4", query(assertNotNull(url("tencent", "gcj02", key = "unit-test-key")))["tocoord"])
    }

    @Test fun noAndroidHostMeansNoInstalledChoicesOrNavigation() {
        ExternalLinkBridge.unregister()
        assertTrue(ExternalLinkBridge.installedNavigationMaps().isEmpty())
        for (id in NavigationMapUrls.androidPackages.keys) assertFalse(ExternalLinkBridge.navigateTo(id, 39.9, 116.4, "wgs84", "Destination"))
        assertFalse(ExternalLinkBridge.openMap(Double.NaN, 116.4, "wgs84", "Destination"))
    }
}
