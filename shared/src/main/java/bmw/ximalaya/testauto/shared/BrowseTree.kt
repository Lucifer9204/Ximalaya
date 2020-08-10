package bmw.ximalaya.testauto.shared

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat

class BrowseTree(context: Context, var musicSource: XmlyMusicSource) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>(
        Pair(
            TINGYU_BROWSABLE_ROOT, mutableListOf(
                MediaMetadataCompat.Builder().apply {
                    id = TINGYU_HOME_ROOT
                    title = "HOME"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_recommended)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build(),
                MediaMetadataCompat.Builder().apply {
                    id = TINGYU_BROWSER_ROOT
                    title = "BROWSER"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_browser)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build(),
                MediaMetadataCompat.Builder().apply {
                    id = TINGYU_RECENT_ROOT
                    title = "RECENT"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_recent)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build(),
                MediaMetadataCompat.Builder().apply {
                    id = TINGYU_LIBRARY_ROOT
                    title = "LIBRARY"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_library)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build()
            )
        ), Pair(TINGYU_HOME_ROOT, mutableListOf())
        , Pair(TINGYU_BROWSER_ROOT, mutableListOf())
        , Pair(TINGYU_RECENT_ROOT, mutableListOf())
        , Pair(TINGYU_LIBRARY_ROOT, mutableListOf())
    )

    operator fun get(key: String) = mediaIdToChildren[key]
    fun init() {
        musicSource.load()
        musicSource.whenReady {
            if(it){
                val sourceList = musicSource.map { it }
                this[TINGYU_BROWSER_ROOT]?.add(
                    MediaMetadataCompat.Builder().apply {
                        id = "id_jdlg"
                        title = "经典老歌"
                        artist = "周华健"
                        albumArtUri = sourceList[0].albumArtUri.toString()
                        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    }.build()
                )
                mediaIdToChildren["id_jdlg"] = mutableListOf<MediaMetadataCompat>().apply{addAll(sourceList)}
                this[TINGYU_HOME_ROOT]?.addAll(sourceList)
                this[TINGYU_RECENT_ROOT]?.addAll(sourceList)
                this[TINGYU_LIBRARY_ROOT]?.addAll(sourceList)
            }
        }
    }
}