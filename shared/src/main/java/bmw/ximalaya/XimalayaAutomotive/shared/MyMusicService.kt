package bmw.ximalaya.test.shared

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.bumptech.glide.Glide
import bmw.ximalaya.test.LijhLog
import bmw.ximalaya.test.XmlyMediaPlayer
import kotlin.random.Random


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

    private lateinit var session: MediaSessionCompat
    private val xmlyPlayer by lazy {
        XmlyMediaPlayer(this)
    }
    private lateinit var packageValidator: PackageValidator
    private val callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            LijhLog.e()
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0f)
                .build()

            session.setPlaybackState(playbackState)
        }

        override fun onSkipToQueueItem(queueId: Long) {
            LijhLog.e()
        }

        override fun onSeekTo(position: Long) {
            LijhLog.e()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            LijhLog.e("$mediaId")
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0f)
                .build()

            session.setPlaybackState(playbackState)
        }

        override fun onPause() {
            LijhLog.e()
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0f)
                .build()

            session.setPlaybackState(playbackState)
        }

        override fun onStop() {
            LijhLog.e()
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0f)
                .build()

            session.setPlaybackState(playbackState)
        }

        override fun onSkipToNext() {
            LijhLog.e()
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0f)
                .build()

            session.setPlaybackState(playbackState)
        }

        override fun onSkipToPrevious() {
            LijhLog.e()
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 0f)
                .build()

            session.setPlaybackState(playbackState)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            LijhLog.e()
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            LijhLog.e()
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0f)
                .build()

            session.setPlaybackState(playbackState)
        }
    }

    override fun onSearch(query: String, extras: Bundle?, result: Result<MutableList<MediaItem>>) {
        LijhLog.e("$query")
//        super.onSearch(query, extras, result)
        if(query == "home"){
            val mediaItems = mutableListOf<MediaItem>()
            createLibrary(mediaItems)

            result.sendResult(mediaItems)
        } else {
            result.detach()
        }
    }
    val workHandler = Handler(HandlerThread("worker_thread").also { it.start() }.looper)
    var bitmap :Bitmap? = null
    override fun onCreate() {
        super.onCreate()
        LijhLog.e()
        xmlyPlayer
        workHandler.post {
            LijhLog.e("bitmap = $bitmap")
            bitmap = Glide.with(this).asBitmap()
                .load(Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg"))
                .submit().get()
            LijhLog.e("bitmap = $bitmap")
        }
        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        session = MediaSessionCompat(this, "MyMusicService")
        sessionToken = session.sessionToken
        session.setCallback(callback)
        session.setFlags(
            MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        val extras = Bundle().apply {
//            putString(
//                "android.media.extras.ERROR_RESOLUTION_ACTION_LABEL",
//                "Sign in"
//            )
//            putParcelable(
//                "android.media.extras.ERROR_RESOLUTION_ACTION_INTENT",
//                signInActivityPendingIntent
//            )
        }
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(getAvailableActions() or PlaybackStateCompat.ACTION_PLAY)
            .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0f)
//            .setErrorMessage(
//                PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED,
//                "Authentication required"
//            )
            .setExtras(extras)
            .build()

        session.setPlaybackState(playbackState)
        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
    }

    override fun onDestroy() {
        LijhLog.e()
        session.release()
    }
    private fun getAvailableActions(): Long {
        return PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
    }
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        LijhLog.e("$clientPackageName $clientUid isKnownCaller=$isKnownCaller")
        return if (isKnownCaller) {
            val rootExtras = Bundle().apply {
                putBoolean(MEDIA_SEARCH_SUPPORTED, true)
                putBoolean(CONTENT_STYLE_SUPPORTED, true)
                putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
            }
            BrowserRoot(MY_MEDIA_ROOT_ID, rootExtras)
        } else {
            null
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
        // Assume for example that the music catalog is already loaded/cached.
        LijhLog.e("$parentId $result")
        val mediaItems: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()

        // Check if this is the root menu:
        when{
            parentId.startsWith("category/")->{
                createAlbum(parentId.substring("category/".length), result)
            }
        }
        when(parentId){
            MY_MEDIA_ROOT_ID->{
                createTitle(result)
//                createCategory(result)
            }
            "home"-> {
                createHome(mediaItems)
                result.sendResult(mediaItems)
            }
            "browser"-> {
                createBrowser(mediaItems)
                result.sendResult(mediaItems)
            }
            "library"->{
                createLibrary(mediaItems)
                result.sendResult(mediaItems)
            }
            else->{
                result.detach()
            }
        }


    }

    private fun createAlbum(
        categoryId: String,
        result: Result<MutableList<MediaItem>>
    ) {
        val categoryFuture = xmlyPlayer.getCategories()
        categoryFuture.whenComplete { t, u ->
            if(u==null){
                result.sendResult(t.categories.also { it.sortBy { it.id } }.map {
                        category->
                    MediaItem(
                        MediaMetadataCompat.Builder().also {
                            it.id = "category/${category.id}"
                            it.title = category.categoryName
//                            it.artist = jsonMusic.artist
//                            it.album = jsonMusic.album
//                            it.duration = durationMs
//                            it.genre = jsonMusic.genre
//                            it.mediaUri = jsonMusic.source
//                            it.albumArtUri = jsonMusic.image
//                            it.trackNumber = jsonMusic.trackNumber
//                            it.trackCount = jsonMusic.totalTrackCount

                            // To make things easier for *displaying* these, set the display properties as well.
                            it.displayTitle = category.categoryName
//                            it.displaySubtitle = jsonMusic.artist
//                            it.displayDescription = jsonMusic.album
//                            it.displayIconUri = jsonMusic.image

                            // Add downloadStatus to force the creation of an "extras" bundle in the resulting
                            // MediaMetadataCompat object. This is needed to send accurate metadata to the
                            // media session during updates.
                            it.downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
                        }.build().description, MediaItem.FLAG_BROWSABLE
                    )
                }.toMutableList())
            } else {
                result.sendResult(null)
            }
        }
        result.detach()
    }

    private fun createCategory(result: Result<MutableList<MediaItem>>) {
        val categoryFuture = xmlyPlayer.getCategories()
        categoryFuture.whenComplete { t, u ->
            if(u==null){
                result.sendResult(t.categories.also { it.sortBy { it.id } }.map {
                    category->
                    MediaItem(
                        MediaMetadataCompat.Builder().also {
                            it.id = "category/${category.id}"
                            it.title = category.categoryName
//                            it.artist = jsonMusic.artist
//                            it.album = jsonMusic.album
//                            it.duration = durationMs
//                            it.genre = jsonMusic.genre
//                            it.mediaUri = jsonMusic.source
//                            it.albumArtUri = jsonMusic.image
//                            it.trackNumber = jsonMusic.trackNumber
//                            it.trackCount = jsonMusic.totalTrackCount

                            // To make things easier for *displaying* these, set the display properties as well.
                            it.displayTitle = category.categoryName
//                            it.displaySubtitle = jsonMusic.artist
//                            it.displayDescription = jsonMusic.album
//                            it.displayIconUri = jsonMusic.image

                            // Add downloadStatus to force the creation of an "extras" bundle in the resulting
                            // MediaMetadataCompat object. This is needed to send accurate metadata to the
                            // media session during updates.
                            it.downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
                        }.build().description, MediaItem.FLAG_BROWSABLE
                    )
                }.toMutableList())
            } else {
                result.sendResult(null)
            }
        }
        result.detach()

    }

    private fun createLibrary(mediaItems: MutableList<MediaBrowserCompat.MediaItem>) {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId("mediaId000")
        mediaDescriptionBuilder.setTitle("my title")
        mediaDescriptionBuilder.setIconUri(Uri.parse("https://designguidelines.withgoogle.com/automotive-os-apps/assets/1t-DrgqdrZsvlUMrDCymGXu7ccnvI8U3E/primary-navigation-tabs.gif"))
        mediaDescriptionBuilder.setMediaUri(Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3"))
        val extras = Bundle()
        extras.putLong(EXTRA_IS_EXPLICIT, 1)
        extras.putInt(EXTRA_PLAY_COMPLETION_STATE, Random.nextInt(0,3))
        mediaDescriptionBuilder.setExtras(extras)
        val mediaItem = MediaItem(
            mediaDescriptionBuilder.build(), MediaItem.FLAG_PLAYABLE/* playable or browsable flag*/
        )
        mediaItems.add(mediaItem)
        mediaItems.add(
            createMediaItem(
                "radio",
                "Radio",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createMediaItem(
                "recommend",
                "Recommend",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createMediaItem(
                "novel",
                "Novel",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createMediaItem(
                "music",
                "Music",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
    }

    private fun createBrowser(mediaItems: MutableList<MediaItem>) {
        mediaItems.add(
            createMediaItem(
                "radio",
                "Radio",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createMediaItem(
                "recommend",
                "Recommend",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createMediaItem(
                "novel",
                "Novel",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createMediaItem(
                "music",
                "Music",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
    }

    private fun createHome(mediaItems: MutableList<MediaItem>) {
        mediaItems.add(
            createBrowsableMediaItem(
                "radio",
                "Radio",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createBrowsableMediaItem(
                "recommend",
                "Recommend",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createBrowsableMediaItem(
                "novel",
                "Novel",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createBrowsableMediaItem(
                "music",
                "Music",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
    }

    private fun createTitle(result: Result<MutableList<MediaItem>>) {
        val mediaItems: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()
        mediaItems.add(
            createBrowsableMediaItem(
                "home",
                "HOME",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createBrowsableMediaItem(
                "browser",
                "BROWSER",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createBrowsableMediaItem(
                "recent",
                "RECENT",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        mediaItems.add(
            createBrowsableMediaItem(
                "library",
                "LIBRARY",
                Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg")
            )
        )
        result.sendResult(mediaItems)
    }

    private fun createMediaItem(
        mediaId: String, title: String, iconUri: Uri,group: String?=null
    ): MediaItem {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(mediaId)
        mediaDescriptionBuilder.setTitle(title)
        mediaDescriptionBuilder.setIconUri(iconUri)
//        mediaDescriptionBuilder.setMediaUri(iconUri)

        bitmap?.let{
            LijhLog.e()
            mediaDescriptionBuilder.setIconBitmap(it)
        }
        val extras = Bundle()
        extras.putLong(EXTRA_IS_EXPLICIT, 1)
        extras.putInt(EXTRA_PLAY_COMPLETION_STATE, Random.nextInt(0,3))

        group?.let{extras.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, it)}
//        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
        mediaDescriptionBuilder.setExtras(extras)
        val builder = MediaMetadataCompat.Builder()
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,mediaId)
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,title)
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,title)
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,title)
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3").toString())
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg").toString())
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg").toString())
        bitmap?.let {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
        }
        builder.displayTitle = title
        builder.displaySubtitle = title
        builder.displayDescription = title
        builder.displayIconUri = "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg"
        return MediaItem(
            builder.build().description, MediaItem.FLAG_PLAYABLE/* playable or browsable flag*/
        )
    }

    private fun createBrowsableMediaItem(
        mediaId: String, folderName: String, iconUri: Uri,group:String?=null
    ): MediaItem {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(mediaId)
        mediaDescriptionBuilder.setTitle(folderName)
        mediaDescriptionBuilder.setIconUri(iconUri)
        val extras = Bundle()
        group?.let{extras.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, it)}
//        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
//        extras.putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
        mediaDescriptionBuilder.setExtras(extras)
//        return MediaItem(
//            mediaDescriptionBuilder.build(), MediaItem.FLAG_BROWSABLE
//        )
        val builder = MediaMetadataCompat.Builder()
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,mediaId)
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,folderName)
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,folderName)
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,folderName)
        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3").toString())
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg").toString())
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,Uri.parse("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg").toString())
        bitmap?.let {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
        }
        builder.displayTitle = folderName
        builder.displaySubtitle = folderName
        builder.displayDescription = folderName
        builder.displayIconUri = "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg"
        return MediaItem(
            builder.build().description, MediaItem.FLAG_BROWSABLE/* playable or browsable flag*/
        )
    }

}

const val MY_MEDIA_ROOT_ID = "bmw.ximalaya.test"
const val UAMP_BROWSABLE_ROOT = "/"
const val UAMP_EMPTY_ROOT = "@empty@"
const val UAMP_RECOMMENDED_ROOT = "__RECOMMENDED__"
const val UAMP_ALBUMS_ROOT = "__ALBUMS__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.example.android.uamp.next/drawable/"
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
