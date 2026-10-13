package net.hypixel.nerdbot.marmalade.storage.database.model.reminder;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.hypixel.nerdbot.marmalade.json.adapter.EpochMillisAdapter;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@ToString
public class Reminder {

    private static final String COLLECTION_NAME = "reminders";

    private UUID uuid;
    @BsonIgnore
    private String description;
    private String channelId;
    private String userId;
    @JsonAdapter(EpochMillisAdapter.class)
    private long time;

    public Reminder() {
    }

    public Reminder(String description, Date time, String channelId, String userId) {
        this(description, time.getTime(), channelId, userId);
    }

    public Reminder(String description, long time, String channelId, String userId) {
        this.uuid = UUID.randomUUID();
        this.description = description;
        this.channelId = channelId;
        this.userId = userId;
        this.time = time;
    }

    public Date getTimeAsDate() {
        return new Date(time);
    }

    public String getCollectionName() {
        return COLLECTION_NAME;
    }
}