package net.hypixel.nerdbot.marmalade.storage.database.model.user.stats;

import net.hypixel.nerdbot.marmalade.json.DataSerialization;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRestrictedWarningTimestampsTest {

    @Test
    void warningIsRecordedOnlyForTheGivenGroup() {
        NominationInfo info = new NominationInfo();

        info.recordRoleRestrictedInactivityWarning("alpha-testers");

        assertThat(info.getLastRoleRestrictedInactivityWarningTimestamp("alpha-testers")).isPresent();
        assertThat(info.getLastRoleRestrictedInactivityWarningTimestamp("wiki-team")).isEmpty();
    }

    @Test
    void recordingAlsoUpdatesTheOverallTotalAndTimestamp() {
        NominationInfo info = new NominationInfo();

        info.recordRoleRestrictedInactivityWarning("alpha-testers");
        info.recordRoleRestrictedInactivityWarning("wiki-team");

        assertThat(info.getTotalRoleRestrictedInactivityWarnings()).isEqualTo(2);
        assertThat(info.getLastRoleRestrictedInactivityWarningTimestamp())
            .isEqualTo(info.getLastRoleRestrictedInactivityWarningTimestamp("wiki-team"));
    }

    @Test
    void perGroupTimestampsRoundTripThroughGson() {
        NominationInfo info = new NominationInfo();
        info.recordRoleRestrictedInactivityWarning("alpha-testers");
        long recorded = info.getLastRoleRestrictedInactivityWarningTimestamp("alpha-testers").orElseThrow();

        NominationInfo reloaded = DataSerialization.GSON.fromJson(DataSerialization.GSON.toJson(info), NominationInfo.class);

        assertThat(reloaded.getLastRoleRestrictedInactivityWarningTimestamp("alpha-testers")).contains(recorded);
    }

    @Test
    void legacyDocumentWithoutPerGroupTimestampsHasNoneRecorded() {
        NominationInfo reloaded = DataSerialization.GSON.fromJson(
            "{\"totalRoleRestrictedInactivityWarnings\":3,\"lastRoleRestrictedInactivityWarningTimestamp\":1700000000000}",
            NominationInfo.class
        );

        assertThat(reloaded.getLastRoleRestrictedInactivityWarningTimestamp("alpha-testers")).isEmpty();
        assertThat(reloaded.getTotalRoleRestrictedInactivityWarnings()).isEqualTo(3);
    }

    @Test
    void explicitNullMapIsTreatedAsEmptyAndCanStillRecord() {
        NominationInfo reloaded = DataSerialization.GSON.fromJson(
            "{\"lastRoleRestrictedInactivityWarningTimestamps\":null}",
            NominationInfo.class
        );

        assertThat(reloaded.getLastRoleRestrictedInactivityWarningTimestamp("alpha-testers")).isEmpty();

        reloaded.recordRoleRestrictedInactivityWarning("alpha-testers");

        assertThat(reloaded.getLastRoleRestrictedInactivityWarningTimestamp("alpha-testers")).isPresent();
    }
}
