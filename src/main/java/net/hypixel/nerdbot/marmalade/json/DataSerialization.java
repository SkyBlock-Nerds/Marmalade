package net.hypixel.nerdbot.marmalade.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.hypixel.nerdbot.marmalade.json.adapter.InstantTypeAdapter;
import net.hypixel.nerdbot.marmalade.json.adapter.UUIDTypeAdapter;
import net.hypixel.nerdbot.marmalade.storage.badge.Badge;
import net.hypixel.nerdbot.marmalade.storage.badge.BadgeTypeAdapter;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSerialization {

    /**
     * GSON instance for compact output operations.
     */
    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(Badge.class, new BadgeTypeAdapter())
        .create();

    /**
     * GSON instance for human-readable output (config files, debug, etc.).
     */
    public static final Gson PRETTY_GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .registerTypeAdapter(Badge.class, new BadgeTypeAdapter())
        .create();
}
