package net.hypixel.nerdbot.marmalade.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmbedFactoryTest {

    @Test
    void successEmbedHasGreenColor() {
        EmbedBuilder builder = EmbedFactory.success("Title", "Desc");
        assertThat(builder.build().getColorRaw()).isEqualTo(EmbedFactory.COLOR_SUCCESS.getRGB());
    }

    @Test
    void errorEmbedHasRedColor() {
        EmbedBuilder builder = EmbedFactory.error("Title", "Desc");
        assertThat(builder.build().getColorRaw()).isEqualTo(EmbedFactory.COLOR_ERROR.getRGB());
    }

    @Test
    void infoEmbedHasBlurpleColor() {
        EmbedBuilder builder = EmbedFactory.info("Title", "Desc");
        assertThat(builder.build().getColorRaw()).isEqualTo(EmbedFactory.COLOR_INFO.getRGB());
    }

    @Test
    void warningEmbedHasYellowColor() {
        EmbedBuilder builder = EmbedFactory.warning("Title", "Desc");
        assertThat(builder.build().getColorRaw()).isEqualTo(EmbedFactory.COLOR_WARNING.getRGB());
    }

    @Test
    void embedHasTitleAndDescription() {
        EmbedBuilder builder = EmbedFactory.info("My Title", "My Description");
        MessageEmbed embed = builder.build();

        assertThat(embed.getTitle()).isEqualTo("My Title");
        assertThat(embed.getDescription()).isEqualTo("My Description");
    }

    @Test
    void embedHasTimestamp() {
        EmbedBuilder builder = EmbedFactory.info("Title", "Desc");
        assertThat(builder.build().getTimestamp()).isNotNull();
    }

    @Test
    void returnsEmbedBuilderForChaining() {
        EmbedBuilder builder = EmbedFactory.success("Title", "Desc");
        builder.setFooter("Footer text");

        assertThat(builder.build().getFooter()).isNotNull();
        assertThat(builder.build().getFooter().getText()).isEqualTo("Footer text");
    }

    @Test
    void addFieldsTruncatesLongValues() {
        EmbedBuilder builder = EmbedFactory.info("Title", "Desc");
        String longValue = "x".repeat(2000);

        EmbedFactory.addField(builder, "Field", longValue, false);

        MessageEmbed.Field field = builder.build().getFields().get(0);
        assertThat(field.getValue()).hasSizeLessThanOrEqualTo(MessageEmbed.VALUE_MAX_LENGTH);
    }

    @Test
    void addFieldsFromMap() {
        EmbedBuilder builder = EmbedFactory.info("Title", "Desc");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Key1", "Value1");
        fields.put("Key2", "Value2");

        EmbedFactory.addFields(builder, fields, true);

        List<MessageEmbed.Field> builtFields = builder.build().getFields();
        assertThat(builtFields).hasSize(2);
        assertThat(builtFields.get(0).isInline()).isTrue();
        assertThat(builtFields.get(1).isInline()).isTrue();
    }

    @Test
    void customColorEmbed() {
        Color custom = new Color(100, 150, 200);
        EmbedBuilder builder = EmbedFactory.create("Title", "Desc", custom);
        assertThat(builder.build().getColorRaw()).isEqualTo(custom.getRGB());
    }
}