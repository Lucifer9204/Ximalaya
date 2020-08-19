package bmw.ximalaya.test.extensions

import android.content.Context
import com.ximalaya.ting.android.opensdk.constants.ConstantsOpenSdk
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack
import com.ximalaya.ting.android.opensdk.httputil.Config
import com.ximalaya.ting.android.opensdk.model.PostResponse
import com.ximalaya.ting.android.opensdk.model.album.AlbumList
import com.ximalaya.ting.android.opensdk.model.album.SubscribeAlbumList
import com.ximalaya.ting.android.opensdk.model.category.CategoryList
import com.ximalaya.ting.android.opensdk.model.live.radio.RadioList
import com.ximalaya.ting.android.opensdk.model.track.TrackList
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager
import java.util.concurrent.CompletableFuture

class XmlyMediaFactory(ctx: Context) {

    private val mXimalaya: CommonRequest = CommonRequest.getInstanse()
    private val mXmPlayerManager: XmPlayerManager = XmPlayerManager.getInstance(ctx)

    init {
        //BMW mAppKey
        //val mAppKey = "caab9bab1978f96b55183615c07893da"
        //Ximalaya Demo
        val mAppKey = "9f9ef8f10bebeaa83e71e62f935bede8"
//        val mAppKey = BuildConfig.APP_KEY
      // val mAppKey = "cd20c7ad5c24ad42978f84384369d84a"
        //BMW mPackId
      // val mPackId = "bmw.ximalaya.test"
        //Ximalaya Demo
        val mPackId = "com.app.test.android"
//        val mPackId = BuildConfig.APPLICATION_ID
     //  val mPackId = "com.neusoft.alfus.nas.extra.cloud.fm"
        //BMW mAppSecret
       //val mAppSecret = "f63f5bf2005278cefbbc248896e4b774"
        //Ximalaya Demo
        val mAppSecret = "8646d66d6abe2efd14f2891f9fd1c8af"

        mXimalaya.setAppkey(mAppKey)
        mXimalaya.setPackid(mPackId)
        mXimalaya.useHttps = true
        mXimalaya.init(ctx, mAppSecret)

        val config = Config()
        config.useProxy = false

        config.proxyHost = "172.30.50.10"
        config.proxyPort = 8080
        config.connectionTimeOut = 9000
        config.readTimeOut = 9000
        config.writeTimeOut = 9000
        CommonRequest.getInstanse().httpConfig = config
        mXmPlayerManager.init()

        ConstantsOpenSdk.isDebug = true
    }

    fun getTracks(albumId:String): CompletableFuture<TrackList> {
        val specificParams: MutableMap<String, String> = mutableMapOf(
            Pair(DTransferConstants.ALBUM_ID, albumId),
            Pair(DTransferConstants.SORT, "asc"),
            Pair(DTransferConstants.PAGE_SIZE, "2")
        )
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
    
     fun getRadios(): CompletableFuture<RadioList> {
        val specificParams:MutableMap<String, String> = mutableMapOf(Pair(DTransferConstants.RADIOTYPE, "3"))
        val future = CompletableFuture<RadioList>()
        (CommonRequest::getRadios)(specificParams, object: IDataCallBack<RadioList>{
            override fun onSuccess(p0: RadioList?) {
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
    fun getAlbumList(categoryId: String): CompletableFuture<AlbumList> {
        val specificParams: MutableMap<String, String> = mutableMapOf(
            Pair(DTransferConstants.CATEGORY_ID, categoryId),
            Pair(DTransferConstants.CALC_DIMENSION, "1"),
            Pair(DTransferConstants.PAGE, "1"),
            Pair(DTransferConstants.PAGE_SIZE, "3")
        )
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

    fun getAlbumByUid(uid: String):CompletableFuture<SubscribeAlbumList> {
        val specificParams: MutableMap<String, String> =
            mutableMapOf(Pair(DTransferConstants.UID, uid), Pair("updated_at", "0"))
        val future = CompletableFuture<SubscribeAlbumList>()
        (CommonRequest::getAlbumByUid)(specificParams, object: IDataCallBack<SubscribeAlbumList>{
            override fun onSuccess(p0: SubscribeAlbumList?) {
                NeuLog.e("getAlbumByUid onSuccess)")
                future.complete(p0)
            }

            override fun onError(p0: Int, p1: String?) {
                NeuLog.e("getAlbumByUid onError ${p1})")
                future.completeExceptionally(Exception(p1))
            }
        })
        return future
    }

    fun addOrDelSubscribe(uid: String, addOrDel: Int, albumId: String):CompletableFuture<PostResponse> {
        val specificParams: MutableMap<String, String> = mutableMapOf(
            Pair(DTransferConstants.UID, uid),
            Pair(DTransferConstants.ALBUM_ID, albumId),
            Pair("operation_type", addOrDel.toString())
        )
        val future = CompletableFuture<PostResponse>()
        (CommonRequest::AddOrDelSubscribe)(specificParams, object: IDataCallBack<PostResponse>{
            override fun onSuccess(p0: PostResponse?) {
                future.complete(p0)
            }

            override fun onError(p0: Int, p1: String?) {
                NeuLog.e("AddOrDelSubscribe onError ${p1})")
                future.completeExceptionally(Exception(p1))
            }
        })
        return future
    }

    fun isSubscribe(uid: String, albumId: String):CompletableFuture<Map<String ,Boolean>> {
        val specificParams: MutableMap<String, String> = mutableMapOf(
            Pair(DTransferConstants.UID, uid),
            Pair(DTransferConstants.ALBUM_ID, albumId)
        )
        val future = CompletableFuture<Map<String, Boolean>>()
        (CommonRequest::isSubscribe)(specificParams, object: IDataCallBack<Map<String ,Boolean>>{
            override fun onSuccess(p0: Map<String ,Boolean>?) {
                future.complete(p0)
            }

            override fun onError(p0: Int, p1: String?) {
                future.completeExceptionally(Exception(p1))
            }
        })
        return future
    }

}