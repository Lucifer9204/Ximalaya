package bmw.ximalaya.test

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

class XmlyMediaPlayer(ctx: Context) : IXmPlayerStatusListener, IXmAdsStatusListener,
    XmPlayerManager.IConnectListener {
    private var albums: MutableList<Album>? = null
    private var categories: MutableList<Category>? = null
    val TAG = javaClass.simpleName
    private val mXimalaya: CommonRequest = CommonRequest.getInstanse()
    private val mXmPlayerManager: XmPlayerManager = XmPlayerManager.getInstance(ctx)

    init {
        val mAppKey = "caab9bab1978f96b55183615c07893da"
//        val mAppKey = BuildConfig.APP_KEY
//        val mAppKey = "cd20c7ad5c24ad42978f84384369d84a"
        val mPackId = "bmw.ximalaya.test"
//        val mPackId = BuildConfig.APPLICATION_ID
//        val mPackId = "com.neusoft.alfus.nas.extra.cloud.fm"
        val mAppSecret = "f63f5bf2005278cefbbc248896e4b774"
//        val mAppSecret = BuildConfig.APP_SECRET
//        val mAppSecret = "286f08848f6cecd74a197b0cc1e26f08"
        mXimalaya.setAppkey(mAppKey)
        mXimalaya.setPackid(mPackId)
        mXimalaya.useHttps = true
        mXimalaya.init(ctx, mAppSecret)
//        mXimalaya.mNoSupportHttps.add("http://www.baidu.com/request")
//        mXimalaya.mNoSupportHttps.add("http://adse.ximalaya.com")

        val config: Config = Config()
        config.useProxy = true // 若想使用代理，必须配置此项为true，否则代理配置被忽略

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

        getCategories().whenComplete { t, u ->
            LijhLog.e("getCategories(${t})")
            if (t != null){
                categories = t.categories

                categories?.filter{
                    LijhLog.e("categories(${it.categoryName})")
                    it.categoryName == "音乐"
                }?.getOrNull(0)?.let {
                    getAlbumList("${it.id}").whenComplete { t, u ->
                        LijhLog.e("getAlbumList(${t})")
                        if(t !=null){
                            albums = t.albums
                            albums?.filter{
                                LijhLog.e("albums(${it.albumTitle})")
                                it.albumTitle == "经典老歌"
                            }?.getOrNull(0)?.let {
                                LijhLog.e(it.albumTitle)
                                getTracks("${it.id}").whenComplete { t,u ->
                                    LijhLog.e("getTracks($t)")
                                    mXmPlayerManager.playList(t.tracks,0)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getTracks(albumId:String): CompletableFuture<TrackList> {
        val specificParams:MutableMap<String, String> = mutableMapOf(Pair(DTransferConstants.ALBUM_ID, albumId),Pair(DTransferConstants.SORT, "asc"))
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
            }

            override fun onError(p0: Int, p1: String?) {
                future.completeExceptionally(Exception(p1))
            }
        })
        return future
    }
    fun getTags(specificParams:MutableMap<String, String>, callback: IDataCallBack<TagList>){
        (CommonRequest::getTags)(specificParams, callback)
    }
    fun getAlbumList(categoryId: String): CompletableFuture<AlbumList> {
        val specificParams:MutableMap<String, String> = mutableMapOf(Pair(DTransferConstants.CATEGORY_ID, categoryId),Pair(DTransferConstants.CALC_DIMENSION ,"1"))
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
    }

    override fun onPlayStart() {
        LijhLog.e()
    }

    override fun onSoundSwitch(p0: PlayableModel?, p1: PlayableModel?) {
        LijhLog.e()
    }

    override fun onPlayProgress(p0: Int, p1: Int) {
        LijhLog.e()
    }

    override fun onPlayPause() {
        LijhLog.e()
    }

    override fun onBufferProgress(p0: Int) {
        LijhLog.e()
    }

    override fun onPlayStop() {
        LijhLog.e()
    }

    override fun onBufferingStart() {
        LijhLog.e()
    }

    override fun onSoundPlayComplete() {
        LijhLog.e()
    }

    override fun onError(p0: XmPlayerException?): Boolean {
        LijhLog.e("($p0)")
//        mXmPlayerManager.playNext()
        return true
    }

    override fun onSoundPrepared() {
        LijhLog.e()
    }

    override fun onBufferingStop() {
        LijhLog.e()
    }

    override fun onAdsStartBuffering() {
        LijhLog.e()
    }

    override fun onAdsStopBuffering() {
        LijhLog.e()
    }

    override fun onStartPlayAds(p0: Advertis?, p1: Int) {
        LijhLog.e()
    }

    override fun onStartGetAdsInfo() {
        LijhLog.e()
    }

    override fun onGetAdsInfo(p0: AdvertisList?) {
        LijhLog.e()
    }

    override fun onCompletePlayAds() {
        LijhLog.e()
    }

    override fun onError(p0: Int, p1: Int) {
        LijhLog.e()
    }

    override fun onConnected() {
        LijhLog.e()
    }
}