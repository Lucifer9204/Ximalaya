package bmw.ximalaya.testauto.shared

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_setting.*

var bitmap:Bitmap? = null
val workHandler = Handler(HandlerThread("worker").also { it.start() }.looper)
class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        bitmap?: workHandler.post {
            bitmap = Glide.with(this).asBitmap().load("https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg").submit().get()
            runOnUiThread {
                imageView.setImageBitmap(bitmap)
            }
        }
        bitmap?.let{imageView.setImageBitmap(it)}
    }
}