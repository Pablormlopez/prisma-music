package com.prisma.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class VisualTheme(val title: String) { CHROMATIC("Chromatic Flow"), GLASS("Liquid Glass"), MINIMAL("Pure Minimal"), NIGHT("Black Night") }
enum class LightMode(val title: String) { SYSTEM("Según el sistema"), DARK("Oscuro"), LIGHT("Claro") }

class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("prisma", Context.MODE_PRIVATE)
    var theme: VisualTheme
        get() = runCatching { VisualTheme.valueOf(prefs.getString("theme", "CHROMATIC")!!) }.getOrDefault(VisualTheme.CHROMATIC)
        set(v) { prefs.edit().putString("theme", v.name).apply() }
    var mode: LightMode
        get() = runCatching { LightMode.valueOf(prefs.getString("mode", "SYSTEM")!!) }.getOrDefault(LightMode.SYSTEM)
        set(v) { prefs.edit().putString("mode", v.name).apply() }
    var hideWhatsApp: Boolean
        get() = prefs.getBoolean("hideWhatsApp", true)
        set(v) { prefs.edit().putBoolean("hideWhatsApp", v).apply() }
    var excluded: Set<String>
        get() = prefs.getStringSet("excluded", emptySet())!!.toSet()
        set(v) { prefs.edit().putStringSet("excluded", v).apply() }
    var eq: DspEngine.Config
        get() = runCatching {
            val j = JSONObject(prefs.getString("eq", "{}")!!)
            val gains = j.optJSONArray("gains")
            DspEngine.Config(j.optBoolean("enabled"), DoubleArray(10) { gains?.optDouble(it, 0.0) ?: 0.0 }, j.optDouble("q", 1.0), j.optDouble("preamp", 0.0), j.optDouble("bass", 0.0), j.optDouble("mids", 0.0), j.optDouble("treble", 0.0), j.optDouble("voice", 0.0))
        }.getOrDefault(DspEngine.Config.flat())
        set(v) { prefs.edit().putString("eq", JSONObject().put("enabled",v.enabled).put("gains",JSONArray(v.gains.toList())).put("q",v.q).put("preamp",v.preamp).put("bass",v.bass).put("mids",v.mids).put("treble",v.treble).put("voice",v.voice).toString()).apply() }
    fun playlists(): Map<String, List<String>> = runCatching {
        val j = JSONObject(prefs.getString("playlists", "{}")!!)
        j.keys().asSequence().associateWith { key -> val a=j.getJSONArray(key); List(a.length()) { a.getString(it) } }
    }.getOrDefault(emptyMap())
    fun savePlaylists(lists: Map<String,List<String>>) { val j=JSONObject(); lists.forEach { (k,v)-> j.put(k,JSONArray(v)) }; prefs.edit().putString("playlists",j.toString()).apply() }
    fun lyrics(id: String): String = prefs.getString("lyrics:$id", "")!!
    fun saveLyrics(id: String, text: String) { prefs.edit().putString("lyrics:$id", text).apply() }
}
