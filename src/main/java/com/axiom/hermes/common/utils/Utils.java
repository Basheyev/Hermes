package com.axiom.hermes.common.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Utils {

    /**
     * Конвертирует объект неизвестного типа в Long
     * @param object объект неизвестного типа
     * @return значение или 0 - если не получилось
     */
    public static long asLong(Object object) {
        if (object==null) return 0;
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
