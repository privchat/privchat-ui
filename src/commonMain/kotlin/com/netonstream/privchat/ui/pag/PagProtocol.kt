package com.netonstream.privchat.ui.pag

import kotlinx.serialization.json.*

object PagProtocol {
    fun configuration(request: PagPlaybackRequest): String = buildJsonObject {
        put("url", request.resource.url)
        put("sha256", request.resource.sha256)
        put("bytes", request.resource.size)
        put("usage", request.usage.wire)
        put("requestId", request.requestId)
        put("loop", request.loop)
        put("width", request.resource.width)
        put("height", request.resource.height)
        put("durationMs", request.resource.durationMs)
    }.toString()

    fun event(raw: String): PagEvent? = runCatching {
        if (raw.length > 4096) return null
        val json = Json.parseToJsonElement(raw).jsonObject
        val id = json["requestId"]?.jsonPrimitive?.takeIf { it.isString }?.content ?: return null
        if (id.isBlank() || id.length > 128) return null
        val type = json["type"]?.jsonPrimitive?.takeIf { it.isString }?.content ?: return null
        val event = PagEventType.entries.firstOrNull { it.wire == type } ?: return null
        PagEvent(id, event, json["code"]?.jsonPrimitive?.takeIf { it.isString }?.content?.take(128))
    }.getOrNull()
}
