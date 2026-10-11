package net.hypixel.nerdbot.marmalade.storage.badge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TieredBadgeTest {

    private static TieredBadge bugHunter() {
        return new TieredBadge("bug_hunter", "Bug Hunter", List.of(
            new TieredBadge.Tier("Tier 1", "🥇", 1),
            new TieredBadge.Tier("Tier 2", "", 2),
            new TieredBadge.Tier("Tier 3", null, 3)));
    }

    @Test
    void formattedTierNameIncludesBadgeNameAndTierEmoji() {
        assertThat(bugHunter().getFormattedName(1)).isEqualTo("🥇 Bug Hunter (Tier 1)");
    }

    @Test
    void formattedTierNameOmitsBlankEmoji() {
        assertThat(bugHunter().getFormattedName(2)).isEqualTo("Bug Hunter (Tier 2)");
    }

    @Test
    void formattedTierNameOmitsNullEmoji() {
        assertThat(bugHunter().getFormattedName(3)).isEqualTo("Bug Hunter (Tier 3)");
    }

    @Test
    void formattedTierNameFallsBackToTierNumberWhenTierIsNotConfigured() {
        assertThat(bugHunter().getFormattedName(7)).isEqualTo("Bug Hunter (Tier 7)");
    }

    @Test
    void formattedTierNameUsesConfiguredTierNameVerbatim() {
        TieredBadge badge = new TieredBadge("event", "Event", List.of(new TieredBadge.Tier("Champion", "🏆", 1)));

        assertThat(badge.getFormattedName(1)).isEqualTo("🏆 Event (Champion)");
    }

    @Test
    void getTierReturnsNullForUnconfiguredTier() {
        assertThat(bugHunter().getTier(7)).isNull();
    }
}