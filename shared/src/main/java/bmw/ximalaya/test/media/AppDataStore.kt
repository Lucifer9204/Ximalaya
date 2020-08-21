package bmw.ximalaya.test.media

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

class AppDataStore {
    var qrCodeBitmap: Bitmap? = null
    var baseUserInfo: BaseUserInfo? = null

    companion object {
        private var mAppDataStore: AppDataStore? = null
        val instance = mAppDataStore
        fun newInstance() {
            mAppDataStore = AppDataStore()
        }
    }

    class BaseUserInfo(
        var id: Int,
        var kind: String?,
        var nickName: String?,
        var avatarUrl: String?,
        var avatarCachedDrawable: Drawable?,
        var isVerified: String?,
        var isVip: Boolean,
        var vipExpiredAt: Long
    )
}