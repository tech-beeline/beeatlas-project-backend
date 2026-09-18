/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.utils;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;


public final class InterfaceCodeGenerator {

    private InterfaceCodeGenerator() {
    }

    public static String crc32(String... attributes) {
        StringBuilder source = new StringBuilder();
        for (String attribute : attributes) {
            if (attribute != null && !attribute.isEmpty()) {
                source.append(attribute);
            }
        }
        CRC32 crc32 = new CRC32();
        crc32.update(source.toString().getBytes(StandardCharsets.UTF_8));
        return String.format("%08x", crc32.getValue());
    }
}
