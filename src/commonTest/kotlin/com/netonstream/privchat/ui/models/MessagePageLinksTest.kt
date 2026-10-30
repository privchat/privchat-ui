package com.netonstream.privchat.ui.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagePageLinksTest {
    private val task = """{"text":"修复按钮","route":"workspace.task","workspace_id":"5","work_item_id":"12"}"""

    @Test fun linksComeFromExtraOrItsMetadataWithTheirRouteAndParams() {
        val expected = listOf(MessagePageLink("修复按钮", "workspace.task", mapOf("workspace_id" to "5", "work_item_id" to "12")))
        assertEquals(expected, messagePageLinks("""{"links":[$task]}"""))
        assertEquals(expected, messagePageLinks("""{"metadata":{"links":[$task]}}"""))
    }

    @Test fun malformedOrAbsentLinksShowNothing() {
        assertTrue(messagePageLinks(null).isEmpty())
        assertTrue(messagePageLinks("").isEmpty())
        assertTrue(messagePageLinks("not json \"links\"").isEmpty())
        assertTrue(messagePageLinks("""{"links":[{"text":"","route":"r"},{"text":"x"},{"route":"r"},1]}""").isEmpty())
    }

    @Test fun aLinkTakesItsFirstOccurrenceAndNeverOverlapsAnother() {
        val text = "「修复按钮」卡住了\n原因：修复按钮失败"
        val a = MessagePageLink("修复按钮", "workspace.task", emptyMap())
        val b = MessagePageLink("修复按钮", "other", emptyMap())
        val ranges = pageLinkRanges(text, listOf(a, b))
        assertEquals(listOf(1 until 5, text.lastIndexOf("修复按钮").let { it until it + 4 }), ranges.map { it.first })
        assertEquals(listOf(a, b), ranges.map { it.second })
        assertTrue(pageLinkRanges(text, listOf(MessagePageLink("不在文中", "r", emptyMap()))).isEmpty())
    }
}
