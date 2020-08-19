package bmw.ximalaya.test.media


import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.IntDef
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.ximalaya.ting.android.opensdk.model.album.Album
import com.ximalaya.ting.android.opensdk.model.category.Category
import com.ximalaya.ting.android.opensdk.model.live.radio.Radio
import com.ximalaya.ting.android.opensdk.model.track.Track
import bmw.ximalaya.test.extensions.NeuLog
import bmw.ximalaya.test.extensions.XmlyMediaFactory
import com.ximalaya.ting.android.opensdk.datatrasfer.AccessTokenManager
import io.reactivex.disposables.Disposable
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

private const val TAG = "XmlyMusicSource"
private const val EXTRA_MEDIA_GENRE = MediaStore.EXTRA_MEDIA_GENRE

class XmlyMusicSource(ctx: Context) : Iterable<MediaMetadataCompat> {


    private var subscriber: Disposable? = null
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()
    private val glide by lazy { Glide.with(ctx) }
    private var catalog: List<MediaMetadataCompat> = emptyList()
    var categoriesList: List<MediaMetadataCompat> = emptyList()
    var categoryMap: List<MutableMap<String, List<MediaMetadataCompat>>> = emptyList()

    var albumBrowseMap: List<MutableMap<String, List<MediaMetadataCompat>>> = emptyList()
    var albumList: List<MediaMetadataCompat> = emptyList()
    var albumListTemp: List<Album> = emptyList()
    private var categoriesListTemp: List<Category> = emptyList()
    private var curPos: Int = 0
    private var curPosCategory: Int = 0
    var favoriteAlbumList: List<String> = emptyList()
    var currentAlbumId = ""

    var albumMap: List<MutableMap<String, List<MediaMetadataCompat>>> = emptyList()
    private val radioPic : String = "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=1250966952,2439095461&fm=26&gp=0.jpg"

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

    fun clearFavoriteAlbumList(){
        favoriteAlbumList = emptyList()
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

    fun load(xmlyMediaFactory: XmlyMediaFactory) {
        state = STATE_INITIALIZING

        getCategoryMediaMetadataCompats(xmlyMediaFactory)
        //getAlbumMediaMetadataCompats(xmlyMediaFactory)
        NeuLog.e("[Infor]getAllFavoriteAlbumId start")
        getAllFavoriteAlbumId(xmlyMediaFactory)
    }

    private class UpdateAlbumTask(
        val glide: RequestManager,
        val listener: (List<MediaMetadataCompat>) -> Unit
    ) : AsyncTask<List<Album>, Void, List<MediaMetadataCompat>>() {

        override fun doInBackground(vararg params: List<Album>): List<MediaMetadataCompat> {
            // NeuLog.e("getAlbumMediaMetadataCompats doInBackground")
            val mediaItems = ArrayList<MediaMetadataCompat>()
            params.forEach { albums ->
                mediaItems += albums.map { album ->
                    val image: String =
                        if (!album.coverUrlLarge.contains("https")) {
                            album.coverUrlLarge.replaceFirst("http", "https")
                        } else {
                            album.coverUrlLarge
                        }
                    val artUri = convertImageToUri(image, glide)

                    MediaMetadataCompat.Builder()
                        .from(album)
                        .apply {
                            albumArtUri = artUri
                        }
                        .build()
                }.toList()
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
            //NeuLog.e("getTracks(${albumTemp.coverUrlSmall})")
            return this
        }

        private fun convertImageToUri(image: String, glide: RequestManager): String {
            val artFile = glide
                .downloadOnly()
                .load(image)
                .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                .get()

            // Expose file via Local URI
            return artFile.asAlbumArtContentUri().toString()
        }
    }

    private class UpdateTrackTask(
        val glide: RequestManager,
        val listener: (List<MediaMetadataCompat>) -> Unit
    ) :
        AsyncTask<List<Track>, Void, List<MediaMetadataCompat>>() {
        override fun doInBackground(vararg params: List<Track>): List<MediaMetadataCompat> {
            val mediaItems = ArrayList<MediaMetadataCompat>()
            params.forEach { tracks ->
                mediaItems += tracks.map { track ->
                    // NeuLog.e("getTracks(${track.trackTitle})")
                    val image =
                        if (!track.coverUrlMiddle.contains("https")) {
                            track.coverUrlMiddle.replaceFirst("http", "https")
                        } else {
                            track.coverUrlMiddle
                        }
                    val artUri = convertImageToUri(image, glide)

                    MediaMetadataCompat.Builder()
                        .from(track)
                        .apply {
                            albumArtUri = artUri
                            displayIconUri = artUri
                        }
                        .build()
                }.toList()
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
            val httpsUrl = if (!track.downloadUrl.contains("https")) {
                track.downloadUrl.replaceFirst("http", "https")
            } else {
                track.downloadUrl
            }

            val durationMs = TimeUnit.SECONDS.toMillis(track.duration.toLong())
            id = track.dataId.toString()
            title = track.trackTitle
            flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

            artist = track.announcer.nickname
            album = track.album?.albumTitle
            duration = durationMs
            genre = "Electronic"
            mediaUri = httpsUrl
            //mediaUri = "https://live.xmcdn.com/live/1427/64.m3u8?transcode=ts"
            //mediaUri = "https://live.xmcdn.com/live/764/24.m3u8"
            //mediaUri = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/gear0/prog_index.m3u8"
            //albumArtUri = artUri
            trackNumber = track.orderNum.toLong()
            trackCount = 20
            displayTitle = track.trackTitle
            displaySubtitle = track.announcer.nickname
            displayDescription = track.album?.albumTitle
            //  displayIconUri = artUri
            downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
            return this
        }

        private fun convertImageToUri(image: String, glide: RequestManager): String {
            val artFile = glide
                .downloadOnly()
                .load(image)
                .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                .get()

            // Expose file via Local URI
            return artFile.asAlbumArtContentUri().toString()
        }
    }

    private class UpdateCategoryTask(
        val glide: RequestManager,
        val listener: (List<MediaMetadataCompat>) -> Unit
    ) :
        AsyncTask<List<Category>, Void, List<MediaMetadataCompat>>() {

        override fun doInBackground(vararg params: List<Category>): List<MediaMetadataCompat> {

            val mediaItems = ArrayList<MediaMetadataCompat>()
            params.forEach { categories ->
                mediaItems += categories.map { category ->
                    val image = if (!category.coverUrlMiddle.contains("https")) {
                        category.coverUrlMiddle.replaceFirst("http", "https")
                    } else {
                        category.coverUrlMiddle
                    }
                    val artUri = convertImageToUri(image, glide)
                    MediaMetadataCompat.Builder()
                        .from(category)
                        .apply {
                            albumArtUri = artUri
                        }
                        .build()
                }.toList()
            }
            return mediaItems
        }

        override fun onPostExecute(mediaItems: List<MediaMetadataCompat>) {
            super.onPostExecute(mediaItems)
            listener(mediaItems)
        }

        fun MediaMetadataCompat.Builder.from(categoryTemp: Category): MediaMetadataCompat.Builder {
            id = categoryTemp.id.toString()
            title = categoryTemp.categoryName
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            //    albumArtUri = artUri
            downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
            //  NeuLog.e("getTracks(${categoryTemp.coverUrlSmall})")

            return this
        }

        private fun convertImageToUri(image: String, glide: RequestManager): String {
            val artFile = glide
                .downloadOnly()
                .load(image)
                .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                .get()

            // Expose file via Local URI
            return artFile.asAlbumArtContentUri().toString()
        }
    }

    fun getAlbumMediaMetadataCompats(xmlyMediaFactory: XmlyMediaFactory): List<MediaMetadataCompat> {
        val mediaItems = ArrayList<MediaMetadataCompat>()
        // NeuLog.e("getAlbumMediaMetadataCompats")
        //   val artUriE = convertImageToUri("http://imagev2.xmcdn.com/group74/M08/F7/E5/wKgO3F6ZKlyTkqKqAAMEKEhOSIw777.jpg!op_type=5&upload_type=album&device_type=ios&name=mobile_small&magick=png")

        xmlyMediaFactory.getAlbumList("0").whenComplete { t, _ ->
            // NeuLog.e("getAlbumMediaMetadataCompats getAlbumList(${t})")
            if (t != null) {

                UpdateAlbumTask(glide) { mediaItems ->
                    // NeuLog.e("getAlbumMediaMetadataCompats  mediaItems ->")
                    albumList = mediaItems
                    //     catalog = mediaItems
                    //    state = STATE_INITIALIZED
                    NeuLog.e("mediaItems()")
                }.execute(t.albums)
                // NeuLog.e("getAlbumMediaMetadataCompats  mediaItems -> end")
                albumListTemp = t.albums
                if (albumListTemp.isNotEmpty()) {
                    getTracksRecursion(xmlyMediaFactory, albumListTemp[0].id.toString())

                }
            } else {
                state = STATE_ERROR
            }
        }

        // NeuLog.e("getAlbumMediaMetadataCompats return mediaItems")
        return mediaItems
    }

    fun getAllFavoriteAlbumId(xmlyPlayer: XmlyMediaFactory) {
        //TODO If not login return
        NeuLog.e("getAllFavoriteAlbumId")
        if (AccessTokenManager.getInstanse().uid != null && AccessTokenManager.getInstanse().uid.isNotEmpty()) {
            NeuLog.e("getAllFavoriteAlbumId uid is ${AccessTokenManager.getInstanse().uid})")
            xmlyPlayer.getAlbumByUid(AccessTokenManager.getInstanse().uid).whenComplete { t, _ ->
               // NeuLog.e("getAlbumByUid done(${t.albums})")
                for(item in t.albums){
                    favoriteAlbumList = favoriteAlbumList + item.id.toString()
                }
            }
        }
    }

    fun addOrDelSubscribe(xmlyPlayer: XmlyMediaFactory, addOrDel: Int) {
        //TODO If user not login return
        NeuLog.e("AddOrDelSubscribe opt[${addOrDel}]  0:delete 1:add")
        if(addOrDel != 0 && addOrDel != 1)return
        if(AccessTokenManager.getInstanse().uid != null && AccessTokenManager.getInstanse().uid.isNotEmpty()) {
            xmlyPlayer.addOrDelSubscribe(
                AccessTokenManager.getInstanse().uid,
                addOrDel,
                currentAlbumId
            ).whenComplete { _, _ ->
                NeuLog.e("AddOrDelSubscribe done")
            }
        }
    }

    private fun getAlbumRecursion(xmlyMediaFactory: XmlyMediaFactory, categoryId: String) {

        xmlyMediaFactory.getAlbumList(categoryId).whenComplete { t, _ ->
            var albumListTypeTemp: List<Album> = emptyList()
            if (categoryId == "0") {
                var radioAlbum = Album();
                radioAlbum.id = -100;
                radioAlbum.albumTitle = "电台"
                radioAlbum.coverUrlLarge = radioPic
                albumListTypeTemp += radioAlbum
                albumListTypeTemp += t.albums
            }
            else{
                albumListTypeTemp += t.albums
            }
            albumListTemp += t.albums
            //   NeuLog.e("getTracks(${t.tracks})")
            UpdateAlbumTask(glide) { mediaItems ->
                if (categoryId == "0") {
                    albumList += mediaItems
                }

                val mapItem: MutableMap<String, List<MediaMetadataCompat>> =
                    LinkedHashMap()

                mapItem.put(categoryId, mediaItems)
                categoryMap += mapItem

                curPosCategory++
                if (categoriesListTemp.size <= curPosCategory) {
                    //   state = STATE_INITIALIZED
                    curPosCategory = 0
                    if (albumListTemp.isNotEmpty()) {
                        getTracksRecursion(xmlyMediaFactory, albumListTemp[0].id.toString())
                    }
                    //    NeuLog.e("mediaItems()")
                } else {
                    getAlbumRecursion(
                        xmlyMediaFactory,
                        categoriesListTemp[curPosCategory].id.toString()
                    )
                }

            }.execute(albumListTypeTemp)
        }
    }


    private fun getTracksRecursion(xmlyMediaFactory: XmlyMediaFactory, albumId: String) {

        xmlyMediaFactory.getTracks(albumId).whenComplete { t, _ ->
            //NeuLog.e("getTracks(${t.tracks})")
            UpdateTrackTask(glide) { mediaItems ->
                //     albumList = mediaItems
                catalog += mediaItems
                val mapItem: MutableMap<String, List<MediaMetadataCompat>> =
                    LinkedHashMap()

                mapItem.put(albumId.toString(), mediaItems)
                albumMap += mapItem

                curPos++
                if (albumListTemp.size <= curPos) {
                    curPos = 0
                    //获取电台
                    xmlyMediaFactory.getRadios().whenComplete { t, u ->
                        UpdateRadioTask(glide) { mediaItems ->

                       //     albumList.add(0, radioAlbum)

                            //     albumList = mediaItems
                            catalog += mediaItems
                            val mapItem: MutableMap<String, List<MediaMetadataCompat>> =
                                LinkedHashMap()

                            mapItem.put("-100", mediaItems)
                            albumMap += mapItem

                            state = STATE_INITIALIZED
                        }.execute(t.radios)
                    }

                    NeuLog.e("mediaItems()")
                } else {
                    getTracksRecursion(xmlyMediaFactory, albumListTemp[curPos].id.toString())
                }

            }.execute(t.tracks)
        }
    }

    private class UpdateRadioTask(
        val glide: RequestManager,
        val listener: (List<MediaMetadataCompat>) -> Unit
    ) :
        AsyncTask<List<Radio>, Void, List<MediaMetadataCompat>>() {

        override fun doInBackground(vararg params: List<Radio>): List<MediaMetadataCompat> {

            val mediaItems = ArrayList<MediaMetadataCompat>()
            if (params != null) {

                params.forEach { radios ->

                    mediaItems += radios.map { radio ->

                 //       NeuLog.e("getTracks(${radio.radioName})")

                        var image: String = ""

                        if (!radio.coverUrlLarge.contains("https")) {
                            image = radio.coverUrlLarge.replaceFirst("http", "https")
                        } else {
                            image = radio.coverUrlLarge
                        }

                        //      LijhLog.e("getTracks---(${album.coverUrlMiddle})")
                        val artUri = convertImageToUri(image, glide)


//                        val art = glide.applyDefaultRequestOptions(glideOptions)
//                            .asBitmap()
//                            .load(image)
//                            .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
//                            .get()

                        MediaMetadataCompat.Builder()
                            .from(radio)
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

        fun MediaMetadataCompat.Builder.from(radio: Radio): MediaMetadataCompat.Builder {
            //     val image = track.coverUrlMiddle
            //   val artUri = convertImageToUri(image)
            var httpsUrl: String = ""

            if (!radio.rate24AacUrl.contains("https")) {
                httpsUrl = radio.rate24AacUrl.replaceFirst("http", "https")
            } else {
                httpsUrl = radio.rate24AacUrl
            }

            val durationMs = TimeUnit.SECONDS.toMillis(radio.endTime - radio.startTime)
            id = radio.dataId.toString()
            title = radio.radioName
            flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

            artist = radio.programName
            album = "电台"
            duration = durationMs
            genre = "Electronic"
            //     mediaUri = "https://live.xmcdn.com/live/764/24.m3u8"
            //       mediaUri = "https://live.xmcdn.com/live/1427/64.m3u8?transcode=ts"
            mediaUri = httpsUrl
            //    albumArtUri = artUri
            displayTitle = radio.radioName
            displaySubtitle = radio.programName
            displayDescription = radio.programName
            //  displayIconUri = artUri
            downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

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
    private fun convertImageToUri(image: String): String {
        val artFile = glide
            .downloadOnly()
            .load(image)
            .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
            .get()

        // Expose file via Local URI
        return artFile.asAlbumArtContentUri().toString()
    }

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

        // Allow it to be used in the typical builder style.
        return this
    }

    private fun getCategoryMediaMetadataCompats(xmlyMediaFactory: XmlyMediaFactory): List<MediaMetadataCompat> {
        val mediaItems = ArrayList<MediaMetadataCompat>()

        xmlyMediaFactory.getCategories().whenComplete { t, _ ->
            //NeuLog.e("getAlbumList(${t})")

            if (t != null) {

                categoriesListTemp += t.categories
                var category = Category()
                category.id = 0
                category.categoryName = "热门分类"
                category.coverUrlMiddle =
                    "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg"
                categoriesListTemp += category
                UpdateCategoryTask(glide) { mediaItems ->
                    categoriesList = mediaItems
                    //     catalog = mediaItems
                    // NeuLog.e("mediaItems()")
                }.execute(categoriesListTemp)

                if (categoriesListTemp.isNotEmpty()) {
                    getAlbumRecursion(xmlyMediaFactory, categoriesListTemp[0].id.toString())
                }

                //    albumList = mediaItems
                //     catalog = mediaItems
//                NeuLog.e("mediaItems()")
                //       state = STATE_INITIALIZED
            } else {
                state = STATE_ERROR
            }
        }

        return mediaItems
    }


    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    /**
     * Handles searching a MyMusicService from a focused voice search, often coming
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

}
