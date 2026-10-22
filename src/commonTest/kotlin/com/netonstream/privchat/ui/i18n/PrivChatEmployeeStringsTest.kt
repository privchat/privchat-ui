package com.netonstream.privchat.ui.i18n

import kotlin.test.*

class PrivChatEmployeeStringsTest {
    @Test fun employeeLabelsAreWiredForAllFourLanguages() {
        val languages = listOf(enAgent.employees, zhAgent.employees, hantAgent.employees, viAgent.employees)
        assertEquals(4, languages.map { it.title }.distinct().size)
        assertSame(enEmployees, enAgent.employees)
        assertSame(zhEmployees, zhAgent.employees)
        assertSame(hantEmployees, hantAgent.employees)
        assertSame(viEmployees, viAgent.employees)
        languages.forEach { labels ->
            listOf(labels.title, labels.create, labels.edit, labels.nickname, labels.role, labels.sessionTitle,
                labels.executor, labels.model, labels.workdir, labels.newSession, labels.bindSession,
                labels.configOnly, labels.roleHint, labels.immutableHint, labels.managedOnly,
                labels.discoveryUnavailable, labels.noWorkdirs, labels.noExecutors, labels.historyConsent,
                labels.confirmBinding, labels.pending, labels.retry, labels.save, labels.saved,
                labels.send, labels.message, labels.openExecution, labels.refresh, labels.empty,
                labels.noCandidates, labels.modelHint, labels.bindModelHint).forEach { assertTrue(it.isNotBlank()) }
        }
    }
}
