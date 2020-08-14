package bmw.ximalaya.test.extensions

import android.content.Context
import com.ximalaya.ting.android.opensdk.auth.utils.QrcodeLoginUtil
import com.ximalaya.ting.android.opensdk.constants.ConstantsOpenSdk
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack
import com.ximalaya.ting.android.opensdk.httputil.Config
import com.ximalaya.ting.android.opensdk.model.PlayableModel
import com.ximalaya.ting.android.opensdk.model.advertis.Advertis
import com.ximalaya.ting.android.opensdk.model.advertis.AdvertisList
import com.ximalaya.ting.android.opensdk.model.album.Album
import com.ximalaya.ting.android.opensdk.model.album.AlbumList
import com.ximalaya.ting.android.opensdk.model.category.Category
import com.ximalaya.ting.android.opensdk.model.category.CategoryList
import com.ximalaya.ting.android.opensdk.model.tag.TagList
import com.ximalaya.ting.android.opensdk.model.track.TrackList
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager
import com.ximalaya.ting.android.opensdk.player.advertis.IXmAdsStatusListener
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException
import java.util.concurrent.CompletableFuture

class XmlyMediaFactory(ctx: Context) : IXmPlayerStatusListener, IXmAdsStatusListener,
    XmPlayerManager.IConnectListener {
    private var albums: MutableList<Album>? = null
    private var categories: MutableList<Category>? = null
    val TAG = javaClass.simpleName
    private val mXimalaya: CommonRequest = CommonRequest.getInstanse()
    private val mXmPlayerManager: XmPlayerManager = XmPlayerManager.getInstance(ctx)

    init {
        val mAppKey = "caab9bab1978f96b55183615c07893da"
//        val mAppKey = BuildConfig.APP_KEY
      // val mAppKey = "cd20c7ad5c24ad42978f84384369d84a"
       val mPackId = "bmw.ximalaya.test"
//        val mPackId = BuildConfig.APPLICATION_ID
     //  val mPackId = "com.neusoft.alfus.nas.extra.cloud.fm"
       val mAppSecret = "f63f5bf2005278cefbbc248896e4b774"

     //  val mAppSecret = "286f08848f6cecd74a197b0cc1e26f08"
//        val mAppSecret = BuildConfig.APP_SECRET /
        mXimalaya.setAppkey(mAppKey)
        mXimalaya.setPackid(mPackId)
        mXimalaya.useHttps = true
        mXimalaya.init(ctx, mAppSecret)
//        mXimalaya.mNoSupportHttps.add("http://www.baidu.com/request")
//        mXimalaya.mNoSupportHttps.add("http://adse.ximalaya.com")

        val config: Config = Config()
        config.useProxy = false

        config.proxyHost = "172.30.50.10"
        config.proxyPort = 8080
        config.connectionTimeOut = 3000
        config.readTimeOut = 3000
        config.writeTimeOut = 3000
        CommonRequest.getInstanse().httpConfig = config
        mXmPlayerManager.init()
        mXmPlayerManager.addPlayerStatusListener(this)
        mXmPlayerManager.addAdsStatusListener(this)
        mXmPlayerManager.addOnConnectedListerner(this)
//        mXmPlayerManager.setPlayListChangeListener(this)

        ConstantsOpenSdk.isDebug = true

    /*    getCategories().whenComplete { t, u ->
            NeuLog.e("getCategories(${t})")
            if (t != null){
                categories = t.categories

                categories?.filter{
                    NeuLog.e("categories(${it.categoryName})")
                    it.categoryName == "音乐"
                }?.getOrNull(0)?.let {
                    getAlbumList("${it.id}").whenComplete { t, u ->
                        NeuLog.e("getAlbumList(${t})")
                        if(t !=null){
                            albums = t.albums
                            albums?.filter{
                                NeuLog.e("albums(${it.albumTitle})")
                                it.albumTitle == "经典老歌"
                            }?.getOrNull(0)?.let {
                                NeuLog.e(it.albumTitle)
                                getTracks("${it.id}").whenComplete { t,u ->
                                    NeuLog.e("getTracks($t)")
                                    mXmPlayerManager.playList(t.tracks,0)
                                }
                            }
                        }
                    }
                }
            }
        }*/
    }

    fun getTracks(albumId:String): CompletableFuture<TrackList> {
        val specificParams:MutableMap<String, String> = mutableMapOf(Pair(DTransferConstants.ALBUM_ID, albumId),Pair(DTransferConstants.SORT, "asc"), Pair(DTransferConstants.PAGE_SIZE, "10"))
        val future = CompletableFuture<TrackList>()
        (CommonRequest::getTracks)(specificParams, object: IDataCallBack<TrackList>{
            override fun onSuccess(p0: TrackList?) {
                future.complete(p0)
            }

            override fun onError(p0: Int, p1: String?) {
                future.completeExceptionally(Exception(p1))
            }
        })
        return future
    }

    fun getCategories(): CompletableFuture<CategoryList> {
        val specificParams:MutableMap<String, String> = mutableMapOf()
        val future = CompletableFuture<CategoryList>()
        (CommonRequest::getCategories)(specificParams, object: IDataCallBack<CategoryList>{
            override fun onSuccess(p0: CategoryList?) {
                future.complete(p0)
                NeuLog.e()
            }

            override fun onError(p0: Int, p1: String?) {
                future.completeExceptionally(Exception(p1))
                NeuLog.e()
            }
        })
        return future
    }
    fun getTags(specificParams:MutableMap<String, String>, callback: IDataCallBack<TagList>){
        (CommonRequest::getTags)(specificParams, callback)
    }
    fun getAlbumList(categoryId: String): CompletableFuture<AlbumList> {
        val specificParams:MutableMap<String, String> = mutableMapOf(Pair(DTransferConstants.CATEGORY_ID, categoryId),Pair(DTransferConstants.CALC_DIMENSION ,"1"),Pair(DTransferConstants.PAGE ,"1"),Pair(DTransferConstants.PAGE_SIZE ,"10"))
        val future = CompletableFuture<AlbumList>()
        (CommonRequest::getAlbumList)(specificParams, object: IDataCallBack<AlbumList>{
            override fun onSuccess(p0: AlbumList?) {
                future.complete(p0)
            }

            override fun onError(p0: Int, p1: String?) {
                future.completeExceptionally(Exception(p1))
            }
        })
        return future
    }

    fun requestGernerateQRCodeForLogin(specificParams:MutableMap<String, String>, callback: QrcodeLoginUtil.IGenerateCallBack){
        (QrcodeLoginUtil::requestGernerateQRCodeForLogin)(specificParams, callback)
        NeuLog.e()
    }

    override fun onPlayStart() {
        NeuLog.e()
    }

    override fun onSoundSwitch(p0: PlayableModel?, p1: PlayableModel?) {
        NeuLog.e()
    }

    override fun onPlayProgress(p0: Int, p1: Int) {
        NeuLog.e()
    }

    override fun onPlayPause() {
        NeuLog.e()
    }

    override fun onBufferProgress(p0: Int) {
        NeuLog.e()
    }

    override fun onPlayStop() {
        NeuLog.e()
    }

    override fun onBufferingStart() {
        NeuLog.e()
    }

    override fun onSoundPlayComplete() {
        NeuLog.e()
    }

    override fun onError(p0: XmPlayerException?): Boolean {
        NeuLog.e("($p0)")
        mXmPlayerManager.playNext()
        return true
    }

    override fun onSoundPrepared() {
        NeuLog.e()
    }

    override fun onBufferingStop() {
        NeuLog.e()
    }

    override fun onAdsStartBuffering() {
        NeuLog.e()
    }

    override fun onAdsStopBuffering() {
        NeuLog.e()
    }

    override fun onStartPlayAds(p0: Advertis?, p1: Int) {
        NeuLog.e()
    }

    override fun onStartGetAdsInfo() {
        NeuLog.e()
    }

    override fun onGetAdsInfo(p0: AdvertisList?) {
        NeuLog.e()
    }

    override fun onCompletePlayAds() {
        NeuLog.e()
    }

    override fun onError(p0: Int, p1: Int) {
        NeuLog.e()
    }

    override fun onConnected() {
        NeuLog.e()
    }
}