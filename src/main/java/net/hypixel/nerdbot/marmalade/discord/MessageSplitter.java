package net.hypixel.nerdbot.marmalade.discord;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for splitting long strings into chunks that fit within Discord's message length limits.
 * Provides both a plain split and a code-block-aware split that avoids breaking fenced code blocks
 * across message boundaries.
 */
@UtilityClass
public class MessageSplitter {

    /**
     * Splits {@code content} into chunks using {@link Message#MAX_CONTENT_LENGTH} as the limit.
     *
     * @param content the text to split; returns an empty list if {@code null} or empty
     * @return an ordered list of chunks, each no longer than {@link Message#MAX_CONTENT_LENGTH}
     */
    public static List<String> split(String content) {
        return split(content, Message.MAX_CONTENT_LENGTH);
    }

    /**
     * Splits {@code content} into chunks no longer than {@code maxLength} characters each.
     * Prefers splitting on newline boundaries, then spaces, and falls back to a hard split when
     * no natural break point is found in the trailing 20% of the window.
     *
     * @param content   the text to split; returns an empty list if {@code null} or empty
     * @param maxLength the maximum character length of each chunk
     * @return an ordered list of chunks, each no longer than {@code maxLength}
     */
    public static List<String> split(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        if (content.length() <= maxLength) {
            return List.of(content);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + maxLength, content.length());

            if (end == content.length()) {
                chunks.add(content.substring(start));
                break;
            }

            int searchStart = start + (int) (maxLength * 0.8);
            // chunkEnd is the exclusive end index for the chunk content
            // nextStart is where the next chunk begins (may skip separator characters)
            int chunkEnd = -1;
            int nextStart = -1;

            // Search backwards from end for newline -- split before it, skip it
            for (int i = end - 1; i >= searchStart; i--) {
                if (content.charAt(i) == '\n') {
                    chunkEnd = i;
                    nextStart = i + 1;
                    break;
                }
            }

            // Search backwards from end for space if no newline found -- split before it, skip it
            if (chunkEnd == -1) {
                for (int i = end - 1; i >= searchStart; i--) {
                    if (content.charAt(i) == ' ') {
                        chunkEnd = i;
                        nextStart = i + 1;
                        break;
                    }
                }
            }

            // Hard split if no break point found
            if (chunkEnd == -1) {
                chunkEnd = end;
                nextStart = end;
            }

            chunks.add(content.substring(start, chunkEnd));
            start = nextStart;
        }

        return chunks;
    }

    /**
     * Splits {@code content} into chunks using {@link Message#MAX_CONTENT_LENGTH} as the limit,
     * closing any open fenced code block at the end of each chunk and reopening it at the start of
     * the next.
     *
     * @param content the text to split; returns an empty list if {@code null} or empty
     * @return an ordered list of chunks with balanced code-block fences
     */
    public static List<String> splitPreservingCodeBlocks(String content) {
        return splitPreservingCodeBlocks(content, Message.MAX_CONTENT_LENGTH);
    }

    /**
     * Splits {@code content} into chunks no longer than {@code maxLength} characters each,
     * closing any open fenced code block at the end of each chunk and reopening it (preserving the
     * language identifier) at the start of the next.
     *
     * @param content   the text to split; returns an empty list if {@code null} or empty
     * @param maxLength the maximum character length of each chunk
     * @return an ordered list of chunks with balanced code-block fences
     */
    public static List<String> splitPreservingCodeBlocks(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        if (content.length() <= maxLength) {
            return List.of(content);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        boolean inCodeBlock = false;
        String codeBlockLanguage = "";

        while (start < content.length()) {
            int end = Math.min(start + maxLength, content.length());

            if (end == content.length()) {
                chunks.add(content.substring(start));
                break;
            }

            // Scan the current window to determine code block state at the split boundary
            boolean currentlyInCodeBlock = inCodeBlock;
            String currentLanguage = codeBlockLanguage;
            int tickPos = start;
            while (tickPos < end) {
                int idx = content.indexOf("```", tickPos);
                if (idx == -1 || idx >= end) {
                    break;
                }
                if (!currentlyInCodeBlock) {
                    // Opening fence -- extract language identifier on the same line
                    int langStart = idx + 3;
                    int langEnd = content.indexOf('\n', langStart);
                    if (langEnd == -1 || langEnd >= end) {
                        langEnd = end;
                    }
                    currentLanguage = content.substring(langStart, langEnd).trim();
                    currentlyInCodeBlock = true;
                } else {
                    currentLanguage = "";
                    currentlyInCodeBlock = false;
                }
                tickPos = idx + 3;
            }

            // Find the best split position, preferring natural line boundaries
            int searchStart = start + (int) (maxLength * 0.8);
            int chunkEnd = -1;
            int nextStart = -1;

            for (int i = end - 1; i >= searchStart; i--) {
                if (content.charAt(i) == '\n') {
                    chunkEnd = i;
                    nextStart = i + 1;
                    break;
                }
            }

            if (chunkEnd == -1) {
                for (int i = end - 1; i >= searchStart; i--) {
                    if (content.charAt(i) == ' ') {
                        chunkEnd = i;
                        nextStart = i + 1;
                        break;
                    }
                }
            }

            if (chunkEnd == -1) {
                chunkEnd = end;
                nextStart = end;
            }

            String chunk = content.substring(start, chunkEnd);

            if (currentlyInCodeBlock) {
                chunk = chunk + "```";
            }

            chunks.add(chunk);

            inCodeBlock = currentlyInCodeBlock;
            codeBlockLanguage = currentLanguage;
            start = nextStart;

            // Prepend the reopening fence to the remaining content so it becomes
            // the natural start of the next chunk
            if (start < content.length() && currentlyInCodeBlock) {
                String prefix = "```" + currentLanguage + "\n";
                content = content.substring(0, start) + prefix + content.substring(start);
            }
        }

        return chunks;
    }
}