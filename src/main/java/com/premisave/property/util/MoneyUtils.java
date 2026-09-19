package com.premisave.property.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** One place that decides how amounts are shown to users: "USD 1,500.00". */
public final class MoneyUtils {

    public static final String CURRENCY = Constants.DEFAULT_CURRENCY;

    private MoneyUtils() {
    }

    /** "1,500.00" — no currency code. */
    public static String amount(BigDecimal value) {
        BigDecimal v = value != null ? value : BigDecimal.ZERO;
        // DecimalFormat isn't thread-safe, so build one per call.
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(v.setScale(2, RoundingMode.HALF_UP));
    }

    /** "USD 1,500.00" */
    public static String format(BigDecimal value) {
        return CURRENCY + " " + amount(value);
    }
}