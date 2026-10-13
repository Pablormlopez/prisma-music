package com.prisma.music

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/** Two decoders overlap only the audible boundary. The session/queue stays owned by the primary. */
@UnstableApi internal class AutoMix(private val primary:ExoPlayer,private val incoming:ExoPlayer,private val outputGain:()->Float={1f}) {
    var seconds=0f
    private var target:MediaItem?=null
    private var origin:String?=null
    private var window=0L
    var mixing=false;private set
    fun cancel() {mixing=false;target=null;origin=null;window=0;incoming.stop();incoming.clearMediaItems();incoming.volume=0f}
    fun tick():Float {
        val next=primary.nextMediaItemIndex
        if(seconds<=0 || !primary.isPlaying || primary.repeatMode==Player.REPEAT_MODE_ONE || next==C.INDEX_UNSET || next==primary.currentMediaItemIndex || primary.duration<=0) {if(target!=null)cancel();return 1f}
        val item=primary.getMediaItemAt(next)
        val current=primary.currentMediaItem?.mediaId
        if(target!=null && (target!=item || origin!=current))cancel()
        val remaining=(primary.duration-primary.currentPosition).coerceAtLeast(0)
        val planned=minOf((seconds*1000*primary.playbackParameters.speed).toLong(),primary.duration/2).coerceAtLeast(1)
        if(remaining<=planned+2000 && target==null) {
            target=item;origin=current
            incoming.setMediaItem(item);incoming.setPlaybackSpeed(primary.playbackParameters.speed)
            incoming.volume=0f;incoming.prepare()
        }
        if(target!=null && !mixing && remaining in 1..planned && incoming.playbackState==Player.STATE_READY) {
            window=remaining;mixing=true;incoming.play()
        }
        if(mixing) {
            val progress=(1f-remaining.toFloat()/window.coerceAtLeast(1)).coerceIn(0f,1f)
            val gains=mixGains(progress);incoming.volume=gains.second*outputGain()
            return gains.first
        }
        return 1f
    }
    fun handoff(item:MediaItem?) {
        if(mixing && item==target) {
            val position=incoming.currentPosition
            cancel()
            primary.volume=1f
            primary.seekTo(position)
        } else if(target!=null)cancel()
    }
}
