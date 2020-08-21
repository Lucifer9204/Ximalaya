package bmw.ximalaya.test.media

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import bmw.ximalaya.test.extensions.NeuLog
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaCrypto
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource

/**
 * Class to bridge UAMP to the ExoPlayer MediaSession extension.
 */
class XmlyPlaybackPreparer(
    private val musicSource: XmlyMusicSource,
    private val exoPlayer: ExoPlayer,
    private val dataSourceFactory: DataSource.Factory,
    private val dataHttpSourceFactory: DataSource.Factory
) : MediaSessionConnector.PlaybackPreparer {

    /**
     * UAMP supports preparing (and playing) from search, as well as media ID, so those
     * capabilities are declared here.
     *
     * TODO: Add support for ACTION_PREPARE and ACTION_PLAY, which mean "prepare/play something".
     */
    override fun getSupportedPrepareActions(): Long =
        PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_PLAY_FROM_URI or
                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM


    override fun onPrepare(playWhenReady: Boolean) = Unit

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        musicSource.whenReady {
            val itemToPlay: MediaMetadataCompat? = musicSource.find { item ->
                item.id == mediaId
            }
            if (itemToPlay == null) {
                Log.w(TAG, "Content not found: MediaID=$mediaId")

                // TODO: Notify caller of the error.
            } else {
                val metadataList = buildPlaylist(itemToPlay)
                val mediaSource: ConcatenatingMediaSource
                if (itemToPlay.mediaUri.toString().contains(".m3u8")) {
                    NeuLog.e("m3u8:${itemToPlay.mediaUri.toString()}")
                    mediaSource = ConcatenatingMediaSource()
                    mediaSource.addMediaSource(
                        HlsMediaSource.Factory(dataHttpSourceFactory)
                            .setTag(itemToPlay.description)
                            .createMediaSource(Uri.parse(itemToPlay.mediaUri.toString()))
                    )
                } else {
                    NeuLog.e("m4a:${itemToPlay.mediaUri.toString()}")
                    mediaSource = metadataList.toMediaSource(dataSourceFactory)
                }

                // Since the playlist was probably based on some ordering (such as tracks
                // on an album), find which window index to play first so that the song the
                // user actually wants to hear plays first.
                val initialWindowIndex = metadataList.indexOf(itemToPlay)

                exoPlayer.prepare(mediaSource)
                exoPlayer.seekTo(initialWindowIndex, 0)
                exoPlayer.playWhenReady = playWhenReady

                NeuLog.e("RepeatMode" + (exoPlayer.repeatMode))
                NeuLog.e("getTrackSelection" + (exoPlayer.currentTrackSelections))
                NeuLog.e("getTrackGroup" + (exoPlayer.currentTrackGroups))

                val itemToRencent: MediaMetadataCompat? = musicSource.rencentList.find { item ->
                    item.id == mediaId
                }
                if (itemToRencent != null) {
                    if (musicSource.rencentList[0].id == mediaId) {
                        return@whenReady;
                    }
                    musicSource.rencentList.remove(itemToRencent)
                }

                musicSource.rencentList.add(0, itemToPlay)
            }
        }
    }


    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
        musicSource.whenReady {
            val metadataList = musicSource.search(query, extras ?: Bundle.EMPTY)
            if (metadataList.isNotEmpty()) {
                val mediaSource = metadataList.toMediaSource(dataSourceFactory)
                exoPlayer.prepare(mediaSource)
                exoPlayer.playWhenReady = playWhenReady
            }
        }
    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

    override fun onCommand(
        player: Player,
        controlDispatcher: ControlDispatcher,
        command: String,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = false

    /**
     * Builds a playlist based on a [MediaMetadataCompat].
     *
     * TODO: Support building a playlist by artist, genre, etc...
     *
     * @param item Item to base the playlist on.
     * @return a [List] of [MediaMetadataCompat] objects representing a playlist.
     */
    private fun buildPlaylist(item: MediaMetadataCompat): List<MediaMetadataCompat> =
        musicSource.filter { it.album == item.album }.sortedBy { it.trackNumber }
}

private const val TAG = "MediaSessionHelper"
