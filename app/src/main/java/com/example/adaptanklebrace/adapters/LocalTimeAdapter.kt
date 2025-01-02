package com.example.adaptanklebrace.adapters

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.adaptanklebrace.data.Exercise.CREATOR.formatter
import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.Q)
class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    override fun serialize(src: LocalTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalTime {
        // Ensure we are handling the JsonElement correctly as a string
        return if (json != null && json.isJsonPrimitive) {
            LocalTime.parse(json.asString, formatter)
        } else {
            throw JsonParseException("Expected a string for LocalTime, but found: ${json?.javaClass?.name}")
        }
    }
}
