package bmw.ximalaya.test.media

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.IntDef
import bmw.ximalaya.test.extensions.NeuLog
import bmw.ximalaya.test.extensions.XmlyMediaPlayer
import com.bumptech.glide.Glide
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
    fun load() {
        subscriber?.apply {
            if(!isDisposed){
                dispose()
            }
        }
        state = STATE_INITIALIZING

        subscriber = Observable.create<List<MediaMetadataCompat>> {
//            xmlyPlayer.getCategories().whenComplete { t, u ->
//                it.onNext(t.categories.map {
//                    category->
//                    MediaMetadataCompat.Builder().apply {
//                    id = "${category.id}"
//                        NeuLog.e()
//                        title = "${ category.categoryName }"
//                        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
//                      //  val image = category.coverUrlSmall
//                       // val artUri = convertImageToUri(image)
//                        NeuLog.e()
//                }.build()})
//            }

            it.onNext(
                listOf(MediaMetadataCompat.Builder().apply {
                val image =
                    "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg"
                val artUri = convertImageToUri(image)
                val durationMs = TimeUnit.SECONDS.toMillis(90)
                id = "wake_up_01"
                title = "Intro - The Way Of Waking Up (feat. Alan Watts)"
                flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

                artist = "The Kyoto Connection"
                album = "Wake Up"
                duration = durationMs
                genre = "Electronic"
                mediaUri =
                    "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3"
                albumArtUri = artUri
                trackNumber = 1
                trackCount = 13
                displayTitle = "Intro - The Way Of Waking Up (feat. Alan Watts)"
                displaySubtitle = "The Kyoto Connection"
                displayDescription = "Wake Up"
                displayIconUri = artUri
                downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
            }.build())
            )
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                catalog = it
                state = STATE_INITIALIZED
            }, {
                state = STATE_ERROR
            })
    }

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
                NeuLog.d(TAG, "Focused genre search: '$genre'")
                filter { song ->
                    song.genre == genre
                }
            }
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                // For an Artist focused search, only the artist is set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                NeuLog.d(TAG, "Focused artist search: '$artist'")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist)
                }
            }
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                // For an Album focused search, album and artist are set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                NeuLog.d(TAG, "Focused album search: album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                }
            }
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                // For a Song (aka Media) focused search, title, album, and artist are set.
                val title = extras[MediaStore.EXTRA_MEDIA_TITLE]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                NeuLog.d(TAG, "Focused media search: title='$title' album='$album' artist='$artist")
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
                NeuLog.d(TAG, "Unfocused search for '$query'")
                filter { song ->
                    song.title.containsCaseInsensitive(query)
                            || song.genre.containsCaseInsensitive(query)
                }
            } else {
                // If the user asked to "play music", or something similar, the query will also
                // be blank. Given the small catalog of songs in the sample, just return them
                // all, shuffled, as something to play.
                NeuLog.d(TAG, "Unfocused search without keyword")
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

private const val TAG = "XmlyMusicSource"




