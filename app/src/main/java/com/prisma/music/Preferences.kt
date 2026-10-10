package com.prisma.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class VisualTheme(val title: String) { CHROMATIC("Chromatic Flow"), GLASS("Liquid Glass"), MINIMAL("Pure Minimal"), NIGHT("Black Night"), STUDIO("Studio"), SUNSET("Sunset") }
enum class LightMode(val title: String) { SYSTEM("Según el sistema"), DARK("Oscuro"), LIGHT("Claro") }

enum class LibraryLayout(val title:String) { THEME("Según el tema"), LIST("Lista"), GRID("Cuadrícula"), COMPACT("Lista compacta") }
enum class SongInfoStyle(val title:String) { ARTIST("Artista"), ALBUM("Artista y álbum"), FILE("Nombre del archivo") }
data class Personalization(val accent:String="",val layout:LibraryLayout=LibraryLayout.THEME,val info:SongInfoStyle=SongInfoStyle.ARTIST)

class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("prisma", Context.MODE_PRIVATE)
    var personalization: Personalization
        get() = runCatching { val j=JSONObject(prefs.getString("personalization","{}")!!); Personalization(j.optString("accent"),LibraryLayout.valueOf(j.optString("layout","THEME")),SongInfoStyle.valueOf(j.optString("info","ARTIST"))) }.getOrDefault(Personalization())
        set(v) { prefs.edit().putString("personalization",JSONObject().put("accent",v.accent).put("layout",v.layout.name).put("info",v.info.name).toString()).apply() }
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
    var showMetadata: Boolean
        get() = prefs.getBoolean("showMetadata", true)
        set(v) { prefs.edit().putBoolean("showMetadata", v).apply() }
    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keepScreenOn", false)
        set(v) { prefs.edit().putBoolean("keepScreenOn", v).apply() }
    var skipSilence: Boolean
        get() = prefs.getBoolean("skipSilence", false)
        set(v) { prefs.edit().putBoolean("skipSilence", v).apply() }
    var playbackSpeed: Float
        get() = prefs.getFloat("playbackSpeed", 1f).coerceIn(.5f, 2f)
        set(v) { prefs.edit().putFloat("playbackSpeed", v.coerceIn(.5f, 2f)).apply() }
    var minimumDuration: Int
        get() = prefs.getInt("minimumDuration", 0).coerceIn(0, 120)
        set(v) { prefs.edit().putInt("minimumDuration", v.coerceIn(0, 120)).apply() }
    // Wall-clock deadline survives activity recreation; the service owns the timer.
    var sleepDeadline: Long
        get() = prefs.getLong("sleepDeadline", 0)
        set(v) { prefs.edit().putLong("sleepDeadline", v).apply() }
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
    fun recentIds(): List<String> = runCatching {
        val a=JSONArray(prefs.getString("recentSongs","[]")!!)
        List(a.length()) {a.getString(it)}
    }.getOrDefault(emptyList())
    fun rememberSong(id:String) { if(id.isNotBlank()) prefs.edit().putString("recentSongs",JSONArray((listOf(id)+recentIds()).distinct().take(24)).toString()).apply() }
    fun lyrics(id: String): String = prefs.getString("lyrics:$id", "")!!
    fun saveLyrics(id: String, text: String) { prefs.edit().putString("lyrics:$id", text).apply() }
}
