package com.netonstream.privchat.ui.models

import com.netonstream.privchat.sdk.dto.MessageEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A page link the server attached to a message (privchat-protocol `MessageLink`, found at
 * `metadata.links` in the message extra):
 * [text] is shown underlined where it appears in the message, and tapping it opens the page named by
 * [route] with [params]. Which pages exist is the host app's business; this layer only shows and reports.
 */
data class MessagePageLink(val text: String, val route: String, val params: Map<String, String>)

fun MessageEntry.pageLinks(): List<MessagePageLink> = messagePageLinks(extra)

fun messagePageLinks(extra: String?): List<MessagePageLink> {
    if (extra.isNullOrBlank() || !extra.contains("\"links\"")) return emptyList()
    val root = runCatching { Json.parseToJsonElement(extra) }.getOrNull() as? JsonObject ?: return emptyList()
    val links = (root["links"] ?: (root["metadata"] as? JsonObject)?.get("links")) as? JsonArray ?: return emptyList()
    return links.mapNotNull { element ->
        val link = element as? JsonObject ?: return@mapNotNull null
        fun string(value: Any?) = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        val text = string(link["text"])?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val route = string(link["route"])?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val params = (link["params"] as? JsonObject)?.mapNotNull { (key, value) -> string(value)?.let { key to it } }?.toMap()
        MessagePageLink(text, route, params ?: emptyMap())
    }
}

/** Where each link sits in [text]: its first occurrence not overlapping an earlier link, as UTF-16 [start, end). */
internal fun pageLinkRanges(text: String, links: List<MessagePageLink>): List<Pair<IntRange, MessagePageLink>> {
    val taken = mutableListOf<IntRange>()
    fun free(range: IntRange) = taken.none { it.first <= range.last && range.first <= it.last }
    return links.mapNotNull { link ->
        generateSequence(text.indexOf(link.text)) { text.indexOf(link.text, it + 1).takeIf { next -> next >= 0 } }
            .takeWhile { it >= 0 }
            .map { it until it + link.text.length }
            .firstOrNull(::free)
            ?.also { taken += it }
            ?.let { it to link }
    }.sortedBy { it.first.first }
}
