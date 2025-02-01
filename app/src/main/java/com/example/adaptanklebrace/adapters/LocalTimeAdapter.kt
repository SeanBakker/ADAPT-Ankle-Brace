package com.example.adaptanklebrace.adapters

import com.example.adaptanklebrace.utils.GeneralUtil
import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalTime

class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    override fun serialize(src: LocalTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(GeneralUtil.timeFormatter))
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalTime {
        // Ensure we are handling the JsonElement correctly as a string
        return if (json != null && json.isJsonPrimitive) {
            LocalTime.parse(json.asString, GeneralUtil.timeFormatter)
        } else {
            throw JsonParseException("Expected a string for LocalTime, but found: ${json?.javaClass?.name}")
        }
    }
}
