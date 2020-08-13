package bmw.ximalaya.test.media

import android.media.MediaMetadata
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import bmw.ximalaya.test.extensions.NeuLog
import bmw.ximalaya.test.extensions.XmlyMediaPlayer
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 *
 *
 * To implement a MediaBrowserService, you need to:
 *
 *  *  Extend [MediaBrowserServiceCompat], implementing the media browsing
 * related methods [MediaBrowserServiceCompat.onGetRoot] and
 * [MediaBrowserServiceCompat.onLoadChildren];
 *
 *  *  In onCreate, start a new [MediaSessionCompat] and notify its parent
 * with the session's token [MediaBrowserServiceCompat.setSessionToken];
 *
 *  *  Set a callback on the [MediaSessionCompat.setCallback].
 * The callback will receive all the user's actions, like play, pause, etc;
 *
 *  *  Handle all the actual music playing using any method your app prefers (for example,
 * [android.media.MediaPlayer])
 *
 *  *  Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * [MediaSessionCompat.setPlaybackState]
 * [MediaSessionCompat.setMetadata] and
 * [MediaSessionCompat.setQueue])
 *
 *  *  Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 *
 * To make your app compatible with Android Auto, you also need to:
 *
 *  *  Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 *
 */
class MyMusicService : MediaBrowserServiceCompat() {

    /**
     * Configure ExoPlayer to handle audio focus for us.
     * See [Player.AudioComponent.setAudioAttributes] for details.
     */
    private val exoPlayer: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).build().apply {
            setAudioAttributes(xMLYAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
    }

     val xmlyPlayer by lazy {
        XmlyMediaPlayer(this)
    }

    private lateinit var session: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector
    private val musicSource by lazy { XmlyMusicSource(this) }
    private val browseTree: BrowseTree by lazy {
        BrowseTree(applicationContext, musicSource, xmlyPlayer)
    }

    private lateinit var packageValidator: PackageValidator

    private val xMLYAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerEventListener()


    /**
     * Listen for events from ExoPlayer.
     */
    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {

                    // If playback is paused we remove the foreground state which allows the
                    // notification to be dismissed. An alternative would be to provide a "close"
                    // button in the notification which stops playback and clears the notification.
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) stopForeground(false)
                        if (callback != null) {

                        }
                    }
                }
                else -> {

                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            var message = R.string.generic_error;
            when (error.type) {
                // If the data from MediaSource object could not be loaded the Exoplayer raises
                // a type_source error.
                // An error message is printed to UI via Toast message to inform the user.
                ExoPlaybackException.TYPE_SOURCE -> {
                    message = R.string.error_media_not_found;
                    NeuLog.e(TAG, "TYPE_SOURCE: " + error.sourceException.message)
                }
                // If the error occurs in a render component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_RENDERER -> {
                    NeuLog.e(TAG, "TYPE_RENDERER: " + error.rendererException.message)
                }
                // If occurs an unexpected RuntimeException Exoplayer raises a type_unexpected error.
                ExoPlaybackException.TYPE_UNEXPECTED -> {
                    NeuLog.e(TAG, "TYPE_UNEXPECTED: " + error.unexpectedException.message)
                }
                // Occurs when there is a OutOfMemory error.
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                    NeuLog.e(TAG, "TYPE_OUT_OF_MEMORY: " + error.outOfMemoryError.message)
                }
                // If the error occurs in a remote component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_REMOTE -> {
                    NeuLog.e(TAG, "TYPE_REMOTE: " + error.message)
                }
            }
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            session.setPlaybackState(playbackState)
            updatePlayingInfo()
        }

        override fun onSkipToQueueItem(queueId: Long) {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM)
            session.setPlaybackState(playbackState)
        }

        override fun onSeekTo(position: Long) {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            session.setPlaybackState(playbackState)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            NeuLog.e("$mediaId")

            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            session.setPlaybackState(playbackState)
            updatePlayingInfo()
        }

        override fun onPause() {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_PAUSED)

            session.setPlaybackState(playbackState)
            updatePlayingInfo()
        }

        override fun onStop() {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_STOPPED)

            session.setPlaybackState(playbackState)
        }

        override fun onSkipToNext() {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)

            session.setPlaybackState(playbackState)
        }

        override fun onSkipToPrevious() {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS)
            session.setPlaybackState(playbackState)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            NeuLog.e(action)
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            session.setPlaybackState(playbackState)

            session.setQueue(listOf(MediaSessionCompat.QueueItem(musicSource.iterator().next().description,0)))
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            NeuLog.e()
            val playbackState = createPlaybackState(PlaybackStateCompat.STATE_PLAYING)

            session.setPlaybackState(playbackState)
            updatePlayingInfo()
        }
    }

     fun updatePlayingInfo() {
        val song = musicSource.iterator().next()
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, song.displayTitle)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, 410)
            .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, 1)
            .putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, 10)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
            .putString(
                MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,song.albumArtUri.toString()
            )
            .putString(
                MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                song.albumArtUri.toString()
            )
            .putLong(EXTRA_IS_EXPLICIT, EXTRA_METADATA_ENABLED_VALUE)
            .putLong(EXTRA_IS_DOWNLOADED, EXTRA_METADATA_ENABLED_VALUE)
            .build()
        session.setMetadata(metadata)
        session.setQueueTitle("queue title")
        session.setQueue(listOf(MediaSessionCompat.QueueItem(song.description, 0),MediaSessionCompat.QueueItem(song.description, 2),MediaSessionCompat.QueueItem(song.description, 3)))
    }

    override fun onSearch(query: String, extras: Bundle?, result: Result<MutableList<MediaItem>>) {
        NeuLog.e("$query")
//        super.onSearch(query, extras, result)
        if(query == "home"){
            val mediaItems = mutableListOf<MediaItem>()

            result.sendResult(mediaItems)
        } else {
            result.detach()
        }
    }
    override fun onCreate() {
        super.onCreate()
        NeuLog.e()
        //xmlyPlayer
        browseTree.init()

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        session = MediaSessionCompat(this, "MyMusicService")
        sessionToken = session.sessionToken
        session.setCallback(callback)
        session.setFlags(
            MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        val playbackState = createPlaybackState(PlaybackStateCompat.STATE_PLAYING)

        session.setPlaybackState(playbackState)
        session.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)


        // ExoPlayer will manage the MediaSession for us.
        mediaSessionConnector = MediaSessionConnector(session).also { connector ->
            // Produces DataSource instances through which media data is loaded.
            val dataSourceFactory = DefaultDataSourceFactory(
                this, Util.getUserAgent(this, XMLY_USER_AGENT), null
            )

            // Create the PlaybackPreparer of the media session connector.
            val playbackPreparer = XmlyPlaybackPreparer(
                musicSource,
                exoPlayer,
                dataSourceFactory
            )

            connector.setPlayer(exoPlayer)
            connector.setPlaybackPreparer(playbackPreparer)
            /*connector.setCustomActionProviders(
            )*/
            connector.setQueueNavigator(XmlyQueueNavigator(session))
        }

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
    }

    private fun createPlaybackState(@State state: Int): PlaybackStateCompat? {
        val it = musicSource.iterator()
        val extras = if(it.hasNext())it.next().bundle else Bundle()
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
            .setActiveQueueItemId(0)
            .setBufferedPosition(20)
            .addCustomAction("a", "a", R.drawable.ic_recommended)
            .addCustomAction("c", "c", R.drawable.ic_recommended)
            .addCustomAction("d", "d", R.drawable.ic_recommended)
            .addCustomAction("e", "e", R.drawable.ic_recommended)
            .addCustomAction("f", "e", R.drawable.ic_recommended)
            .addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    "b",
                    "b",
                    R.drawable.ic_album
                ).build()
            )
            .setState(state, 0, 0f)
    //            .setErrorMessage(
    //                PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED,
    //                "Authentication required"
    //            )
            .setExtras(extras)
            .build()
        return playbackState
    }

    override fun onDestroy() {
        NeuLog.e()
        session.release()

        // Free ExoPlayer resources.
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
     fun getAvailableActions(): Long {
        return PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_PLAY_FROM_URI or
                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
    }
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        NeuLog.e("$clientPackageName $clientUid isKnownCaller=$isKnownCaller")
        return if (isKnownCaller) {
            val rootExtras = Bundle().apply {
                putBoolean(MEDIA_SEARCH_SUPPORTED, true)
                putBoolean(CONTENT_STYLE_SUPPORTED, true)
                putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
            }
            BrowserRoot(TINGYU_BROWSABLE_ROOT, rootExtras)
        } else {
            null
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaItem>>) {
        // Assume for example that the music catalog is already loaded/cached.
        NeuLog.e("$parentId $result")
        val resultsSent = musicSource.whenReady {
            val children = browseTree[parentId]?.map { item ->
                MediaItem(item.description, item.flag)
            }
            if (children?.isEmpty() != false) {
                result.sendResult(null)
            } else {
                result.sendResult(children)
            }
        }
        if(!resultsSent){
            result.detach()
        }
    }
}

/**
 * Helper class to retrieve the the Metadata necessary for the ExoPlayer MediaSession connection
 * extension to call [MediaSessionCompat.setMetadata].
 */
private class XmlyQueueNavigator(
    mediaSession: MediaSessionCompat
) : TimelineQueueNavigator(mediaSession) {
    private val window = Timeline.Window()
    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
        player.currentTimeline
            .getWindow(windowIndex, window).tag as MediaDescriptionCompat
}


const val TINGYU_BROWSABLE_ROOT = "/"
const val TINGYU_EMPTY_ROOT = "@empty@"
const val TINGYU_HOME_ROOT = "__HOME__"
const val TINGYU_BROWSER_ROOT = "__BROWSER__"
const val TINGYU_RECENT_ROOT = "__RECENT__"
const val TINGYU_LIBRARY_ROOT = "__LIBRARY__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_DRAWABLE_ROOT_URI = "android.resource://bmw.ximalaya.test/drawable/"
const val RESOURCE_MIPMAP_ROOT_URI = "android.resource://bmw.ximalaya.test/mipmap/"
const val EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT = "android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT"
const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
const val CONTENT_STYLE_LIST = 1
const val CONTENT_STYLE_GRID = 2

const val EXTRA_IS_EXPLICIT = "android.media.IS_EXPLICIT"
const val EXTRA_IS_DOWNLOADED = "android.media.extra.DOWNLOAD_STATUS"
const val EXTRA_METADATA_ENABLED_VALUE:Long = 1
const val EXTRA_PLAY_COMPLETION_STATE = "android.media.extra.PLAYBACK_STATUS"
const val STATUS_NOT_PLAYED = 0
const val STATUS_PARTIALLY_PLAYED = 1
const val STATUS_FULLY_PLAYED = 2
const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px
private const val TAG = "MyMusicService"
private const val XMLY_USER_AGENT = "xmly.next"
