package com.axiom.hermes.common.validation;

import com.axiom.hermes.common.exceptions.HermesException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.axiom.hermes.common.exceptions.HermesException.BAD_REQUEST;

/**
 * Осуществляет проверки корректности данных
 */
public class Validator {

    // Регулярное выражение для валидации номера мобильного: +X XXX XXXXXXX, +X XXX XXX XX XX, +X(XXX)XXX-XX-XX
    public static final String MOBILE = "^\\+?(\\d)[ ]?[(]?(\\d{3})[)]?[ ]?(\\d{3})[ -]?(\\d{2})[ -]?(\\d{2})$";
    // Регулярное выражение для валидации ИНН/ИИН/БИН (9, 10, 12, 14 цифр)
    public static final String BIN = "^(\\d{9}|\\d{10}|\\d{12}|\\d{14})$";

    /**
     * Проверяет является ли мобильный номер валидным по шаблону регулярного выражения
     * @param mobile мобильный номер
     * @return валидный мобильный номер
     * @throws HermesException
     */
    public static String validateMobile(String mobile) throws HermesException {

        if (mobile==null || mobile.length()==0)
            throw new HermesException(BAD_REQUEST, "Invalid parameter", "Mobile number can not be null or empty.");

        Pattern pattern = Pattern.compile(MOBILE);
        Matcher matcher = pattern.matcher(mobile);

        // Если соответствует шаблону
        if (matcher.matches()) {
            return "+" + matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4) + matcher.group(5);
        } else {
            throw new HermesException(BAD_REQUEST, "Invalid parameter",
                    "Mobile number '" + mobile + "' does not match 11 digit patterns: " +
                    "+XXXXXXXXXXX, +X (XXX) XXX XX XX, +X (XXX) XXX-XX-XX etc");
        }

    }

    /**
     * Проверяет является ли БИН/ИИН валидным
     * @param businessID БИН/ИИН
     * @throws HermesException
     */
    public static void validateBusinessID(String businessID) throws HermesException {
        if (businessID==null || businessID.length()==0)
            throw new HermesException(BAD_REQUEST, "Invalid parameter", "businessID can not be null or empty.");

        Pattern pattern = Pattern.compile(BIN);
        Matcher matcher = pattern.matcher(businessID);

        if (!matcher.matches()) {
            throw new HermesException(BAD_REQUEST, "Invalid parameter",
             "Business identification number '" + businessID + "' does not match 9/10/12 or 14 digit pattern.");
        }
    }


    public static void validateName(String name) throws HermesException {
        if (name==null || name.length()==0) {
            throw new HermesException(BAD_REQUEST, "Invalid parameter", "Product name can not be null or empty.");
        }
    }

    public static void validateVendorCode(String name) throws HermesException {
        if (name==null  || name.length()==0) {
            throw new HermesException(BAD_REQUEST, "Invalid parameter", "vendorCode can not be null or empty.");
        }
    }

    /**
     * Проверяет является ли число натуральным и если нет кидает исключение
     * @param name названия параметра для сообщения об ошибке
     * @param value проверяемое значение
     * @throws HermesException
     */
    public static void nonNegativeInteger(String name, long value) throws HermesException {
        if (value < 0) {
            throw new HermesException(BAD_REQUEST, "Invalid parameter",
                    "Parameter '" + name + "' is less than zero");
        }
    }

    /**
     * Проверяет является число положительным
     * @param name названия параметра для сообщения об ошибке
     * @param value проверяемое значение
     * @throws HermesException
     */
    public static void nonNegativeNumber(String name, double value) throws HermesException {
        if (value < 0) {
            throw new HermesException(BAD_REQUEST, "Invalid parameter",
                    "Parameter '" + name + "' is less than zero");
        }
    }

    //--------------------------------------------------------------------------------------------------------
    // здесь его быть не должно - потому что не валидация, а конвертация
    /**
     * Конвертирует объект неизвестного типа в Long
     * @param object объект неизвестного типа
     * @return значение или 0 - если не получилось
     */
    public static long asLong(Object object) {
        if (object==null) return 0;
        if (object instanceof Integer) return ((Integer) object).longValue();
        if (object instanceof BigInteger) return ((BigInteger) object).longValue();
        if (object instanceof BigDecimal) return ((BigDecimal) object).longValue();
        if (object instanceof Long) return (Long) object;
        if (object instanceof String) {
            try {
                return Long.parseLong((String) object);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

}
