package net.hypixel.nerdbot.marmalade.storage.database.model.user.birthday;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import net.hypixel.nerdbot.marmalade.json.adapter.BirthDateAdapter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Setter
public class BirthdayData {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    @JsonAdapter(BirthDateAdapter.class)
    @SerializedName(value = "birthDate", alternate = {"birthdayTimestamp", "birthday"})
    private LocalDate birthDate;
    private boolean shouldAnnounceAge;
    private String timezone;
    private int lastAnnouncedYear;

    public ZoneId getTimeZoneId() {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE;
        }

        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return DEFAULT_ZONE;
        }
    }

    public boolean isBirthdaySet() {
        return birthDate != null;
    }

    // lastAnnouncedYear survives so removing and re-adding a birthday can't trigger a second announcement
    public void clearBirthday() {
        birthDate = null;
        shouldAnnounceAge = false;
        timezone = null;
    }

    public LocalDate getBirthdayIn(int year) {
        if (!isBirthdaySet()) {
            throw new IllegalStateException("Birthday is not set");
        }

        // withYear moves 29 February to 28 February in non-leap years
        return birthDate.withYear(year);
    }

    public int getAgeOn(LocalDate date) {
        int age = date.getYear() - birthDate.getYear();
        return date.isBefore(getBirthdayIn(date.getYear())) ? age - 1 : age;
    }

    public String getFormattedDisplay() {
        if (!isBirthdaySet()) {
            return "Not Set";
        }

        return DISPLAY_FORMAT.format(birthDate) + " (" + getTimeZoneId().getId() + ")";
    }
}
