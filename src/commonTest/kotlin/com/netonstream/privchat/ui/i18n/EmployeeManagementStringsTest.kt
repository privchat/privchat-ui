package com.netonstream.privchat.ui.i18n

import kotlin.test.*

class EmployeeManagementStringsTest {
    @Test fun fiveTypedPacksCoverPreviewSafetyAndRecovery() {
        val packs = listOf("en", "zh-CN", "zh-TW", "ja", "vi").map(::employeeManagementStringsFor)
        assertEquals(5, packs.map { it.rebind }.distinct().size)
        packs.forEach { p ->
            listOf(p.detail, p.manage, p.edit, p.rebind, p.binding, p.currentBinding, p.targetBinding, p.chooseSession,
                p.newSession, p.existingSession, p.preview, p.confirm, p.impact, p.historyConsent, p.privateOnly,
                p.pendingOther, p.conflict, p.ownerOnly, p.pendingNew, p.noSessionTitle, p.keepEditing,
                p.discardTitle, p.discardMessage, p.nativeSession, p.inFlight, p.reloadBinding, p.reloadBindingHint)
                .forEach { assertTrue(it.isNotBlank()) }
        }
    }
    @Test fun localeVariantsAndFallbackUseCompletePacks() {
        assertEquals("管理员工", employeeManagementStringsFor("zh-CN").manage)
        assertEquals(employeeManagementStringsFor("zh-TW"), employeeManagementStringsFor("zh-Hant"))
        assertEquals(employeeManagementStringsFor("zh-TW"), employeeManagementStringsFor("zh-HK"))
        assertEquals(employeeManagementStringsFor("en"), employeeManagementStringsFor("fr"))
    }
}
