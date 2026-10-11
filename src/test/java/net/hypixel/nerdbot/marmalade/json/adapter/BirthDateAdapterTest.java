package net.hypixel.nerdbot.marmalade.json.adapter;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BirthDateAdapterTest {

    private static final Gson GSON = new Gson();

    static class Holder {
        @JsonAdapter(BirthDateAdapter.class)
        LocalDate date;
    }

    private static LocalDate read(String dateJson) {
        return GSON.fromJson("{\"date\": " + dateJson + "}", Holder.class).date;
    }

    @Test
    void readsIsoDate() {
        assertThat(read("\"2007-10-04\"")).isEqualTo(LocalDate.of(2007, 10, 4));
    }

    @Test
    void readsLegacyMillisAsUtcDate() {
        assertThat(read("1191456000000")).isEqualTo(LocalDate.of(2007, 10, 4));
        assertThat(read("{\"$numberLong\": \"1191456000000\"}")).isEqualTo(LocalDate.of(2007, 10, 4));
        assertThat(read("{\"$date\": \"2007-10-04T00:00:00Z\"}")).isEqualTo(LocalDate.of(2007, 10, 4));
        assertThat(read("\"2007-10-04T00:00:00.000Z\"")).isEqualTo(LocalDate.of(2007, 10, 4));
    }

    @Test
    void readsLegacyPre1970Millis() {
        assertThat(read("{\"$date\": {\"$numberLong\": \"-20000000000\"}}")).isEqualTo(LocalDate.of(1969, 5, 14));
    }

    @Test
    void legacyUnsetSentinelReadsAsNull() {
        assertThat(read("-1")).isNull();
        assertThat(read("{\"$numberLong\": \"-1\"}")).isNull();
    }

    @Test
    void nullAndGarbageReadAsNull() {
        assertThat(read("null")).isNull();
        assertThat(read("\"not a date\"")).isNull();
    }

    @Test
    void writesIsoDateThatSurvivesABsonRoundTrip() {
        Holder holder = new Holder();
        holder.date = LocalDate.of(1955, 2, 28);

        String stored = Document.parse(GSON.toJson(holder)).toJson();

        assertThat(stored).contains("\"1955-02-28\"");
        assertThat(GSON.fromJson(stored, Holder.class).date).isEqualTo(LocalDate.of(1955, 2, 28));
    }
}
