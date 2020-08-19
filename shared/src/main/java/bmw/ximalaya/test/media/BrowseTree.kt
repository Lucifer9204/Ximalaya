package bmw.ximalaya.test.media


import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import bmw.ximalaya.test.extensions.NeuLog
import bmw.ximalaya.test.extensions.XmlyMediaFactory

class BrowseTree(
    context: Context,
    private var musicSource: XmlyMusicSource,
    private var xmlyMediaFactory: XmlyMediaFactory
) {

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
            if (it) {
                val sourceList = musicSource
                this[TINGYU_BROWSER_ROOT]?.addAll(
                    musicSource.categoriesList.subList(0, musicSource.categoriesList.size - 1)
                )
                musicSource.categoriesList.filter {category->category.id.toString() != "0"}
                    .forEach { category ->
                        mediaIdToChildren[category.id.toString()] =
                            mutableListOf<MediaMetadataCompat>().apply {
                                musicSource.categoryMap.forEach {categoryMap->
                                    categoryMap[category.id.toString()]?.let{categories->
                                        addAll(categories)
                                    }
                                }
                            }
                    }
                musicSource.albumListTemp.forEach {album->
                    mediaIdToChildren[album.id.toString()] =
                        mutableListOf<MediaMetadataCompat>().apply {
                            musicSource.albumMap.forEach {
                                it[album.id.toString()]?.let {
                                    addAll(it)
                                }
                            }
                        }
                }

                NeuLog.e("mediaItems(${musicSource.albumList})")
                this[TINGYU_HOME_ROOT]?.addAll(musicSource.albumList)

                this[TINGYU_RECENT_ROOT]?.addAll(sourceList)
                updateFavoriteList()
            }
        }
    }

    fun updateFavoriteList(){
        //TODO Need to do the sync with getAblumByUid when back startup 
        musicSource.whenReady {
            if (it) {
                NeuLog.e("updateFavoriteList")
                this[TINGYU_LIBRARY_ROOT]?.clear()
                for(item in musicSource.albumList){
                    for(favItem in musicSource.favoriteAlbumList){
                        if(favItem == item.id.toString()) {
                            NeuLog.e("updateFavoriteList add success albumid[${favItem}]")
                            this[TINGYU_LIBRARY_ROOT]?.add(
                                MediaMetadataCompat.Builder().apply {
                                    id = item.id.toString()
                                    title = item.title
                                    artist = item.albumArtist
                                    albumArtUri = Uri.parse(item.description.iconUri.toString()).toString()
                                    displayIconUri = Uri.parse(item.description.iconUri.toString()).toString()
                                    flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                                }.build()
                            )
                            break
                        }
                    }
                }
            }
        }
    }

}