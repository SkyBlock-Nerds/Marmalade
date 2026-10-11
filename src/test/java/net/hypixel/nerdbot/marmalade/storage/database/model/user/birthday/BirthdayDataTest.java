package net.hypixel.nerdbot.marmalade.storage.database.model.user.birthday;

import net.hypixel.nerdbot.marmalade.storage.database.model.user.DiscordUser;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BirthdayDataTest {

    private static BirthdayData born(int year, int month, int day) {
        BirthdayData data = new BirthdayData();
        data.setBirthDate(LocalDate.of(year, month, day));
        return data;
    }

    @Test
    void newDataIsNotSet() {
        BirthdayData data = new BirthdayData();
        assertThat(data.isBirthdaySet()).isFalse();
        assertThat(data.getFormattedDisplay()).isEqualTo("Not Set");
        assertThat(data.getLastAnnouncedYear()).isZero();
    }

    @Test
    void pre1970BirthdaysCountAsSet() {
        assertThat(born(1955, 6, 1).isBirthdaySet()).isTrue();
    }

    @Test
    void birthdayInMovesLeapDayToFebruary28thInNonLeapYears() {
        BirthdayData leapling = born(2008, 2, 29);
        assertThat(leapling.getBirthdayIn(2027)).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(leapling.getBirthdayIn(2028)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(born(2007, 10, 4).getBirthdayIn(2026)).isEqualTo(LocalDate.of(2026, 10, 4));
    }

    @Test
    void birthdayInRequiresABirthday() {
        assertThatThrownBy(() -> new BirthdayData().getBirthdayIn(2026)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ageGoesUpOnTheBirthday() {
        BirthdayData data = born(2007, 10, 4);
        assertThat(data.getAgeOn(LocalDate.of(2026, 10, 3))).isEqualTo(18);
        assertThat(data.getAgeOn(LocalDate.of(2026, 10, 4))).isEqualTo(19);
        assertThat(data.getAgeOn(LocalDate.of(2026, 10, 5))).isEqualTo(19);
    }

    @Test
    void leaplingAgeGoesUpOnFebruary28thInNonLeapYears() {
        BirthdayData leapling = born(2008, 2, 29);
        assertThat(leapling.getAgeOn(LocalDate.of(2027, 2, 27))).isEqualTo(18);
        assertThat(leapling.getAgeOn(LocalDate.of(2027, 2, 28))).isEqualTo(19);
        assertThat(leapling.getAgeOn(LocalDate.of(2028, 2, 28))).isEqualTo(19);
        assertThat(leapling.getAgeOn(LocalDate.of(2028, 2, 29))).isEqualTo(20);
    }

    @Test
    void formattedDisplayShowsDateAndZone() {
        BirthdayData data = born(2007, 10, 4);
        data.setTimezone("Asia/Tokyo");
        assertThat(data.getFormattedDisplay()).isEqualTo("04 October 2007 (Asia/Tokyo)");
    }

    @Test
    void invalidOrMissingTimezoneFallsBackToUtc() {
        BirthdayData data = born(2007, 10, 4);
        assertThat(data.getTimeZoneId()).isEqualTo(ZoneId.of("UTC"));
        data.setTimezone("Mars/Olympus");
        assertThat(data.getTimeZoneId()).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void clearBirthdayKeepsTheLastAnnouncedYear() {
        BirthdayData data = born(2007, 10, 4);
        data.setShouldAnnounceAge(true);
        data.setTimezone("Asia/Tokyo");
        data.setLastAnnouncedYear(2026);

        data.clearBirthday();

        assertThat(data.isBirthdaySet()).isFalse();
        assertThat(data.isShouldAnnounceAge()).isFalse();
        assertThat(data.getTimezone()).isNull();
        assertThat(data.getLastAnnouncedYear()).isEqualTo(2026);
    }

    @Test
    void setBirthdayRejectsFutureDates() {
        DiscordUser user = new DiscordUser("1");
        LocalDate today = LocalDate.of(2026, 10, 11);

        assertThatThrownBy(() -> user.setBirthday(today.plusDays(1), today))
            .isInstanceOf(DateTimeException.class).hasMessage("Birthday cannot be in the future");
        user.setBirthday(today, today);
        assertThat(user.getBirthdayData().getBirthDate()).isEqualTo(today);
    }

    @Test
    void setBirthdayRejectsYearsBefore1900() {
        DiscordUser user = new DiscordUser("1");
        LocalDate today = LocalDate.of(2026, 10, 11);

        assertThatThrownBy(() -> user.setBirthday(LocalDate.of(1899, 12, 31), today))
            .isInstanceOf(DateTimeException.class).hasMessage("Year cannot be before 1900");
        user.setBirthday(LocalDate.of(1900, 1, 1), today);
        assertThat(user.getBirthdayData().getBirthDate()).isEqualTo(LocalDate.of(1900, 1, 1));
    }

    @Test
    void setBirthdayCreatesMissingBirthdayData() {
        DiscordUser user = new DiscordUser("1");
        user.setBirthdayData(null);

        user.setBirthday(LocalDate.of(2007, 10, 4), LocalDate.of(2026, 10, 11));

        assertThat(user.getBirthdayData().getBirthDate()).isEqualTo(LocalDate.of(2007, 10, 4));
    }
}
