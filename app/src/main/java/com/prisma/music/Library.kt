package com.prisma.music

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.palette.graphics.Palette

data class Song(val id: String, val uri: Uri, val title: String, val artist: String, val album: String, val genre: String, val folder: String, val name: String, val duration: Long, val size: Long, val mime: String, val art: Uri, val dateAdded: Long = 0) {
    fun mediaItem() = MediaItem.Builder().setMediaId(id).setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(art).build()).build()
}

object Library {
    fun isWhatsApp(folder: String): Boolean = folder.split('/','\\').any { it.equals("WhatsApp",true) || it.equals("com.whatsapp",true) || it.equals("com.whatsapp.w4b",true) || it.startsWith("WhatsApp ",true) }
    fun visible(songs: List<Song>, prefs: Preferences): List<Song> = songs.filter { (!prefs.hideWhatsApp || !isWhatsApp(it.folder)) && it.folder !in prefs.excluded && it.duration >= prefs.minimumDuration * 1000L }
    fun scan(context: Context): List<Song> {
        val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val folderColumn = if(Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        val columns = mutableListOf("_id","title","artist","album","album_id","duration","_size","mime_type","_display_name",MediaStore.MediaColumns.DATE_ADDED,folderColumn)
        if(Build.VERSION.SDK_INT >= 30) columns.add("genre")
        val result = mutableListOf<Song>()
        context.contentResolver.query(base, columns.toTypedArray(), null, null, "title COLLATE NOCASE ASC")?.use { c ->
            fun str(key:String) = c.getColumnIndex(key).let { if(it<0 || c.isNull(it)) "" else c.getString(it) }
            fun number(key:String) = c.getColumnIndex(key).let { if(it<0 || c.isNull(it)) 0L else c.getLong(it) }
            while(c.moveToNext()) {
                val id=number("_id"); val name=str("_display_name")
                val folder = str(folderColumn).let { if(Build.VERSION.SDK_INT < 29) it.substringBeforeLast('/',"") else it }.trimEnd('/')
                result.add(Song(id.toString(),ContentUris.withAppendedId(base,id),str("title").ifBlank { name.substringBeforeLast('.') },str("artist").takeUnless { it=="<unknown>" || it.isBlank() } ?: "Artista desconocido",str("album").takeUnless { it=="<unknown>" || it.isBlank() } ?: "Sin álbum",str("genre").ifBlank { "Sin género" },folder,name,number("duration"),number("_size"),str("mime_type"),ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),number("album_id")),number(MediaStore.MediaColumns.DATE_ADDED)))
            }
        }
        return result
    }
    data class Details(val text: String, val art: Bitmap?, val color: Int?, val embeddedLyrics: String = "", val glassBackdrop: Bitmap? = null,val sampleRate:Int?=null,val channels:Int?=null,val sourceBits:Int?=null)
    fun details(context: Context, song: Song): Details {
        var art: Bitmap? = null
        var rate:Int? = null; var channels:Int? = null; var bitrate:Int? = null; var bits:Int? = null; var codec:String? = null
        val retriever=MediaMetadataRetriever()
        try {
            retriever.setDataSource(context,song.uri)
            bitrate=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            retriever.embeddedPicture?.let { bytes ->
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }; BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                val options=BitmapFactory.Options().apply { inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/800).coerceAtLeast(1) }
                art=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
            }
        } catch(_:Exception) {} finally { retriever.release() }
        val extractor=MediaExtractor()
        try {
            extractor.setDataSource(context,song.uri,null)
            for(i in 0 until extractor.trackCount) {
                val f=extractor.getTrackFormat(i)
                if(f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true) {
                    codec=f.getString(MediaFormat.KEY_MIME)
                    fun value(k:String) = if(f.containsKey(k)) runCatching { f.getInteger(k) }.getOrNull() else null
                    rate=value(MediaFormat.KEY_SAMPLE_RATE); channels=value(MediaFormat.KEY_CHANNEL_COUNT)
                    bitrate=value(MediaFormat.KEY_BIT_RATE) ?: bitrate
                    bits=value("bits-per-sample")
                    break
                }
            }
        } catch(_:Exception) {} finally { extractor.release() }
        val estimate=if(song.duration>0) song.size*8/song.duration else null
        val parts=listOfNotNull(song.name.substringAfterLast('.',song.mime).uppercase(),codec,bitrate?.let { "${it/1000} kbps" } ?: estimate?.let { "≈ $it kbps (estimado)" },rate?.let { "${it/1000.0} kHz" },bits?.let { "$it bits" },channels?.let { if(it==2) "Estéreo" else "$it canal(es)" },"%.1f MB".format(song.size/1000000.0))
        Preferences(context).animatedArtwork(song.id)?.let {uri->
            runCatching {
                val u=Uri.parse(uri)
                val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                context.contentResolver.openInputStream(u)?.use {BitmapFactory.decodeStream(it,null,bounds)}
                val sampled=BitmapFactory.Options().apply {inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/800).coerceAtLeast(1)}
                context.contentResolver.openInputStream(u)?.use {BitmapFactory.decodeStream(it,null,sampled)}
            }.getOrNull()?.let {art=it}
        }
        val color=art?.let { Palette.from(it).generate().getVibrantColor(0xffb0a2ff.toInt()) }
        val lyrics=runCatching { context.contentResolver.openInputStream(song.uri)?.use { EmbeddedLyrics.read(it) } ?: "" }.getOrDefault("")
        return Details(parts.joinToString(" · "),art,color,lyrics,art?.let { FrostedArtwork.create(it) },rate,channels,bits)
    }
}

data class LyricLine(val time: Long?, val text: String)
object Lyrics {
    private val timestamp=Regex("\\[(\\d+):(\\d{2})(?:[.:](\\d{1,3}))?]")
    fun parse(text:String): List<LyricLine> {
        val offset=Regex("\\[offset:([+-]?\\d+)]",RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0
        val result=mutableListOf<LyricLine>()
        text.lineSequence().forEach { raw ->
            val matches=timestamp.findAll(raw).toList()
            val content=raw.replace(timestamp,"").trim()
            if(matches.isNotEmpty()) matches.forEach { m ->
                val fraction=m.groupValues[3].padEnd(3,'0').take(3).toLongOrNull() ?: 0
                result.add(LyricLine((m.groupValues[1].toLong()*60000+m.groupValues[2].toLong()*1000+fraction+offset).coerceAtLeast(0),content))
            } else if(content.isNotBlank() && !content.matches(Regex("\\[[a-zA-Z]+:.*]"))) result.add(LyricLine(null,content))
        }
        return if(result.any { it.time!=null }) result.filter { it.time!=null }.sortedBy { it.time } else result
    }
}
