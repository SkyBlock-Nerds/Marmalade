package net.hypixel.nerdbot.marmalade.discord;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;
import java.util.Map;

/**
 * Factory for creating pre-styled Discord {@link EmbedBuilder} instances and for safely adding
 * fields that respect Discord's character limits.
 */
@UtilityClass
public class EmbedFactory {

    /** Green accent color used for success-state embeds. */
    public static final Color COLOR_SUCCESS = new Color(87, 242, 135);

    /** Red accent color used for error-state embeds. */
    public static final Color COLOR_ERROR = new Color(237, 66, 69);

    /** Blurple accent color used for informational embeds. */
    public static final Color COLOR_INFO = new Color(88, 101, 242);

    /** Yellow accent color used for warning-state embeds. */
    public static final Color COLOR_WARNING = new Color(254, 231, 92);

    /**
     * Creates a success-styled embed with a green accent color and a current timestamp.
     *
     * @param title       the embed title
     * @param description the embed description
     * @return a configured {@link EmbedBuilder}
     */
    public static EmbedBuilder success(String title, String description) {
        return create(title, description, COLOR_SUCCESS);
    }

    /**
     * Creates an error-styled embed with a red accent color and a current timestamp.
     *
     * @param title       the embed title
     * @param description the embed description
     * @return a configured {@link EmbedBuilder}
     */
    public static EmbedBuilder error(String title, String description) {
        return create(title, description, COLOR_ERROR);
    }

    /**
     * Creates an info-styled embed with a blurple accent color and a current timestamp.
     *
     * @param title       the embed title
     * @param description the embed description
     * @return a configured {@link EmbedBuilder}
     */
    public static EmbedBuilder info(String title, String description) {
        return create(title, description, COLOR_INFO);
    }

    /**
     * Creates a warning-styled embed with a yellow accent color and a current timestamp.
     *
     * @param title       the embed title
     * @param description the embed description
     * @return a configured {@link EmbedBuilder}
     */
    public static EmbedBuilder warning(String title, String description) {
        return create(title, description, COLOR_WARNING);
    }

    /**
     * Creates an embed with the given title, description, color, and a current timestamp.
     *
     * @param title       the embed title
     * @param description the embed description
     * @param color       the sidebar accent color
     * @return a configured {@link EmbedBuilder}
     */
    public static EmbedBuilder create(String title, String description, Color color) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(color)
            .setTimestamp(Instant.now());
    }

    /**
     * Adds all entries from {@code fields} to the embed as individual fields, each truncated to
     * {@link MessageEmbed#VALUE_MAX_LENGTH} characters if necessary.
     *
     * @param builder the embed builder to modify
     * @param fields  map of field names to field values; iteration order determines insertion order
     * @param inline  whether each field should be displayed inline
     */
    public static void addFields(EmbedBuilder builder, Map<String, String> fields, boolean inline) {
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            addField(builder, entry.getKey(), entry.getValue(), inline);
        }
    }

    /**
     * Adds a single field to the embed, converting {@code value} via {@link Object#toString()} and
     * truncating to {@link MessageEmbed#VALUE_MAX_LENGTH} characters if necessary. A {@code null}
     * value is rendered as the literal string {@code "null"}.
     *
     * @param builder the embed builder to modify
     * @param name    the field name
     * @param value   the field value; may be {@code null}
     * @param inline  whether the field should be displayed inline
     */
    public static void addField(EmbedBuilder builder, String name, Object value, boolean inline) {
        String text = value == null ? "null" : value.toString();

        if (text.length() > MessageEmbed.VALUE_MAX_LENGTH) {
            text = text.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 3) + "...";
        }

        builder.addField(name, text, inline);
    }
}