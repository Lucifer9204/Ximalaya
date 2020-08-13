package bmw.ximalaya.test.media


import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.IntDef
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.ximalaya.ting.android.opensdk.model.album.Album
import com.ximalaya.ting.android.opensdk.model.track.Track
import bmw.ximalaya.test.extensions.NeuLog
import bmw.ximalaya.test.extensions.XmlyMediaPlayer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

@IntDef(
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class State

const val STATE_CREATED = 1
const val STATE_INITIALIZING = 2
const val STATE_INITIALIZED = 3
const val STATE_ERROR = 4

class XmlyMusicSource(ctx:Context):Iterable<MediaMetadataCompat> {


    private var subscriber: Disposable? = null
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()
    private val glide by lazy { Glide.with(ctx) }
    private var catalog: List<MediaMetadataCompat> = emptyList()
    public var albumList: List<MediaMetadataCompat> = emptyList()

    @State
    var state: Int = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                    onReadyListeners.clear()
                }
            } else {
                field = value
            }
        }
    fun whenReady(performAction: (Boolean) -> Unit): Boolean =
        when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                onReadyListeners += performAction
                false
            }
            else -> {
                performAction(state != STATE_ERROR)
                true
            }
        }
    fun load(xmlyPlayer : XmlyMediaPlayer) {
//        subscriber?.apply {
//            if(!isDisposed){
//                dispose()
//            }
//        }
        state = STATE_INITIALIZING

        getAlbumMediaMetadataCompats(xmlyPlayer)


//        getAlbumMediaMetadataCompats(xmlyPlayer)

//        subscriber = Observable.create<List<MediaMetadataCompat>> {
//
//            it.onNext(
//           //     getAlbumMediaMetadataCompats(xmlyPlayer)
//           //     getMediaMetadataCompats(xmlyPlayer)
//
//
//                listOf(MediaMetadataCompat.Builder().apply {
//                val image =
//                    "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg"
//                val artUri = convertImageToUri(image)
//                val durationMs = TimeUnit.SECONDS.toMillis(90)
//                id = "wake_up_01"
//                title = "Intro - The Way Of Waking Up (feat. Alan Watts)"
//                flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
//
//                artist = "The Kyoto Connection"
//                album = "Wake Up"
//                duration = durationMs
//                genre = "Electronic"
//                mediaUri =
//                    "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3"
//                albumArtUri = artUri
//                trackNumber = 1
//                trackCount = 13
//                displayTitle = "Intro - The Way Of Waking Up (feat. Alan Watts)"
//                displaySubtitle = "The Kyoto Connection"
//                displayDescription = "Wake Up"
//                displayIconUri = artUri
//                downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
//            }.build())
//
//
//            )
//        }.subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe({
//                catalog = it
//         //       state = STATE_INITIALIZED
//            }, {
//                state = STATE_ERROR
//            })
    }

    private class UpdateAlbumTask(val glide: RequestManager, val listener: (List<MediaMetadataCompat>) -> Unit) :
        AsyncTask<List<Album>, Void, List<MediaMetadataCompat>>() {

        override fun doInBackground(vararg params: List<Album>): List<MediaMetadataCompat> {

            val mediaItems = ArrayList<MediaMetadataCompat>()
            if(params !=null) {

                params.forEach { albums ->

                    mediaItems += albums.map { album ->

                        val image = album.coverUrlLarge.replaceFirst("http", "https")

                  //      LijhLog.e("getTracks---(${album.coverUrlMiddle})")
                        val artUri = convertImageToUri(image, glide)


//                        val art = glide.applyDefaultRequestOptions(glideOptions)
//                            .asBitmap()
//                            .load(image)
//                            .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
//                            .get()

                        MediaMetadataCompat.Builder()
                            .from(album)
                            .apply {
                                albumArtUri = artUri
                            }
                            .build()
                    }.toList()
                }

            }

            return mediaItems
        }

        override fun onPostExecute(mediaItems: List<MediaMetadataCompat>) {
            super.onPostExecute(mediaItems)
            listener(mediaItems)
        }

        fun MediaMetadataCompat.Builder.from(albumTemp: Album): MediaMetadataCompat.Builder {

            //     val image = albumTemp.coverUrlSmall


            //    val artUri = convertImageToUri(image)
            id = albumTemp.id.toString()
            title = albumTemp.albumTitle
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            //    albumArtUri = artUri
            downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
            NeuLog.e("getTracks(${albumTemp.coverUrlSmall})")


            return this
        }

        private fun convertImageToUri(image: String, glide: RequestManager): String {
            val artFile = glide
                .downloadOnly()
                .load(image)
                .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                .get()

            // Expose file via Local URI
            val artUri = artFile.asAlbumArtContentUri().toString()
            return artUri
        }


    }

    private class UpdateTrackTask(val glide: RequestManager, val listener: (List<MediaMetadataCompat>) -> Unit) :
        AsyncTask<List<Track>, Void, List<MediaMetadataCompat>>() {

        override fun doInBackground(vararg params: List<Track>): List<MediaMetadataCompat> {

            val mediaItems = ArrayList<MediaMetadataCompat>()
            if(params !=null) {

                params.forEach { tracks ->

                    mediaItems += tracks.map { track ->

                        val image = track.coverUrlMiddle.replaceFirst("http", "https")

                        //      LijhLog.e("getTracks---(${album.coverUrlMiddle})")
                        val artUri = convertImageToUri(image, glide)


//                        val art = glide.applyDefaultRequestOptions(glideOptions)
//                            .asBitmap()
//                            .load(image)
//                            .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
//                            .get()

                        MediaMetadataCompat.Builder()
                            .from(track)
                            .apply {
                                albumArtUri = artUri
                                displayIconUri = artUri
                            }
                            .build()
                    }.toList()
                }

            }

            return mediaItems
        }

        override fun onPostExecute(mediaItems: List<MediaMetadataCompat>) {
            super.onPostExecute(mediaItems)
            listener(mediaItems)
        }

        fun MediaMetadataCompat.Builder.from(track: Track): MediaMetadataCompat.Builder {
       //     val image = track.coverUrlMiddle
         //   val artUri = convertImageToUri(image)
            NeuLog.e("getTracksdownloadUrl---(${track.downloadUrl})")

            val httpsUrl = track.downloadUrl.replaceFirst("http", "https")

            val durationMs = TimeUnit.SECONDS.toMillis(track.duration.toLong())
            id = track.dataId.toString()
            title = track.trackTitle
            flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

            artist = track.announcer.nickname
            album = track.album?.albumTitle
            duration = durationMs
            genre = "Electronic"
            mediaUri = httpsUrl
       //    albumArtUri = artUri
            trackNumber = track.orderNum.toLong()
            trackCount = 20
            displayTitle = track.trackTitle
            displaySubtitle = track.announcer.nickname
            displayDescription = track.album?.albumTitle
          //  displayIconUri = artUri
            downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED


            // The duration from the JSON is given in seconds, but the rest of the code works in
            // milliseconds. Here's where we convert to the proper units.
//        val durationMs = TimeUnit.SECONDS.toMillis(jsonMusic.duration)
//
//        id = jsonMusic.id
//        title = jsonMusic.title
//        artist = jsonMusic.artist
//        album = jsonMusic.album
//        duration = durationMs
//        genre = jsonMusic.genre
//        mediaUri = jsonMusic.source
//        albumArtUri = jsonMusic.image
//        trackNumber = jsonMusic.trackNumber
//        trackCount = jsonMusic.totalTrackCount
//        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
//
//        // To make things easier for *displaying* these, set the display properties as well.
//        displayTitle = jsonMusic.title
//        displaySubtitle = jsonMusic.artist
//        displayDescription = jsonMusic.album
//        displayIconUri = jsonMusic.image
//
//        // Add downloadStatus to force the creation of an "extras" bundle in the resulting
//        // MediaMetadataCompat object. This is needed to send accurate metadata to the
//        // media session during updates.
//        downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

            // Allow it to be used in the typical builder style.
            return this
        }

        private fun convertImageToUri(image: String, glide: RequestManager): String {
            val artFile = glide
                .downloadOnly()
                .load(image)
                .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                .get()

            // Expose file via Local URI
            val artUri = artFile.asAlbumArtContentUri().toString()
            return artUri
        }


    }

    fun getAlbumMediaMetadataCompats(xmlyPlayer : XmlyMediaPlayer): List<MediaMetadataCompat> {
        val mediaItems = ArrayList<MediaMetadataCompat>()

        //   val artUriE = convertImageToUri("http://imagev2.xmcdn.com/group74/M08/F7/E5/wKgO3F6ZKlyTkqKqAAMEKEhOSIw777.jpg!op_type=5&upload_type=album&device_type=ios&name=mobile_small&magick=png")

        xmlyPlayer.getAlbumList("0").whenComplete { t, u ->
            NeuLog.e("getAlbumList(${t})")
            if(t !=null){
//                t.albums?.filter{
//                    //     LijhLog.e("albums(${it.albumTitle})")
//                    it.albumTitle == "经典老歌"
//                }
//                mediaItems += t.albums.map { album ->
//
//                    MediaMetadataCompat.Builder()
//                        .from(album)
//                        .apply {
//                        }
//                        .build()
//                }.toList()

                UpdateAlbumTask(glide) { mediaItems ->
                    albumList = mediaItems
               //     catalog = mediaItems
                //    state = STATE_INITIALIZED
                    NeuLog.e("mediaItems()")
                }.execute(t.albums)

                for(album in t.albums)
                {
                    xmlyPlayer.getTracks("${album.id}").whenComplete { t,u ->
                        NeuLog.e("getTracks(${t.tracks})")
                            UpdateTrackTask(glide) { mediaItems ->
                           //     albumList = mediaItems
                                catalog = mediaItems
                                state = STATE_INITIALIZED
                                NeuLog.e("mediaItems()")
                            }.execute(t.tracks)
                        }
                    break;
                }



                //    albumList = mediaItems
               //     catalog = mediaItems
//                LijhLog.e("mediaItems()")
             //       state = STATE_INITIALIZED
            }else{
                   state = STATE_ERROR
            }
        }


        return mediaItems
    }

//    fun MediaMetadataCompat.Builder.from(albumTemp: Album): MediaMetadataCompat.Builder {
//
//        //     val image = albumTemp.coverUrlSmall
//
//
//        //    val artUri = convertImageToUri(image)
//        id = albumTemp.id.toString()
//        title = albumTemp.albumTitle
//        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
//        //    albumArtUri = artUri
//        downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
//        LijhLog.e("getTracks(${albumTemp.coverUrlSmall})")
//
//
//        return this
//    }


    private fun convertImageToUri(image: String): String {
        val artFile = glide
            .downloadOnly()
            .load(image)
            .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
            .get()

        // Expose file via Local URI
        val artUri = artFile.asAlbumArtContentUri().toString()
        return artUri
    }

//    fun getMediaMetadataCompats(xmlyPlayer : XmlyMediaPlayer): List<MediaMetadataCompat> {
//        val mediaItems = ArrayList<MediaMetadataCompat>()
//
//            mediaItems += xmlyPlayer.tracks.map { song ->
//
//
//                MediaMetadataCompat.Builder()
//                    .from(song)
//                    .apply {
//                    }
//                    .build()
//            }.toList()
//
//
//        return mediaItems
//    }

    fun MediaMetadataCompat.Builder.from(track: Track): MediaMetadataCompat.Builder {
                val image = track.coverUrlMiddle
                val artUri = convertImageToUri(image)
                val durationMs = TimeUnit.SECONDS.toMillis(track.duration.toLong())
                id = track.dataId.toString()
                title = track.trackTitle
                flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

                artist = track.announcer.nickname
                album = track.album?.albumTitle
                duration = durationMs
                genre = "Electronic"
                mediaUri = track.downloadUrl
                albumArtUri = artUri
                trackNumber = track.orderNum.toLong()
                trackCount = 20
                displayTitle = track.trackTitle
                displaySubtitle = track.announcer.nickname
                displayDescription = track.album?.albumTitle
                displayIconUri = artUri
                downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED


        // The duration from the JSON is given in seconds, but the rest of the code works in
        // milliseconds. Here's where we convert to the proper units.
//        val durationMs = TimeUnit.SECONDS.toMillis(jsonMusic.duration)
//
//        id = jsonMusic.id
//        title = jsonMusic.title
//        artist = jsonMusic.artist
//        album = jsonMusic.album
//        duration = durationMs
//        genre = jsonMusic.genre
//        mediaUri = jsonMusic.source
//        albumArtUri = jsonMusic.image
//        trackNumber = jsonMusic.trackNumber
//        trackCount = jsonMusic.totalTrackCount
//        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
//
//        // To make things easier for *displaying* these, set the display properties as well.
//        displayTitle = jsonMusic.title
//        displaySubtitle = jsonMusic.artist
//        displayDescription = jsonMusic.album
//        displayIconUri = jsonMusic.image
//
//        // Add downloadStatus to force the creation of an "extras" bundle in the resulting
//        // MediaMetadataCompat object. This is needed to send accurate metadata to the
//        // media session during updates.
//        downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

        // Allow it to be used in the typical builder style.
        return this
    }




    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    /**
     * Handles searching a [MusicSource] from a focused voice search, often coming
     * from the Google Assistant.
     */
    fun search(query: String, extras: Bundle): List<MediaMetadataCompat> {
        // First attempt to search with the "focus" that's provided in the extras.
        val focusSearchResult = when (extras[MediaStore.EXTRA_MEDIA_FOCUS]) {
            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                // For a Genre focused search, only genre is set.
                val genre = extras[EXTRA_MEDIA_GENRE]
                NeuLog.e(TAG, "Focused genre search: '$genre'")
                filter { song ->
                    song.genre == genre
                }
            }
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                // For an Artist focused search, only the artist is set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                NeuLog.e(TAG, "Focused artist search: '$artist'")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist)
                }
            }
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                // For an Album focused search, album and artist are set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                NeuLog.e(TAG, "Focused album search: album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                }
            }
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                // For a Song (aka Media) focused search, title, album, and artist are set.
                val title = extras[MediaStore.EXTRA_MEDIA_TITLE]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                NeuLog.e(TAG, "Focused media search: title='$title' album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                            && song.title == title
                }
            }
            else -> {
                // There isn't a focus, so no results yet.
                emptyList()
            }
        }

        // If there weren't any results from the focused search (or if there wasn't a focus
        // to begin with), try to find any matches given the 'query' provided, searching against
        // a few of the fields.
        // In this sample, we're just checking a few fields with the provided query, but in a
        // more complex app, more logic could be used to find fuzzy matches, etc...
        if (focusSearchResult.isEmpty()) {
            return if (query.isNotBlank()) {
                NeuLog.e(TAG, "Unfocused search for '$query'")
                filter { song ->
                    song.title.containsCaseInsensitive(query)
                            || song.genre.containsCaseInsensitive(query)
                }
            } else {
                // If the user asked to "play music", or something similar, the query will also
                // be blank. Given the small catalog of songs in the sample, just return them
                // all, shuffled, as something to play.
                NeuLog.e(TAG, "Unfocused search without keyword")
                return shuffled()
            }
        } else {
            return focusSearchResult
        }
    }

    /**
     * [MediaStore.EXTRA_MEDIA_GENRE] is missing on API 19. Hide this fact by using our
     * own version of it.
     */
    private val EXTRA_MEDIA_GENRE
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaStore.EXTRA_MEDIA_GENRE
        } else {
            "android.intent.extra.genre"
        }

}


private val glideOptions = RequestOptions()
    .fallback(R.drawable.default_art)
    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)


private const val TAG = "XmlyMusicSource"
