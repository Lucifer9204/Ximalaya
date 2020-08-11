package bmw.ximalaya.test.media

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat

class BrowseTree(context: Context, var musicSource: XmlyMusicSource) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>(
        Pair(
            NEU_BROWSABLE_ROOT, mutableListOf(
                MediaMetadataCompat.Builder().apply {
                    id = NEU_HOME_ROOT
                    title = "HOME"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_recommended)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build(),
                MediaMetadataCompat.Builder().apply {
                    id = NEU_BROWSER_ROOT
                    title = "BROWSER"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_browser)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build(),
                MediaMetadataCompat.Builder().apply {
                    id = NEU_RECENT_ROOT
                    title = "RECENT"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_recent)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build(),
                MediaMetadataCompat.Builder().apply {
                    id = NEU_LIBRARY_ROOT
                    title = "LIBRARY"
                    albumArtUri =
                        "${RESOURCE_DRAWABLE_ROOT_URI}${context.resources.getResourceEntryName(R.drawable.ic_library)}"
                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }.build()
            )
        ), Pair(NEU_HOME_ROOT, mutableListOf())
        , Pair(NEU_BROWSER_ROOT, mutableListOf())
        , Pair(NEU_RECENT_ROOT, mutableListOf())
        , Pair(NEU_LIBRARY_ROOT, mutableListOf())
    )

    operator fun get(key: String) = mediaIdToChildren[key]
    fun init() {
        musicSource.load()
        musicSource.whenReady {
            if(it){
                val sourceList = musicSource.map { it }
                this[NEU_BROWSER_ROOT]?.add(
                    MediaMetadataCompat.Builder().apply {
                        id = NEU_BROWSERLEVEL1_ROOT
                        title = "BROWSERLEVEL1"
                        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    }.build()
                )
               // mediaIdToChildren["id_jdlg"] = mutableListOf<MediaMetadataCompat>().apply{addAll(sourceList)}
                mediaIdToChildren[NEU_BROWSERLEVEL1_ROOT] = mutableListOf<MediaMetadataCompat>().apply{
                    this?.add(
                        MediaMetadataCompat.Builder().apply {
                            id = "id_jdlg"
                            title = "经典老歌"
                            albumArtUri = sourceList[0].albumArtUri.toString()
                            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                        }.build())
                }
                mediaIdToChildren["id_jdlg"] = mutableListOf<MediaMetadataCompat>().apply{addAll(sourceList)}
                this[NEU_HOME_ROOT]?.addAll(sourceList)
                this[NEU_RECENT_ROOT]?.addAll(sourceList)
                this[NEU_LIBRARY_ROOT]?.addAll(sourceList)
            }
        }
    }
}