package com.netonstream.privchat.ui.effect

import com.netonstream.privchat.ui.pag.*
import com.netonstream.privchat.ui.sticker.canResolveStickerPag
import com.netonstream.privchat.app.pag.pagEventPayload
import java.text.Normalizer
import kotlin.test.*
import kotlinx.serialization.json.*

class PagEffectFocusedTest {
    private class Fixture {
        var wall = 1_000_000L
        var mono = 100L
        var runtime = true
        val session = ChatEffectSession("https://one/app", 7u, 1)
        val pag = PagRepresentation("90", "https://one/resource/90", "ab".repeat(32), 1024, 320, 180, 1000, PagRepresentation.EFFECT_PROFILE)
        val resource = ChatEffectResource("hearts", 1, pag)
        val rule = ChatEffectRule("love", 1, "hearts", 1, listOf("caf\u00e9", "hello"))
        val c = ChatEffectCoordinator({ Normalizer.normalize(it, Normalizer.Form.NFC) }, { wall }, { mono }, { runtime })
        fun snapshot(revision: Long = 1, rules: List<ChatEffectRule> = listOf(rule), resources: List<ChatEffectResource> = listOf(resource)) = ChatEffectSnapshot(session,revision,wall+100_000,rules,resources)
        init { c.session(session); c.setting(true); c.setVisible("10"); c.replaceRules(snapshot()) }
        fun event(id: String = "9007199254740993", origin: ChatEffectOrigin = ChatEffectOrigin.LIVE_RECEIVE, sender: String = "8", text: String = "hello") = ChatEffectCandidate(origin,session,"10",id,sender,text,wall)
        fun advance(ms: Long) { mono+=ms; wall+=ms }
        fun complete() { c.active.value?.let { c.onPagEvent(PagEvent(it.requestId,PagEventType.COMPLETE)) } }
    }

    @Test fun normalizedExactTextUsesRealNfcAndNeverSubstringMatching() {
        val f=Fixture()
        assertFalse(f.c.offer(f.event(text="please say hello")))
        assertFalse(f.c.offer(f.event(id="2",text="HELLO")))
        assertTrue(f.c.offer(f.event(id="3",text="  cafe\u0301  ")))
        assertEquals("love",f.c.active.value?.rule?.ruleId)
        val missing=ChatEffectCoordinator({null},{f.wall},{f.mono},{true})
        missing.session(f.session); missing.setting(true); missing.setVisible("10"); missing.replaceRules(f.snapshot())
        assertFalse(missing.offer(f.event()))
    }

    @Test fun onlyExplicitLiveOriginsAndConfirmedOwnSendAreEligible() {
        for (origin in ChatEffectOrigin.entries) {
            val f=Fixture()
            assertEquals(origin==ChatEffectOrigin.LOCAL_SEND_CONFIRMED || origin==ChatEffectOrigin.LIVE_RECEIVE,
                f.c.offer(f.event(origin=origin,sender=if(origin==ChatEffectOrigin.LOCAL_SEND_CONFIRMED) "7" else "8")),origin.name)
        }
        val f=Fixture()
        assertFalse(f.c.offer(f.event(sender="7")))
        assertFalse(f.c.offer(f.event(origin=ChatEffectOrigin.LOCAL_SEND_CONFIRMED,sender="8")))
        assertFalse(f.c.offer(f.event().copy(plainText=false)))
        assertFalse(f.c.offer(f.event().copy(forwarded=true)))
        assertFalse(f.c.offer(f.event().copy(quoted=true)))
    }

    @Test fun idsAreCanonicalU64NeverDoublesOrLocalTemporaryIds() {
        for (id in listOf("", "0", "01", "-1", "+1", "1.0", "1e3", "18446744073709551616", "９", "temp-1")) {
            assertFalse(ChatEffectCoordinator.serverId(id),id)
            assertFalse(Fixture().let { it.c.offer(it.event(id=id)) },id)
        }
        assertTrue(ChatEffectCoordinator.serverId("18446744073709551615"))
        assertTrue(ChatEffectCoordinator.serverId("9007199254740993"))
        val f=Fixture()
        assertFalse(f.c.offer(f.event().copy(channelId="10.0")))
        assertFalse(f.c.offer(f.event().copy(senderId="8.0")))
    }

    @Test fun platformAccountEpochAndExactVisibleChannelGate() {
        val f=Fixture()
        for (session in listOf(f.session.copy(platform="https://other/app"),f.session.copy(account=9u),f.session.copy(epoch=2))) assertFalse(f.c.offer(f.event().copy(session=session)))
        assertFalse(f.c.offer(f.event().copy(channelId="11")))
        assertTrue(f.c.offer(f.event()))
        f.c.setVisible("11")
        assertNull(f.c.active.value)
        f.c.session(f.session.copy(epoch=2))
        assertNull(f.c.active.value)
        assertFalse(f.c.offer(f.event()))
    }

    @Test fun backgroundDisabledBusyCandidatesAreDroppedNeverQueuedOrResumed() {
        val f=Fixture()
        f.c.setForeground(false)
        assertFalse(f.c.offer(f.event()))
        f.c.setForeground(true)
        assertFalse(f.c.offer(f.event()))
        assertTrue(f.c.offer(f.event(id="2")))
        assertFalse(f.c.offer(f.event(id="3")))
        f.complete(); f.advance(30_000)
        assertFalse(f.c.offer(f.event(id="3")))
        assertTrue(f.c.offer(f.event(id="4")))
        f.c.setting(false)
        assertNull(f.c.active.value)
        assertFalse(f.c.offer(f.event(id="5")))
        f.c.setting(true)
        assertFalse(f.c.offer(f.event(id="5")))
    }

    @Test fun deadlineAndLateCallbacksCannotReviveCancelledViews() {
        val f=Fixture()
        assertFalse(f.c.offer(f.event().copy(occurredAtMs=f.wall-5001)))
        assertFalse(f.c.offer(f.event().copy(occurredAtMs=f.wall+1)))
        assertTrue(f.c.offer(f.event()))
        val id=f.c.active.value!!.requestId
        f.c.onPagEvent(PagEvent(id,PagEventType.READY))
        assertNull(f.c.active.value!!.firstFrameAtMonotonicMs)
        f.advance(5001); f.c.onPagEvent(PagEvent(id,PagEventType.FRAME))
        assertNull(f.c.active.value)
        f.c.onPagEvent(PagEvent(id,PagEventType.READY)); assertNull(f.c.active.value)
        f.advance(30_000)
        assertTrue(f.c.offer(f.event(id="2")))
        val next=f.c.active.value!!.requestId
        f.c.onPagEvent(PagEvent(id,PagEventType.ERROR))
        assertEquals(next,f.c.active.value?.requestId)
        f.c.onPagEvent(PagEvent(next,PagEventType.FRAME)); f.advance(1000); f.c.tick()
        assertNull(f.c.active.value)
    }

    @Test fun monotonicPageAndRuleCooldownsCannotBeLoweredByServer() {
        val f=Fixture()
        val second=f.rule.copy(ruleId="other",keywords=listOf("other"),cooldownMs=0)
        f.c.replaceRules(f.snapshot(2,listOf(f.rule.copy(cooldownMs=0),second)))
        assertTrue(f.c.offer(f.event())); f.complete()
        f.advance(9999); assertFalse(f.c.offer(f.event(id="2",text="other")))
        f.advance(1); assertTrue(f.c.offer(f.event(id="3",text="other"))); f.complete()
        f.advance(19_999); assertFalse(f.c.offer(f.event(id="4")))
        f.advance(1); assertTrue(f.c.offer(f.event(id="5")))
    }

    @Test fun deterministicPriorityAndScopeAndUnsupportedPlacement() {
        val f=Fixture()
        val rules=listOf(f.rule.copy(ruleId="z",priority=9),f.rule.copy(ruleId="a",priority=9),f.rule.copy(ruleId="higher",priority=99,placement=ChatEffectPlacement.BUBBLE))
        f.c.replaceRules(f.snapshot(2,rules))
        assertTrue(f.c.offer(f.event())); assertEquals("a",f.c.active.value?.rule?.ruleId)
        val group=Fixture(); group.c.replaceRules(group.snapshot(2,listOf(group.rule.copy(channelScope=ChatEffectChannelScope.GROUP))))
        assertFalse(group.c.offer(group.event())); group.c.setVisible("10",ChatEffectChannelKind.GROUP)
        assertTrue(group.c.offer(group.event(id="2")))
    }

    @Test fun switchingRoomsCannotBypassGlobalAdmissionCooldown() {
        val f=Fixture()
        val second=f.rule.copy(ruleId="second",keywords=listOf("other"),cooldownMs=0)
        f.c.replaceRules(f.snapshot(2,listOf(f.rule,second)))
        assertTrue(f.c.offer(f.event()))
        f.c.setVisible("11") // Cancels the old view, not the global admission clock.
        assertNull(f.c.active.value)
        f.advance(9999)
        assertFalse(f.c.offer(f.event(id="2",text="other").copy(channelId="11")))
        f.advance(1)
        assertTrue(f.c.offer(f.event(id="3",text="other").copy(channelId="11")))
        f.c.setVisible("10")
        f.advance(10_000)
        assertFalse(f.c.offer(f.event(id="4")),"same rule remains cooled for 30s across rooms")
        f.advance(10_000)
        assertTrue(f.c.offer(f.event(id="5")))
    }

    @Test fun snapshotExpiryRevocationRollbackAndCapabilityFailureCancel() {
        val f=Fixture(); assertTrue(f.c.offer(f.event()))
        f.c.replaceRules(f.snapshot(2).copy(revokedResourceIds=setOf("90")))
        assertNull(f.c.active.value)
        f.c.replaceRules(f.snapshot(1)); f.advance(30_000); assertFalse(f.c.offer(f.event(id="2")))
        f.c.replaceRules(f.snapshot(3)); assertTrue(f.c.offer(f.event(id="3")))
        f.runtime=false; f.c.tick(); assertNull(f.c.active.value)
        val g=Fixture(); assertTrue(g.c.offer(g.event())); g.c.replaceRules(g.snapshot(2).copy(schemaVersion=99)); assertNull(g.c.active.value)
        val h=Fixture(); assertTrue(h.c.offer(h.event())); h.advance(100_001); h.c.tick(); assertNull(h.c.active.value)
    }

    @Test fun errorsAndReducedMotionConsumeCandidateAndCloseClearsSession() {
        val f=Fixture(); assertTrue(f.c.offer(f.event()))
        val id=f.c.active.value!!.requestId
        f.c.onPagEvent(PagEvent(id,PagEventType.ERROR,"HASH_MISMATCH")); assertNull(f.c.active.value)
        f.advance(30_000); assertFalse(f.c.offer(f.event()))
        assertTrue(f.c.offer(f.event(id="2")))
        f.c.onPagEvent(PagEvent(f.c.active.value!!.requestId,PagEventType.REDUCED_MOTION))
        assertNull(f.c.active.value); f.advance(30_000); assertFalse(f.c.offer(f.event(id="3")))
        f.c.close(); assertFalse(f.c.offer(f.event(id="4")))
    }

    @Test fun resourcePolicyAllowsOnlyHttpsOrUnambiguousHttpLoopback() {
        for (url in listOf("https://cdn.example/pag/one", "http://127.0.0.1:8080/a", "http://localhost/a", "http://[::1]:8080/a")) assertTrue(PagRepresentation.trustedUrlShape(url),url)
        for (url in listOf("file:///a", "data:abc", "javascript:hi", "http://remote.example/a", "http://127.0.0.1.evil/a", "http://localhost@evil/a", "http://evil@localhost/a", "http://%6cocalhost/a", "http://localhost:0/a", "http://localhost:65536/a", "http://localhost:abc/a", "http://localhost:/a", "https://cdn.example/a#x", "https://cdn.example\\@evil/a")) assertFalse(PagRepresentation.trustedUrlShape(url),url)
        val f=Fixture()
        assertTrue(f.pag.validFor(PagUsage.EFFECT))
        assertFalse(f.pag.validFor(PagUsage.STICKER))
        assertFalse(f.pag.copy(sha256="00").validFor(PagUsage.EFFECT))
        assertFalse(f.pag.copy(width=4096,height=4096).validFor(PagUsage.EFFECT))
        assertTrue(f.pag.copy(width=2048,height=2048).validFor(PagUsage.EFFECT))
        assertFalse(f.pag.copy(width=2049,height=1280).validFor(PagUsage.EFFECT))
    }

    @Test fun protocolFieldsAndEventsMatchNativeContractWithoutLossyMapToString() {
        val f=Fixture()
        val value=Json.parseToJsonElement(PagProtocol.configuration(PagPlaybackRequest("req",f.pag,PagUsage.EFFECT,false))).jsonObject
        assertEquals(setOf("url","sha256","bytes","usage","requestId","loop","width","height","durationMs"),value.keys)
        assertEquals("effect",value["usage"]!!.jsonPrimitive.content)
        assertEquals(false,value["loop"]!!.jsonPrimitive.boolean)
        assertEquals(1024,value["bytes"]!!.jsonPrimitive.long)
        for (type in PagEventType.entries) assertEquals(PagEvent("req",type),pagEventPayload(mapOf("requestId" to "req","type" to type.wire)))
        assertNull(pagEventPayload(mapOf("requestId" to 3,"type" to "frame")))
        assertNull(PagProtocol.event("{\"requestId\":3,\"type\":\"frame\"}"))
        assertNull(PagProtocol.event("{}")); assertNull(PagProtocol.event("{"))
    }

    @Test fun sharedBudgetNoSecondEffectOrUnboundedPlayersAndReleaseIsIdempotent() {
        val budget=PagPlaybackBudget(2)
        val sticker=Any(); val effect=Any()
        assertTrue(budget.acquire(sticker,PagUsage.STICKER))
        assertTrue(budget.acquire(effect,PagUsage.EFFECT))
        assertFalse(budget.acquire(Any(),PagUsage.STICKER)); assertFalse(budget.acquire(Any(),PagUsage.EFFECT))
        budget.release(effect); budget.release(effect); assertEquals(1,budget.size)
        budget.clear(); assertEquals(0,budget.size)
        val capped=PagPlaybackBudget(100)
        repeat(4) { assertTrue(capped.acquire(Any(),PagUsage.STICKER)) }
        assertFalse(capped.acquire(Any(),PagUsage.STICKER))
    }

    @Test fun realKuiklyJsonObjectEventsPreserveTypesAndNestedPayloads() {
        fun obj(vararg fields: Pair<String, Any>) = com.tencent.kuikly.core.nvi.serialization.json.JSONObject().apply {
            fields.forEach { (key, value) -> put(key, value) }
        }
        for (type in PagEventType.entries) {
            val payload = obj("requestId" to "req", "type" to type.wire)
            val expected = PagEvent("req", type)
            assertEquals(expected, pagEventPayload(payload))
            assertEquals(expected, pagEventPayload(obj("data" to payload)))
            assertEquals(expected, pagEventPayload(obj("data" to "{\"requestId\":\"req\",\"type\":\"${type.wire}\"}")))
        }
        assertEquals(PagEvent("req", PagEventType.ERROR, "LOAD_FAILED"), pagEventPayload(obj("requestId" to "req", "type" to "error", "code" to "LOAD_FAILED")))
        assertNull(pagEventPayload(obj("requestId" to 3, "type" to "frame")))
        assertNull(pagEventPayload(obj("requestId" to "req", "type" to 3)))
        assertNull(pagEventPayload(obj("data" to "{")))
    }

    @Test fun historicalStickerMappingNeverGuessesForeignOrMismatchedIdentity() {
        assertTrue(canResolveStickerPag("9007199254740993","https://one/app/sticker/resource/get/9007199254740993","https://one/app"))
        assertFalse(canResolveStickerPag("1","https://other/app/sticker/resource/get/1","https://one/app"))
        assertFalse(canResolveStickerPag("1","https://one/app/sticker/resource/get/2","https://one/app"))
        assertFalse(canResolveStickerPag("1","https://one/app/sticker/resource/get/1?untrusted=1","https://one/app"))
        assertFalse(canResolveStickerPag("01","https://one/app/sticker/resource/get/01","https://one/app"))
        assertFalse(canResolveStickerPag("1",null,"https://one/app"))
    }
}
