package com.example.sport_be.utils;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugUtilsTest {

    private static final String VIETNAMESE_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzAĂÂBCDĐEÊGHIKLMNOÔƠPQRSTUƯVXY"
                    + "áàảãạăắằẳẵặâấầẩẫậđéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵ"
                    + " 0123456789";

    @Provide
    Arbitrary<String> vietnameseLikeStrings() {
        return Arbitraries.strings()
                .withChars(VIETNAMESE_ALPHABET.toCharArray())
                .ofMinLength(1)
                .ofMaxLength(40)
                .filter(s -> !s.isBlank());
    }

    @Property
    void outputIsNeverNullOrBlankForNonBlankInput(@ForAll("vietnameseLikeStrings") String input) {
        String slug = SlugUtils.toSlug(input);
        assertThat(slug).isNotNull();
    }

    @Property
    void outputOnlyContainsLowercaseLettersDigitsAndHyphens(@ForAll("vietnameseLikeStrings") String input) {
        String slug = SlugUtils.toSlug(input);
        assertThat(slug).matches("^[a-z0-9-]*$");
    }

    @Property
    void outputNeverStartsOrEndsWithHyphen(@ForAll("vietnameseLikeStrings") String input) {
        String slug = SlugUtils.toSlug(input);
        if (!slug.isEmpty()) {
            assertThat(slug).doesNotStartWith("-");
            assertThat(slug).doesNotEndWith("-");
        }
    }

    @Property
    void outputNeverContainsConsecutiveHyphens(@ForAll("vietnameseLikeStrings") String input) {
        String slug = SlugUtils.toSlug(input);
        assertThat(slug).doesNotContain("--");
    }

    @Test
    void toSlug_convertsKnownVietnameseStrings() {
        assertThat(SlugUtils.toSlug("Áo thể thao nam")).isEqualTo("ao-the-thao-nam");
        assertThat(SlugUtils.toSlug("Giày đá bóng")).isEqualTo("giay-da-bong");
        assertThat(SlugUtils.toSlug("Vợt cầu lông Yonex")).isEqualTo("vot-cau-long-yonex");
        assertThat(SlugUtils.toSlug("Đồ Pickleball")).isEqualTo("do-pickleball");
    }

    @Test
    void toSlug_collapsesMultipleSeparatorsIntoSingleHyphen() {
        assertThat(SlugUtils.toSlug("Áo   thể --- thao")).isEqualTo("ao-the-thao");
    }

    @Test
    void toSlug_trimsLeadingAndTrailingSeparators() {
        assertThat(SlugUtils.toSlug("  Áo thể thao  ")).isEqualTo("ao-the-thao");
    }

    @Test
    void toSlug_throwsOnNullInput() {
        assertThatThrownBy(() -> SlugUtils.toSlug(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toSlug_throwsOnBlankInput() {
        assertThatThrownBy(() -> SlugUtils.toSlug("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
