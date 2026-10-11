package net.hypixel.nerdbot.marmalade.storage.database.model.user.stats;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LastActivityTest {

    private static final String GROUP = "staff";

    private static String monthsAgo(int months) {
        return YearMonth.now().minusMonths(months).format(DateTimeFormatter.ofPattern("MM-yyyy"));
    }

    private static ChannelActivityEntry entry(String channelId, Map<String, Integer> monthly) {
        int total = monthly.values().stream().mapToInt(Integer::intValue).sum();
        return new ChannelActivityEntry(channelId, "#" + channelId, total, 0L, new HashMap<>(monthly));
    }

    private static LastActivity withChannelHistory(ChannelActivityEntry... entries) {
        LastActivity activity = new LastActivity();
        activity.setChannelActivityHistory(new ArrayList<>(List.of(entries)));
        return activity;
    }

    private static LastActivity withRoleRestrictedHistory(ChannelActivityEntry... entries) {
        LastActivity activity = new LastActivity();
        activity.getRoleRestrictedChannelActivityHistory().put(GROUP, new ArrayList<>(List.of(entries)));
        return activity;
    }

    @Test
    void channelHistoryIsConsistentAcrossDifferentWindows() {
        LastActivity activity = withChannelHistory(entry("general", Map.of(monthsAgo(0), 5, monthsAgo(3), 7)));

        assertThat(activity.getChannelActivityHistory(1)).singleElement()
            .extracting(ChannelActivityEntry::getMessageCount).isEqualTo(5);
        assertThat(activity.getChannelActivityHistory(365)).singleElement()
            .extracting(ChannelActivityEntry::getMessageCount).isEqualTo(12);
        assertThat(activity.getChannelActivityHistory(1)).singleElement()
            .extracting(ChannelActivityEntry::getMessageCount).isEqualTo(5);
    }

    @Test
    void channelHistoryLeavesStoredEntryUntouched() {
        ChannelActivityEntry stored = entry("general", Map.of(monthsAgo(0), 5, monthsAgo(3), 7, monthsAgo(24), 9));
        Map<String, Integer> originalMonthly = new HashMap<>(stored.getMonthlyMessageCount());
        LastActivity activity = withChannelHistory(stored);

        List<ChannelActivityEntry> result = activity.getChannelActivityHistory(1);

        assertThat(stored.getMonthlyMessageCount()).isEqualTo(originalMonthly);
        assertThat(stored.getMessageCount()).isEqualTo(21);
        assertThat(activity.getChannelActivityHistory()).containsExactly(stored);
        assertThat(result).singleElement().satisfies(filtered -> {
            assertThat(filtered).isNotSameAs(stored);
            assertThat(filtered.getMonthlyMessageCount()).isNotSameAs(stored.getMonthlyMessageCount());
        });
    }

    @Test
    void channelHistoryResultCanBeModifiedWithoutAffectingStoredEntry() {
        ChannelActivityEntry stored = entry("general", Map.of(monthsAgo(0), 5));
        LastActivity activity = withChannelHistory(stored);

        activity.getChannelActivityHistory(30).getFirst().getMonthlyMessageCount().clear();

        assertThat(stored.getMonthlyMessageCount()).containsExactly(Map.entry(monthsAgo(0), 5));
    }

    @Test
    void channelHistoryExcludesMonthsOutsideWindow() {
        LastActivity activity = withChannelHistory(
            entry("general", Map.of(monthsAgo(0), 4, monthsAgo(24), 10, monthsAgo(-1), 3))
        );

        assertThat(activity.getChannelActivityHistory(1)).singleElement().satisfies(filtered -> {
            assertThat(filtered.getMessageCount()).isEqualTo(4);
            assertThat(filtered.getMonthlyMessageCount()).containsOnlyKeys(monthsAgo(0));
        });
    }

    @Test
    void channelHistoryIncludesEveryMonthTouchedByWindow() {
        LastActivity activity = withChannelHistory(
            entry("general", Map.of(monthsAgo(0), 1, monthsAgo(1), 2, monthsAgo(2), 4, monthsAgo(13), 8))
        );

        assertThat(activity.getChannelActivityHistory(100)).singleElement().satisfies(filtered -> {
            assertThat(filtered.getMessageCount()).isEqualTo(7);
            assertThat(filtered.getMonthlyMessageCount()).containsOnlyKeys(monthsAgo(0), monthsAgo(1), monthsAgo(2));
        });
    }

    @Test
    void channelHistoryCarriesEntryMetadataOver() {
        ChannelActivityEntry stored = new ChannelActivityEntry("123", "#general", 99, 1_700_000_000_000L,
            new HashMap<>(Map.of(monthsAgo(0), 3)));
        LastActivity activity = withChannelHistory(stored);

        assertThat(activity.getChannelActivityHistory(30)).singleElement().satisfies(filtered -> {
            assertThat(filtered.getChannelId()).isEqualTo("123");
            assertThat(filtered.getLastKnownDisplayName()).isEqualTo("#general");
            assertThat(filtered.getLastMessageTimestamp()).isEqualTo(1_700_000_000_000L);
            assertThat(filtered.getMessageCount()).isEqualTo(3);
        });
    }

    @Test
    void channelHistoryOmitsEntriesWithNoMessagesInWindow() {
        ChannelActivityEntry stale = entry("stale", Map.of(monthsAgo(24), 10));
        ChannelActivityEntry zeroed = entry("zeroed", Map.of(monthsAgo(0), 0));
        ChannelActivityEntry active = entry("active", Map.of(monthsAgo(0), 2));
        LastActivity activity = withChannelHistory(stale, zeroed, active);

        assertThat(activity.getChannelActivityHistory(30))
            .extracting(ChannelActivityEntry::getChannelId)
            .containsExactly("active");
        assertThat(activity.getChannelActivityHistory()).containsExactly(stale, zeroed, active);
        assertThat(stale.getMonthlyMessageCount()).containsExactly(Map.entry(monthsAgo(24), 10));
    }

    @Test
    void channelHistoryOmitsEntriesWithoutMonthlyCounts() {
        ChannelActivityEntry legacy = new ChannelActivityEntry("legacy", "#legacy", 50, 0L, null);
        ChannelActivityEntry empty = entry("empty", Map.of());
        ChannelActivityEntry active = entry("active", Map.of(monthsAgo(0), 2));
        LastActivity activity = withChannelHistory(legacy, empty, active);

        assertThat(activity.getChannelActivityHistory(30))
            .extracting(ChannelActivityEntry::getChannelId)
            .containsExactly("active");
    }

    @Test
    void channelHistoryIsEmptyWhenNothingRecorded() {
        assertThat(new LastActivity().getChannelActivityHistory(30)).isEmpty();
    }

    @Test
    void totalMessageCountForWindowDoesNotPruneHistory() {
        LastActivity activity = withChannelHistory(
            entry("general", Map.of(monthsAgo(0), 5, monthsAgo(3), 7)),
            entry("off-topic", Map.of(monthsAgo(6), 11))
        );

        assertThat(activity.getTotalMessageCount(1)).isEqualTo(5);
        assertThat(activity.getTotalMessageCount(365)).isEqualTo(23);
        assertThat(activity.getTotalMessageCount(1)).isEqualTo(5);
    }

    @Test
    void roleRestrictedHistoryIsConsistentAcrossDifferentWindows() {
        ChannelActivityEntry stored = entry("staff-chat", Map.of(monthsAgo(0), 5, monthsAgo(3), 7));
        Map<String, Integer> originalMonthly = new HashMap<>(stored.getMonthlyMessageCount());
        LastActivity activity = withRoleRestrictedHistory(stored);

        assertThat(activity.getRoleRestrictedChannelMessageCount(GROUP, 1)).isEqualTo(5);
        assertThat(activity.getRoleRestrictedChannelMessageCount(GROUP, 365)).isEqualTo(12);
        assertThat(stored.getMonthlyMessageCount()).isEqualTo(originalMonthly);
    }

    @Test
    void roleRestrictedHistoryExcludesMonthsOutsideWindowAndOmitsEmptyEntries() {
        LastActivity activity = withRoleRestrictedHistory(
            entry("staff-chat", Map.of(monthsAgo(0), 4, monthsAgo(24), 10)),
            entry("staff-archive", Map.of(monthsAgo(24), 6))
        );

        assertThat(activity.getRoleRestrictedChannelActivityHistory(GROUP, 1)).singleElement().satisfies(filtered -> {
            assertThat(filtered.getChannelId()).isEqualTo("staff-chat");
            assertThat(filtered.getMessageCount()).isEqualTo(4);
            assertThat(filtered.getMonthlyMessageCount()).containsOnlyKeys(monthsAgo(0));
        });
    }

    @Test
    void roleRestrictedHistoryIsEmptyForUnknownGroup() {
        LastActivity activity = new LastActivity();

        assertThat(activity.getRoleRestrictedChannelActivityHistory("missing", 30)).isEmpty();
        assertThat(activity.getRoleRestrictedChannelActivityHistory()).doesNotContainKey("missing");
    }
}