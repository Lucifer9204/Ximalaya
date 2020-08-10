package bmw.ximalaya.testauto.shared

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.IntDef
import com.bumptech.glide.Glide
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

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
            it.onNext(listOf(MediaMetadataCompat.Builder().apply {
                val image =
                    "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg"
                val artUri = convertImageToUri(image)
                id = "wake_up_01"
                title = "Intro - The Way Of Waking Up (feat. Alan Watts)"
                flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

                artist = "The Kyoto Connection"
                album = "Wake Up"
                duration = 90
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
            }.build()))
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
}