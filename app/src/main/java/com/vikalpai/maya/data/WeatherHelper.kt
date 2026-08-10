package com.vikalpai.maya.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Live weather via Open-Meteo (open-meteo.com) — free, no API key needed,
 * unlike most weather APIs. Two calls: geocode the city name to lat/lon,
 * then fetch current conditions for that point.
 */
class WeatherHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun getWeather(city: String): String {
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1"
        val geoRequest = Request.Builder().url(geoUrl).build()

        client.newCall(geoRequest).execute().use { geoResponse ->
            if (!geoResponse.isSuccessful) return "Weather nahi mila"
            val geoBody = geoResponse.body?.string() ?: return "Weather nahi mila"
            val geoJson = JSONObject(geoBody)
            val results = geoJson.optJSONArray("results") ?: return "'$city' naam ki jagah nahi mili"
            if (results.length() == 0) return "'$city' naam ki jagah nahi mili"

            val place = results.getJSONObject(0)
            val lat = place.getDouble("latitude")
            val lon = place.getDouble("longitude")
            val placeName = place.optString("name", city)

            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,relative_humidity_2m"
            val weatherRequest = Request.Builder().url(weatherUrl).build()

            client.newCall(weatherRequest).execute().use { weatherResponse ->
                if (!weatherResponse.isSuccessful) return "Weather nahi mila"
                val weatherBody = weatherResponse.body?.string() ?: return "Weather nahi mila"
                val weatherJson = JSONObject(weatherBody)
                val current = weatherJson.optJSONObject("current") ?: return "Weather nahi mila"
                val temp = current.optDouble("temperature_2m", Double.NaN)
                val humidity = current.optInt("relative_humidity_2m", -1)
                val code = current.optInt("weather_code", -1)
                val condition = describeWeatherCode(code)

                return buildString {
                    append("$placeName mein abhi ${temp.toInt()}°C, $condition")
                    if (humidity >= 0) append(", humidity $humidity%")
                }
            }
        }
    }

    private fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "saaf aasman"
        1, 2, 3 -> "halke baadal"
        45, 48 -> "kohra"
        51, 53, 55, 56, 57 -> "halki boondabaandi"
        61, 63, 65, 66, 67 -> "baarish"
        71, 73, 75, 77 -> "barfbaari"
        80, 81, 82 -> "tez baarish"
        95, 96, 99 -> "aandhi-toofan"
        else -> "mixed weather"
    }
}
