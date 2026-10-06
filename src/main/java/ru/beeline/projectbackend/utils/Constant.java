/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.utils;

public class Constant {
    public static final String USER_ID_HEADER = "user-id";
    public static final String ID_MUST_BE_POSITIVE = "id должен быть больше 0";
    public static final String MISSING_REQUEST_BODY = "Отсутствует тело запроса";
    public static final String EMPTY_OPERATION_IDS = "Список идентификаторов методов не может быть пустым";
    public static final String BLANK_OPERATION_ID = "Идентификатор метода не может быть пустым";
    public static final String EMPTY_BI_STEP_CODES = "Список кодов bi-step не может быть пустым";
    public static final String BLANK_BI_STEP_CODE = "Код bi-step не может быть пустым";
    public static final String CONNECTION_OPERATION_ID_MUST_BE_INTEGER =
            "Поле connectionOperationId должно быть целым числом";
}
