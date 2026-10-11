package net.hypixel.nerdbot.marmalade.json.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Slf4j
public class BirthDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    // Birthdays used to be stored as epoch millis at UTC midnight, with -1 meaning "not set"
    private static final long LEGACY_UNSET = -1L;

    @Override
    public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
        return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
    }

    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        if (json == null || json.isJsonNull()) {
            return null;
        }

        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            try {
                return LocalDate.parse(json.getAsString());
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            long epochMillis = EpochMillisAdapter.parseEpochMillis(json);
            return epochMillis == LEGACY_UNSET
                ? null
                : LocalDate.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        } catch (RuntimeException exception) {
            // Losing one unreadable birthday is better than failing to load the whole user
            log.warn("Ignoring unreadable birth date {}", json, exception);
            return null;
        }
    }
}
