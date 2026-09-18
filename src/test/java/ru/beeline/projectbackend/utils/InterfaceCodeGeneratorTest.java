/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InterfaceCodeGeneratorTest {

    @Test
    @DisplayName("CRC32 от конкатенации атрибутов: 8 символов hex в нижнем регистре")
    void buildsLowercaseHexOfEightChars() {
        String code = InterfaceCodeGenerator.crc32("getOrder", "REST", null, "TC-1", null);

        assertThat(code).isEqualTo("f09d2f37");
        assertThat(code).hasSize(8).matches("[0-9a-f]{8}");
    }

    @Test
    @DisplayName("Пустые и не заданные атрибуты в конкатенацию не входят")
    void skipsNullAndEmptyAttributes() {
        assertThat(InterfaceCodeGenerator.crc32("listOrders", "REST", null, null, null))
                .isEqualTo("cd4c9ad0")
                .isEqualTo(InterfaceCodeGenerator.crc32("listOrders", "", "REST", null, ""));
    }

    @Test
    @DisplayName("Порядок атрибутов значим: перестановка даёт другой код")
    void isOrderSensitive() {
        assertThat(InterfaceCodeGenerator.crc32("getOrder", "REST"))
                .isNotEqualTo(InterfaceCodeGenerator.crc32("REST", "getOrder"));
    }
}
