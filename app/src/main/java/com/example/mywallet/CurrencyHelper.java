package com.example.mywallet;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DecimalFormat;

/**
 * CurrencyHelper manages user-selected currency formatting across all app screens and PDF reports.
 */
public class CurrencyHelper {

    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    public static final String[] SUPPORTED_CURRENCIES = {
            "USD ($)",
            "LKR (Rs)",
            "EUR (€)"
    };

    /**
     * Approximate exchange rates against base USD:
     * 1 USD = 1.00 USD
     * 1 USD = 300.00 LKR
     * 1 USD = 0.92 EUR
     */
    public static double getExchangeRate(String currencyString) {
        if (currencyString == null) return 1.0;
        if (currencyString.contains("LKR") || currencyString.contains("Rs")) {
            return 300.0;
        } else if (currencyString.contains("EUR") || currencyString.contains("€")) {
            return 0.92;
        }
        return 1.0; // Default USD
    }

    public static String getCurrencySymbol(Context context) {
        if (context == null) return "$";
        SharedPreferences prefs = context.getSharedPreferences("my_wallet_prefs", Context.MODE_PRIVATE);
        String currencyStr = prefs.getString("currency", "USD ($)");
        return extractSymbol(currencyStr);
    }

    public static String extractSymbol(String currencyString) {
        if (currencyString == null) return "$";
        if (currencyString.contains("(") && currencyString.contains(")")) {
            int start = currencyString.lastIndexOf("(");
            int end = currencyString.lastIndexOf(")");
            if (start != -1 && end != -1 && end > start) {
                return currencyString.substring(start + 1, end).trim();
            }
        }
        if (currencyString.contains("USD")) return "$";
        if (currencyString.contains("EUR")) return "€";
        if (currencyString.contains("LKR") || currencyString.contains("Rs")) return "Rs";
        return "$";
    }

    public static String formatAmount(Context context, double amount) {
        String symbol = getCurrencySymbol(context);
        boolean needsSpace = symbol.length() > 1 || symbol.equals("Rs");
        String prefix = needsSpace ? symbol + " " : symbol;

        if (amount < 0) {
            return "-" + prefix + df.format(Math.abs(amount));
        }
        return prefix + df.format(amount);
    }
}
