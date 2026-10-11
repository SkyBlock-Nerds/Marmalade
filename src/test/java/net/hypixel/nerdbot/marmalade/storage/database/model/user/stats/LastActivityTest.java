package net.hypixel.nerdbot.marmalade.storage.database.model.user.stats;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LastActivityTest {

    private static final String GROUP = "staff";
    private static final LocalDate OCTOBER_15 = LocalDate.of(2026, 10, 15);
    private static final Clock CLOCK = Clock.fixed(at(OCTOBER_15), ZoneId.systemDefault());

    private static Instant at(LocalDate day) {
        return day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant();
    }

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
    void messagesSinceDailyCountsBeganAreCountedExactly() {
        LastActivity activity = new LastActivity();
        activity.addChannelHistory("general", "general", at(OCTOBER_15.minusDays(40)).toEpochMilli());
        activity.addChannelHistory("general", "general", at(OCTOBER_15.minusDays(29)).toEpochMilli());
        activity.addChannelHistory("off-topic", "off-topic", 2, at(OCTOBER_15).toEpochMilli());

        assertThat(activity.getTotalMessageCount(30, CLOCK)).isEqualTo(3);
        assertThat(activity.getTotalMessageCount(1, CLOCK)).isEqualTo(2);
        assertThat(activity.getTotalMessageCount(41, CLOCK)).isEqualTo(4);
    }

    @Test
    void thirtyDaysNoLongerReachesBackToTheStartOfLastMonth() {
        LastActivity activity = new LastActivity();
        activity.addChannelHistory("general", "general", at(LocalDate.of(2026, 9, 2)).toEpochMilli());

        assertThat(activity.getTotalMessageCount(30, CLOCK)).isZero();
    }

    @Test
    void recordsFromBeforeDailyCountsSpreadEachMonthOverItsDays() {
        LastActivity activity = withChannelHistory(entry("general", Map.of("09-2026", 30, "10-2026", 31)));

        // 16-30 September is 15 of its 30 days, 1-15 October is 15 of 31
        assertThat(activity.getTotalMessageCount(30, CLOCK)).isEqualTo(30);
        assertThat(activity.getTotalMessageCount(15, CLOCK)).isEqualTo(15);
    }

    @Test
    void theMonthDailyCountsBeganInOnlyEstimatesTheMessagesBeforeThey() {
        LastActivity activity = withChannelHistory(entry("general", Map.of("10-2026", 6)));
        activity.addChannelHistory("general", "general", 4, at(LocalDate.of(2026, 10, 11)).toEpochMilli());

        // 6 messages came on 1-10 October, before daily counts began on the 11th; 4 came after
        assertThat(activity.getTotalMessageCount(30, CLOCK)).isEqualTo(10);
        assertThat(activity.getTotalMessageCount(10, CLOCK)).isEqualTo(7);
    }

    @Test
    void anEmptyWindowCountsNothing() {
        LastActivity activity = withChannelHistory(entry("general", Map.of("10-2026", 6)));

        assertThat(activity.getTotalMessageCount(0, CLOCK)).isZero();
    }

    @Test
    void countingDoesNotChangeTheStoredHistory() {
        ChannelActivityEntry stored = entry("general", Map.of("10-2026", 5, "07-2026", 7));
        LastActivity activity = withChannelHistory(stored);

        activity.getTotalMessageCount(365, CLOCK);

        assertThat(stored.getMonthlyMessageCount()).containsOnly(Map.entry("10-2026", 5), Map.entry("07-2026", 7));
        assertThat(activity.getDailyMessageCount()).isEmpty();
    }

    @Test
    void roleRestrictedMessagesAreCountedFromTheirTimestamps() {
        LastActivity activity = new LastActivity();
        long now = System.currentTimeMillis();
        activity.getRoleRestrictedChannelMessageHistory().put(GROUP, new ArrayList<>(List.of(
            now - Duration.ofHours(1).toMillis(), now - Duration.ofDays(20).toMillis(), now - Duration.ofDays(45).toMillis())));

        assertThat(activity.getRoleRestrictedChannelMessageCount(GROUP, 1)).isEqualTo(1);
        assertThat(activity.getRoleRestrictedChannelMessageCount(GROUP, 30)).isEqualTo(2);
        assertThat(activity.getRoleRestrictedChannelMessageCount("missing", 30)).isZero();
    }

    @Test
    void purgingClearsEveryHistoryNotJustTheFirst() {
        LastActivity activity = new LastActivity();
        long now = System.currentTimeMillis();
        long old = now - Duration.ofDays(100).toMillis();
        long recent = now - Duration.ofDays(1).toMillis();
        activity.getSuggestionCreationHistory().addAll(List.of(recent, old));
        activity.getSuggestionCommentHistory().add(old);
        activity.getAlphaSuggestionVoteHistoryMap().put("a", old);
        activity.getProjectSuggestionVoteHistoryMap().put("b", recent);
        activity.getRoleRestrictedChannelMessageHistory().put(GROUP, new ArrayList<>(List.of(old)));
        activity.getRoleRestrictedChannelCommentHistory().put(GROUP, new ArrayList<>(List.of(old)));
        activity.getRoleRestrictedChannelVoteHistory().put(GROUP, new HashMap<>(Map.of("c", old)));

        assertThat(activity.purgeOldHistory(90)).isTrue();

        assertThat(activity.getSuggestionCreationHistory()).containsExactly(recent);
        assertThat(activity.getSuggestionCommentHistory()).isEmpty();
        assertThat(activity.getAlphaSuggestionVoteHistoryMap()).isEmpty();
        assertThat(activity.getProjectSuggestionVoteHistoryMap()).containsOnlyKeys("b");
        assertThat(activity.getRoleRestrictedChannelMessageHistory().get(GROUP)).isEmpty();
        assertThat(activity.getRoleRestrictedChannelCommentHistory().get(GROUP)).isEmpty();
        assertThat(activity.getRoleRestrictedChannelVoteHistory().get(GROUP)).isEmpty();
    }

    @Test
    void purgingNothingSaysSo() {
        LastActivity activity = new LastActivity();
        activity.getSuggestionCreationHistory().add(System.currentTimeMillis());

        assertThat(activity.purgeOldHistory(90)).isFalse();
    }

    @Test
    void purgedDailyCountsFallBackToTheMonthlyTotals() {
        LastActivity activity = new LastActivity();
        LocalDate longAgo = LocalDate.now().minusDays(120);
        activity.addChannelHistory("general", "general", 3, at(longAgo).toEpochMilli());
        activity.addChannelHistory("general", "general", at(LocalDate.now()).toEpochMilli());

        activity.purgeOldHistory(90);

        assertThat(activity.getDailyMessageCount()).containsOnlyKeys(LocalDate.now().toString());
        assertThat(LocalDate.parse(activity.getDailyMessageCountSince())).isAfter(longAgo);
        assertThat(activity.getTotalMessageCount(365)).isEqualTo(4);
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