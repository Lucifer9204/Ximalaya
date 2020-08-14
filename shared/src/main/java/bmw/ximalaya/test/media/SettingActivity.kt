package bmw.ximalaya.test.media

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.HandlerThread
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import bmw.ximalaya.test.extensions.NeuLog
import com.bumptech.glide.Glide
import com.ximalaya.ting.android.opensdk.auth.model.XmlyAuth2AccessToken
import com.ximalaya.ting.android.opensdk.auth.utils.QrcodeLoginUtil
import com.ximalaya.ting.android.opensdk.datatrasfer.AccessTokenManager
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack
import kotlinx.android.synthetic.main.activity_setting.*
import java.util.HashMap




var bitmap:Bitmap? = null
val workHandler = Handler(HandlerThread("worker").also { it.start() }.looper)


class SettingActivity : AppCompatActivity() {
    private lateinit var signInButton: Button
    private lateinit var singOutButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        bitmap?: workHandler.post {
            bitmap = Glide.with(this).asBitmap().load("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg").submit().get()
            runOnUiThread {
                qrCodeImageView.setImageBitmap(bitmap)
            }
        }
       // bitmap?.let{qrCodeImageView.setImageBitmap(it)}

        signInButton = findViewById(R.id.btn_signin)
        singOutButton = findViewById(R.id.btn_signout)
        qrCodeImageView = findViewById(R.id.img_qrcode)
        backButton = findViewById(R.id.btn_back)

        backButton.setOnClickListener(View.OnClickListener {
            finish()
        })


        signInButton.setOnClickListener(View.OnClickListener {
            createQrcodeAndCheck()
            NeuLog.e()
        })

        singOutButton.setOnClickListener(View.OnClickListener {
            NeuLog.e()
        })


    }

    fun requestGernerateQRCodeForLogin(specificParams:MutableMap<String, String>, callback: QrcodeLoginUtil.IGenerateCallBack){
        (QrcodeLoginUtil::requestGernerateQRCodeForLogin)(specificParams, callback)
        NeuLog.e()
    }

    private var countDownTimer: CountDownTimer? = null
    private fun createQrcodeAndCheck() {
        QrcodeLoginUtil.requestGernerateQRCodeForLogin(object :
            HashMap<String, String>() {
            init {
                put("size", "L")
            }
        }, object : QrcodeLoginUtil.IGenerateCallBack {
            override fun qrcodeImage(bitmap: Bitmap, q: String) {
                qrCodeImageView.setImageBitmap(bitmap)
                bitmap?.let{qrCodeImageView.setImageBitmap(it)}
                if (countDownTimer != null) {
                    countDownTimer!!.cancel()
                }
                countDownTimer = object : CountDownTimer(Int.MAX_VALUE.toLong(), 3000) {
                    override fun onTick(millisUntilFinished: Long) {
                        QrcodeLoginUtil.checkQRCodeLoginStatus(object :
                            HashMap<String?, String?>() {
                            init {
                                put("qrcode_id", q)
                            }
                        }, object : IDataCallBack<XmlyAuth2AccessToken?> {
                                override fun onSuccess(objects: XmlyAuth2AccessToken?) {
                                    if (objects!= null) {

                                        AccessTokenManager.getInstanse().setAccessTokenAndUid(
                                            objects.token,
                                            objects.refreshToken,
                                            objects.expiresAt,
                                            objects.uid
                                        )
                                    if (countDownTimer != null) {
                                        countDownTimer!!.cancel()
                                    }
                                }
                            }

                            override fun onError(
                                code: Int,
                                message: String
                            ) {
                                if (code == 207) {
                                    NeuLog.e(TAG, "Client is not logged in")
                                } else if (code == 217) {
                                    NeuLog.e(TAG, "QRCode is expired")
                                    if (countDownTimer != null) {
                                        countDownTimer!!.cancel()
                                    }
                                    createQrcodeAndCheck()
                                }
                            }
                        })
                    }

                    override fun onFinish() {}
                }
                countDownTimer?.start()
            }

            override fun onError(code: Int, message: String) {}
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}

private const val TAG = "SETTING"