/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.dto.usecase;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

import static ru.beeline.projectbackend.utils.Constant.CONNECTION_OPERATION_ID_MUST_BE_INTEGER;

public class ConnectionOperationIdDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.isExpectedNumberIntToken()) {
            try {
                long value = parser.getLongValue();
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    return (int) value;
                }
            } catch (JacksonException ignored) {
            }
        }
        throw new InvalidFormatException(parser, CONNECTION_OPERATION_ID_MUST_BE_INTEGER,
                parser.getText(), Integer.class);
    }
}
