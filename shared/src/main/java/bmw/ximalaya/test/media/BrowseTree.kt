package bmw.ximalaya.test.media


import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.bumptech.glide.Glide
import bmw.ximalaya.test.extensions.NeuLog
import bmw.ximalaya.test.extensions.XmlyMediaFactory

class BrowseTree(context: Context, var musicSource: XmlyMusicSource, var xmlyMediaFactory: XmlyMediaFactory) {

    private val glide by lazy { Glide.with(context) }
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
                    title = "FAVORITE"
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
        musicSource.load(xmlyMediaFactory)
        musicSource.whenReady {
            if(it){
                val sourceList = musicSource.map { it }
                this[TINGYU_BROWSER_ROOT]?.add(
                    MediaMetadataCompat.Builder().apply {
                        id = "id_jdlg"
                        title = "经典老歌"
                      //  artist = "周华健"
                        albumArtUri = sourceList[0].albumArtUri.toString()
                        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    }.build()
                )
                mediaIdToChildren["id_jdlg"] = mutableListOf<MediaMetadataCompat>().apply{
                    this?.add(
                        MediaMetadataCompat.Builder().apply {
                            id = "id_jdlgex"
                            title = "热门歌曲"
                            albumArtUri = sourceList[0].albumArtUri.toString()
                            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                        }.build()
                    )
                }
//                mediaIdToChildren["id_jdlg"] = mutableListOf<MediaMetadataCompat>().apply{addAll(sourceList)}
                mediaIdToChildren["id_jdlgex"] = mutableListOf<MediaMetadataCompat>().apply{
                    addAll(sourceList)
                }

                NeuLog.e("mediaItems(${musicSource.albumList})")
                this[TINGYU_HOME_ROOT]?.addAll(
                    musicSource.albumList
//                    getMediaMetadataCompats()
//                    MediaMetadataCompat.Builder().apply {
//                        id = "id_jdlgh"
//                        title = "经典老歌"
//                        //  artist = "周华健"
//                        albumArtUri = sourceList[0].albumArtUri.toString()
//                        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
//                    }.build()
                )

                for (album in musicSource.albumList)
                {
                    mediaIdToChildren[album.id.toString()] = mutableListOf<MediaMetadataCompat>().apply{
                        var blFind: Boolean = false
                        for (map in musicSource.albumMap)
                        {
                            for ((key,value) in map) {
                                if (key == album.id.toString())
                                {
                                    addAll(value)
                                    blFind = true
                                    break;
                                }
                            }
                            if (blFind)
                            {
                                break;
                            }
                        }
                    }
                }
//                mediaIdToChildren[musicSource.albumList[1].id.toString()] = mutableListOf<MediaMetadataCompat>().apply{
//                    addAll(sourceList)
//                }
         //       this[TINGYU_HOME_ROOT]?.addAll(sourceList)
                this[TINGYU_RECENT_ROOT]?.addAll(sourceList)
                this[TINGYU_LIBRARY_ROOT]?.addAll(sourceList)
            }
        }
    }


}