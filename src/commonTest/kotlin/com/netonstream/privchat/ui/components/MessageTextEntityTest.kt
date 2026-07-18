package com.netonstream.privchat.ui.components

import com.netonstream.privchat.sdk.dto.MessageTextEntity
import com.netonstream.privchat.sdk.dto.MessageTextEntityType
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageTextEntityTest {
    @Test
    fun phone_entity_preserves_the_complete_message() {
        val text = "本人姓名：陈思雅\n身份证：422124195812090021\n手机号码：13684915671\n目前工作:做保洁"
        val phone = "13684915671"
        val start = text.indexOf(phone)
        val entities = validTextEntities(
            text,
            listOf(
                MessageTextEntity(
                    type = MessageTextEntityType.Phone,
                    start = start,
                    end = start + phone.length,
                    text = phone,
                    value = phone,
                ),
            ),
        )

        assertEquals(listOf(phone), entities.map { it.text })
        assertEquals(
            text,
            buildString {
                var cursor = 0
                entities.forEach {
                    append(text.substring(cursor, it.start))
                    append(it.text)
                    cursor = it.end
                }
                append(text.substring(cursor))
            },
        )
    }

    @Test
    fun invalid_and_overlapping_entities_are_ignored() {
        val text = "电话 13684915671"
        val start = text.indexOf("13684915671")
        val valid = MessageTextEntity(
            MessageTextEntityType.Phone,
            start,
            text.length,
            "13684915671",
            "13684915671",
        )
        val invalid = valid.copy(start = start + 1, text = "3684915671")

        assertEquals(listOf(valid), validTextEntities(text, listOf(invalid, valid)))
    }

    @Test
    fun arbitrary_continuous_digits_remain_clickable_numbers() {
        val text = "身份证：422124195812090021，年限：16"
        val identity = "422124195812090021"
        val years = "16"
        val entities = listOf(identity, years).map { number ->
            val start = text.indexOf(number)
            MessageTextEntity(
                MessageTextEntityType.Number,
                start,
                start + number.length,
                number,
                number,
            )
        }

        assertEquals(entities, validTextEntities(text, entities))
    }
}
