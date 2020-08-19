package bmw.ximalaya.test.media

import android.graphics.drawable.Drawable
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.MediaBrowserServiceCompat
import bmw.ximalaya.test.extensions.NeuLog
import bmw.ximalaya.test.extensions.XmlyMediaFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.RepeatModeActionProvider
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.ximalaya.ting.android.opensdk.datatrasfer.AccessTokenManager
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack
import com.ximalaya.ting.android.opensdk.model.user.XmBaseUserInfo


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

    val xmlyMediaFactory by lazy {
        XmlyMediaFactory(this)
    }

    protected lateinit var session: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector
    private val musicSource by lazy { XmlyMusicSource(this) }
    private val browseTree: BrowseTree by lazy {
        BrowseTree(applicationContext, musicSource, xmlyMediaFactory)
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
                    }
                }
                else -> {
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            var message = R.string.generic_error
            when (error.type) {
                // If the data from MediaSource object could not be loaded the Exoplayer raises
                // a type_source error.
                // An error message is printed to UI via Toast message to inform the user.
                ExoPlaybackException.TYPE_SOURCE -> {
                    message = R.string.error_media_not_found
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


    /**
     * Returns a list of [MediaItem]s that match the given search query
     */
    override fun onSearch(query: String, extras: Bundle?, result: Result<MutableList<MediaItem>>) {
        NeuLog.e(query)
        val resultsSent = musicSource.whenReady { successfullyInitialized ->
            if (successfullyInitialized) {
                val resultsList = musicSource.search(query, extras ?: Bundle.EMPTY)
                    .map { mediaMetadata ->
                        MediaItem(mediaMetadata.description, mediaMetadata.flag)
                    }
                result.sendResult(resultsList as MutableList<MediaItem>?)
            }
        }

        if (!resultsSent) {
            result.detach()
        }
    }

    fun isAlbumSelected(): Boolean {
        var ret = false
        for (item in musicSource.favoriteAlbumList) {
            if (musicSource.currentAlbumId == item) {
                ret = true
                break
            }
        }
        return ret
    }

    fun removeAlbumIdFromFavoriteAlbumList() {
        for (item in musicSource.favoriteAlbumList) {
            if (musicSource.currentAlbumId == item) {
                musicSource.favoriteAlbumList = musicSource.favoriteAlbumList - item
                break
            }
        }
    }

    fun addAlbumIdFromFavoriteAlbumList() {
        musicSource.favoriteAlbumList = musicSource.favoriteAlbumList + musicSource.currentAlbumId
    }

    override fun onCreate() {
        super.onCreate()
        NeuLog.e()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if(intent?.action == "CMD_NEU_LOGOUT"){
                    NeuLog.e("CMD_NEU_LOGOUT")
                    musicSource.clearFavoriteAlbumList()
                }else if(intent?.action == "CMD_NEU_LOGIN"){
                    NeuLog.e("CMD_NEU_LOGIN")
                    musicSource.getAllFavoriteAlbumId(xmlyMediaFactory)
                }
            }
        }
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, IntentFilter("CMD_NEU_LOGOUT"))
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiver, IntentFilter("CMD_NEU_LOGIN"))


AppDataStore.newInstance()
        if (AccessTokenManager.getInstanse().hasLogin()) {
            CommonRequest.getBaseUserInfo(mutableMapOf(), object : IDataCallBack<XmBaseUserInfo> {
                override fun onSuccess(p0: XmBaseUserInfo?) {
                    p0?.run {
                        AppDataStore.instance?.baseUserInfo = AppDataStore.BaseUserInfo(id, kind, nickName, avatarUrl, null, isVerified, isVip, vipExpiredAt)
                        Glide.with(this@MyMusicService).load(avatarUrl).into(object : CustomTarget<Drawable>() {
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                AppDataStore.instance?.baseUserInfo?.avatarCachedDrawable = resource
                            }

                        })
                    }
                }

                override fun onError(p0: Int, p1: String?) {
                }
            })
        }
        browseTree.init()

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        session = MediaSessionCompat(this, "MyMusicService")
        sessionToken = session.sessionToken
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
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
            connector.setCustomActionProviders(
                RepeatModeActionProvider(this@MyMusicService),
                object : MediaSessionConnector.CustomActionProvider {
                    override fun getCustomAction(player: Player): PlaybackStateCompat.CustomAction? {
                        val repeatBuilder = PlaybackStateCompat.CustomAction
                            .Builder("p15s", "-15s", R.drawable.ic_fast_rewind)
                        return repeatBuilder.build()
                    }

                    override fun onCustomAction(
                        player: Player,
                        controlDispatcher: ControlDispatcher,
                        action: String,
                        extras: Bundle?
                    ) {
                        NeuLog.e(action)
                        seekTo(goBackPosition())
                    }

                },
                object : MediaSessionConnector.CustomActionProvider {
                    override fun getCustomAction(player: Player): PlaybackStateCompat.CustomAction? {
                        val repeatBuilder = PlaybackStateCompat.CustomAction
                            .Builder("n15s", "+15s", R.drawable.ic_fast_forward)
                        return repeatBuilder.build()
                    }

                    override fun onCustomAction(
                        player: Player,
                        controlDispatcher: ControlDispatcher,
                        action: String,
                        extras: Bundle?
                    ) {
                        NeuLog.e(action)
                        seekTo(goAheadPosition())
                    }

                },
                object : MediaSessionConnector.CustomActionProvider {
                    override fun getCustomAction(player: Player): PlaybackStateCompat.CustomAction? {
                        NeuLog.e("musicSource.favoriteAlbumList:${musicSource.favoriteAlbumList}")
                        NeuLog.e("musicSource.currentAlbumId:${musicSource.currentAlbumId}")
                        var resource = R.drawable.ic_star_empty
                        if (isAlbumSelected()) {
                            resource = R.drawable.ic_star_filled
                        }
                        val favoriteBuilder = PlaybackStateCompat.CustomAction
                            .Builder("bmw.ximalaya.test.media.FAVORITE", "favorite", resource)
                        return favoriteBuilder.build()
                    }

                    override fun onCustomAction(
                        player: Player,
                        controlDispatcher: ControlDispatcher,
                        action: String,
                        extras: Bundle?
                    ) {
                        NeuLog.e("action:$action")
                        NeuLog.e("uid:${AccessTokenManager.getInstanse().uid}")
                        if (AccessTokenManager.getInstanse().uid == null || AccessTokenManager.getInstanse().uid.isEmpty()) {
                            Toast.makeText(
                                applicationContext,
                                "Failed to add favorite,user is not logged in",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            if (isAlbumSelected()) {
                                removeAlbumIdFromFavoriteAlbumList()
                                musicSource.addOrDelSubscribe(xmlyMediaFactory, 0)
                            } else {
                                addAlbumIdFromFavoriteAlbumList()
                                musicSource.addOrDelSubscribe(xmlyMediaFactory, 1)
                            }
                        }
                    }

                })

            connector.setQueueNavigator(XmlyQueueNavigator(session))
        }


        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
    }


    fun goAheadPosition(): Long {
        return exoPlayer.currentPosition + 15000
    }

    fun goBackPosition(): Long {
        NeuLog.e("curr=${exoPlayer.currentPosition}")
        return if (exoPlayer.currentPosition - 15000 < 0) {
            0
        } else
            return (exoPlayer.currentPosition - 15000)
    }

    /**
     * Seek to
     */
    fun seekTo(pos: Long) {
        exoPlayer.seekTo(pos)
    }

    override fun onDestroy() {
        NeuLog.e()
        session.release()

        // Free ExoPlayer resources.
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String, clientUid: Int, rootHints: Bundle?
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
        browseTree.updateFavoriteList()
        musicSource.currentAlbumId = parentId
        val resultsSent = musicSource.whenReady {
            val children = browseTree[parentId]?.map { item ->
                NeuLog.e("map item")
                MediaItem(item.description, item.flag)
            }
            if (children?.isEmpty()!=false) {
                result.sendResult(emptyList())
            } else {
                result.sendResult(children)
            }
        }
        if (!resultsSent) {
            result.detach()
        }
    }
}

/**
 * Helper class to retrieve the the Metadata necessary for the ExoPlayer MediaSession connection
 * extension to call [MediaSessionCompat.setMetadata].
 */
private class XmlyQueueNavigator(mediaSession: MediaSessionCompat) :
    TimelineQueueNavigator(mediaSession) {
    private val window = Timeline.Window()
    override fun getMediaDescription(player: Player, windowIndex: Int) =
        player.currentTimeline.getWindow(windowIndex, window).tag as MediaDescriptionCompat
}


const val TINGYU_BROWSABLE_ROOT = "/"
const val TINGYU_HOME_ROOT = "__HOME__"
const val TINGYU_BROWSER_ROOT = "__BROWSER__"
const val TINGYU_RECENT_ROOT = "__RECENT__"
const val TINGYU_LIBRARY_ROOT = "__LIBRARY__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_DRAWABLE_ROOT_URI = "android.resource://bmw.ximalaya.test/drawable/"
const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
const val CONTENT_STYLE_LIST = 1
const val CONTENT_STYLE_GRID = 2

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px
private const val TAG = "MyMusicService"
private const val XMLY_USER_AGENT = "xmly.next"
