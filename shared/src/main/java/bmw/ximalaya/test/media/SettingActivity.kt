package bmw.ximalaya.test.media

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import bmw.ximalaya.test.extensions.NeuLog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ximalaya.ting.android.opensdk.auth.model.XmlyAuth2AccessToken
import com.ximalaya.ting.android.opensdk.auth.utils.QrcodeLoginUtil
import com.ximalaya.ting.android.opensdk.datatrasfer.AccessTokenManager
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack
import com.ximalaya.ting.android.opensdk.datatrasfer.ILoginOutCallBack
import com.ximalaya.ting.android.opensdk.model.user.XmBaseUserInfo
import kotlinx.android.synthetic.main.activity_setting.*


class SettingActivity : AppCompatActivity() {

    private var mIsResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        setting_back.setOnClickListener { finish() }
        setting_logout_btn.setOnClickListener {
            NeuLog.e()
            AccessTokenManager.getInstanse().loginOut(object : ILoginOutCallBack{
                override fun onSuccess() {
                    val intent = Intent("CMD_NEU_LOGOUT")
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                    NeuLog.e("SETTING", "loginOut.accessToken: ${AccessTokenManager.getInstanse().accessToken}")
                    NeuLog.e("SETTING", "loginOut.hasLogin: ${AccessTokenManager.getInstanse().hasLogin()}")
                    NeuLog.e("SETTING", "loginOut.uid: ${AccessTokenManager.getInstanse().uid}")
                    setting_login.visibility = View.VISIBLE
                    setting_login_loading.visibility = View.GONE
                    setting_logout.visibility = View.INVISIBLE
                    setting_avatar.setImageResource(0)
                    setting_nickName.text = null
                    setting_vip.visibility = View.GONE
                    setting_userId.text = null
                    createQrcodeAndCheck()
                }

                override fun onFail(p0: Int, p1: String?) {
                    NeuLog.e("SETTING", "onFail: $p0, $p1")
                }
            })
        }
        if (AccessTokenManager.getInstanse().hasLogin()) {
            AccessTokenManager.getInstanse().tokenModel?.run {
                getUserInfo()
            }
        } else {
            updateUIState(UIMode.NOT_LOGIN, null, null, null)
            createQrcodeAndCheck()
        }
    }

    override fun onResume() {
        super.onResume()
        mIsResumed = true
    }

    override fun onPause() {
        super.onPause()
        mIsResumed = false
    }

    fun requestGernerateQRCodeForLogin(
        specificParams: MutableMap<String, String>,
        callback: QrcodeLoginUtil.IGenerateCallBack
    ) {
        (QrcodeLoginUtil::requestGernerateQRCodeForLogin)(specificParams, callback)
        NeuLog.e()
    }

    private var countDownTimer: CountDownTimer? = null
    private fun createQrcodeAndCheck() {
        QrcodeLoginUtil.requestGernerateQRCodeForLogin(
            mutableMapOf(Pair("size", "L")),
            object : QrcodeLoginUtil.IGenerateCallBack {
                override fun qrcodeImage(bitmap: Bitmap?, q: String) {
                    NeuLog.e("SETTING", "qrcodeImage: $bitmap, $q")
                    updateUIState(UIMode.NOT_LOGIN, bitmap, null, null)
                    countDownTimer?.cancel()

                    countDownTimer = object : CountDownTimer(Int.MAX_VALUE.toLong(), 3000) {
                        override fun onTick(millisUntilFinished: Long) {
                            QrcodeLoginUtil.checkQRCodeLoginStatus(
                                mutableMapOf(Pair("qrcode_id", q)),
                                object : IDataCallBack<XmlyAuth2AccessToken?> {
                                    override fun onSuccess(objects: XmlyAuth2AccessToken?) {
                                        NeuLog.e("onSuccess:${objects}")
                                        if (objects != null) {
                                            updateUIState(UIMode.LOGIN_BEGIN, null, null, null)
                                            AccessTokenManager.getInstanse().setAccessTokenAndUid(
                                                objects.token,
                                                objects.refreshToken,
                                                objects.expiresAt,
                                                objects.uid
                                            )
                                            getUserInfo()
                                            val intent = Intent("CMD_NEU_LOGIN")
                                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                                            countDownTimer?.cancel()

                                        }
                                    }

                                    override fun onError(
                                        code: Int,
                                        message: String
                                    ) {
                                        NeuLog.e("code:${code} message:${message}")
                                        if (code == 207) {
                                            NeuLog.e(TAG, "Client is not logged in")

                                        } else if (code == 217) {
                                            NeuLog.e(TAG, "QRCode is expired")
                                            countDownTimer?.cancel()
                                            createQrcodeAndCheck()
                                        }
                                    }
                                })
                        }

                        override fun onFinish() {}
                    }
                    countDownTimer?.start()
                }

                override fun onError(code: Int, message: String) {
                    NeuLog.e("SETTING", "onError: $code, $message")
                }
            })
    }
    private fun getUserInfo() {
        NeuLog.e("SETTING", "getUserInfo: ${AppDataStore.instance?.baseUserInfo}")
        AppDataStore.instance?.baseUserInfo?.let {
            updateUIState(UIMode.LOGIN_END, null, null, AppDataStore.instance?.baseUserInfo)
            return
        }
        CommonRequest.getBaseUserInfo(mutableMapOf(), object : IDataCallBack<XmBaseUserInfo> {
            override fun onSuccess(p0: XmBaseUserInfo?) {
                updateUIState(UIMode.LOGIN_END, null, p0, AppDataStore.instance?.baseUserInfo)
            }

            override fun onError(p0: Int, p1: String?) {
            }
        })
    }

    private fun updateUIState(uiMode: UIMode, qrCodeBitmap: Bitmap?, xmBaseUserInfo: XmBaseUserInfo?, baseUserInfo: AppDataStore.BaseUserInfo?) {
        NeuLog.e("SETTING", "updateUIState: $uiMode, $qrCodeBitmap, $xmBaseUserInfo, $baseUserInfo")
        !mIsResumed?:return
        when(uiMode) {
            UIMode.NOT_LOGIN -> {
                AppDataStore.instance?.baseUserInfo = null
                setting_login.visibility = View.VISIBLE
                setting_login_loading.visibility = View.GONE
                setting_logout.visibility = View.GONE
                setting_nickName.text = null
                setting_avatar.setImageResource(R.drawable.ic_launcher)
                setting_vip.visibility = View.GONE
                setting_userId.text = null
                qrCodeBitmap?.let {
                    setting_login_qrcode.setImageBitmap(it)
                }
            }
            UIMode.LOGIN_BEGIN -> {
                AppDataStore.instance?.baseUserInfo = null
                setting_login.visibility = View.VISIBLE
                setting_login_loading.visibility = View.VISIBLE
                setting_logout.visibility = View.GONE
                setting_nickName.text = null
                setting_avatar.setImageResource(R.drawable.ic_launcher)
                setting_vip.visibility = View.GONE
                setting_userId.text = null
            }
            UIMode.LOGIN_END -> {
                setting_login.visibility = View.GONE
                setting_login_loading.visibility = View.GONE
                setting_logout.visibility = View.VISIBLE
                baseUserInfo?.run {
                    NeuLog.e("SETTING", "updateUIState: 1")
                    if (avatarCachedDrawable == null && avatarUrl != null) {
                        Glide.with(this@SettingActivity).load(avatarUrl).into(object : CustomTarget<Drawable>() {
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                this@run.avatarCachedDrawable = resource
                               setting_avatar.setImageDrawable(resource)
                            }

                        })
                    }
                    setting_nickName.text = nickName
                    setting_vip.visibility = when{
                        isVip -> View.VISIBLE
                        else -> View.GONE
                    }
                    setting_userId.text = id.toString()
                    return
                }
                xmBaseUserInfo?.let {
                    AppDataStore.instance?.baseUserInfo = AppDataStore.BaseUserInfo(it.id, it.kind, it.nickName, it.avatarUrl, null, it.isVerified, it.isVip, it.vipExpiredAt)
                    Glide.with(this).load(it.avatarUrl).into(object : CustomTarget<Drawable>() {
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            AppDataStore.instance?.baseUserInfo?.avatarCachedDrawable = resource
                            setting_avatar.setImageDrawable(resource)
                        }

                    })
                    setting_nickName.text = it.nickName
                    setting_vip.visibility = when{
                        it.isVip -> View.VISIBLE
                        else -> View.GONE
                    }
                    setting_userId.text = it.id.toString()
                }
            }
        }
    }

    private enum class UIMode {
        NOT_LOGIN, LOGIN_BEGIN, LOGIN_END
    }
}

private const val TAG = "SETTING"