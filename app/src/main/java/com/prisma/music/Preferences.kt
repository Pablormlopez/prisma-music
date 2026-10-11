package com.prisma.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class VisualTheme(val title: String) { CHROMATIC("Chromatic Flow"), GLASS("Liquid Glass"), MINIMAL("Pure Minimal"), NIGHT("Black Night"), STUDIO("Studio"), SUNSET("Sunset") }
enum class LightMode(val title: String) { SYSTEM("Según el sistema"), DARK("Oscuro"), LIGHT("Claro") }

enum class LibraryLayout(val title:String) { THEME("Según el tema"), LIST("Lista"), GRID("Cuadrícula"), COMPACT("Lista compacta") }
enum class SongInfoStyle(val title:String) { ARTIST("Artista"), ALBUM("Artista y álbum"), FILE("Nombre del archivo") }
data class Personalization(val accent:String="",val layout:LibraryLayout=LibraryLayout.THEME,val info:SongInfoStyle=SongInfoStyle.ARTIST,val motion:MotionStyle=MotionStyle.SOFT,val motionDuration:Int=550)

class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("prisma", Context.MODE_PRIVATE)
    var personalization: Personalization
        get() = runCatching { val j=JSONObject(prefs.getString("personalization","{}")!!); Personalization(j.optString("accent"),LibraryLayout.valueOf(j.optString("layout","THEME")),SongInfoStyle.valueOf(j.optString("info","ARTIST")),runCatching {MotionStyle.valueOf(j.optString("motion","SOFT"))}.getOrDefault(MotionStyle.SOFT),j.optInt("motionDuration",550).coerceIn(250,900)) }.getOrDefault(Personalization())
        set(v) { prefs.edit().putString("personalization",JSONObject().put("accent",v.accent).put("layout",v.layout.name).put("info",v.info.name).put("motion",v.motion.name).put("motionDuration",v.motionDuration.coerceIn(250,900)).toString()).apply() }
    var librarySort: SongSort
        get() = runCatching { SongSort.valueOf(prefs.getString("librarySort", "TITLE_ASC")!!) }.getOrDefault(SongSort.TITLE_ASC)
        set(v) { prefs.edit().putString("librarySort", v.name).apply() }
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
    var showPlayerAlbum: Boolean
        get() = prefs.getBoolean("showPlayerAlbum", false)
        set(v) { prefs.edit().putBoolean("showPlayerAlbum", v).apply() }
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
    var fadeInSeconds:Float
        get() = prefs.getFloat("fadeInSeconds",0f).coerceIn(0f,12f)
        set(v) {prefs.edit().putFloat("fadeInSeconds",v.coerceIn(0f,12f)).apply()}
    var fadeOutSeconds:Float
        get() = prefs.getFloat("fadeOutSeconds",0f).coerceIn(0f,12f)
        set(v) {prefs.edit().putFloat("fadeOutSeconds",v.coerceIn(0f,12f)).apply()}
    var eq:DspEngine.Config
        get() = runCatching {decodeEq(JSONObject(prefs.getString("eq","{}")!!))}.getOrDefault(DspEngine.Config.flat())
        set(v) {prefs.edit().putString("eq",encodeEq(v).toString()).apply()}
    fun eqPresets():Map<String,DspEngine.Config> = runCatching {
        val j=JSONObject(prefs.getString("eqPresets","{}")!!)
        j.keys().asSequence().associateWith {decodeEq(j.getJSONObject(it))}
    }.getOrDefault(emptyMap())
    fun saveEqPresets(presets:Map<String,DspEngine.Config>) {
        val j=JSONObject();presets.forEach {(name,config)->j.put(name,encodeEq(config))}
        prefs.edit().putString("eqPresets",j.toString()).apply()
    }
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
