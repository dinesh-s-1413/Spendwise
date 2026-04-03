package com.example.spendwise

import android.content.Context

object CurrencyHelper {

    private const val PREF_NAME = "SpendWise_Prefs"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    private const val KEY_CURRENCY_CODE = "currency_code"
    private const val KEY_COUNTRY = "country"

    // Country to Currency mapping
    val countryList = listOf(
        "India" to "₹",
        "United States" to "$",
        "United Kingdom" to "£",
        "European Union" to "€",
        "Japan" to "¥",
        "Australia" to "A$",
        "Canada" to "C$",
        "Switzerland" to "CHF",
        "China" to "¥",
        "Singapore" to "S$",
        "UAE" to "AED",
        "Saudi Arabia" to "SAR",
        "Brazil" to "R$",
        "South Korea" to "₩",
        "Mexico" to "MX$",
        "Russia" to "₽",
        "South Africa" to "R",
        "Nigeria" to "₦",
        "Sweden" to "kr",
        "Norway" to "kr",
        "Denmark" to "kr",
        "New Zealand" to "NZ$",
        "Malaysia" to "RM",
        "Thailand" to "฿",
        "Indonesia" to "Rp",
        "Philippines" to "₱",
        "Pakistan" to "₨",
        "Bangladesh" to "৳",
        "Sri Lanka" to "₨",
        "Nepal" to "₨"
    )

    fun saveCountry(context: Context, country: String) {
        val symbol = countryList.find { it.first == country }?.second ?: "$"
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COUNTRY, country)
            .putString(KEY_CURRENCY_SYMBOL, symbol)
            .apply()
    }

    fun getCurrencySymbol(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENCY_SYMBOL, "$") ?: "$"
    }

    fun getCountry(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COUNTRY, "United States") ?: "United States"
    }

    fun getCountryNames(): List<String> {
        return countryList.map { it.first }
    }
}