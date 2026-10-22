package com.example.sport_be.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyUtilsTest {

    @Test
    void formatVnd_formatsKnownAmountWithThousandsSeparators() {
        assertThat(CurrencyUtils.formatVnd(new BigDecimal("1250000"))).isEqualTo("1.250.000 \u20AB");
    }

    @Test
    void formatVnd_formatsZero() {
        assertThat(CurrencyUtils.formatVnd(BigDecimal.ZERO)).isEqualTo("0 \u20AB");
    }

    @Test
    void formatVnd_roundsDecimalPlacesHalfUp() {
        assertThat(CurrencyUtils.formatVnd(new BigDecimal("1250000.5"))).isEqualTo("1.250.001 \u20AB");
        assertThat(CurrencyUtils.formatVnd(new BigDecimal("1250000.4"))).isEqualTo("1.250.000 \u20AB");
    }

    @Test
    void formatVnd_throwsOnNullAmount() {
        assertThatThrownBy(() -> CurrencyUtils.formatVnd(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
