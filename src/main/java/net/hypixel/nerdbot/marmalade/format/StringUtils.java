package net.hypixel.nerdbot.marmalade.format;

import java.text.DecimalFormat;

public final class StringUtils {

    public static final DecimalFormat COMMA_SEPARATED_FORMAT = new DecimalFormat("#,###");

    private StringUtils() {
    }

    public static String formatSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    public static String toOneLine(String input) {
        if (input == null) {
            return null;
        }
        String s = input.replace('\n', ' ').replace('\r', ' ');
        return s.replaceAll("\\s{2,}", " ").trim();
    }
}
