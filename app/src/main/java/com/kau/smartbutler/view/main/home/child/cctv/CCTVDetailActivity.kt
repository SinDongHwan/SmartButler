package com.kau.smartbutler.view.main.home.child.cctv
//현 문제점 스레드, 해결방법 두가지 버튼(뒤로가기, 다음으로 갈때 쓰레드를 종료한다.
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.gson.JsonObject
import com.kau.smartbutler.R
import com.kau.smartbutler.base.BaseActivity
import com.kau.smartbutler.model.CCTV
import com.kau.smartbutler.model.PostDetectionAreaRequest
import com.kau.smartbutler.util.network.getCCTVNetworkInstance
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_cctv_detail.*
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.*

class CCTVDetailActivity(

        override val layoutRes : Int = R.layout.activity_cctv_detail,
        override val isUseDatabinding: Boolean = false)
    : BaseActivity() {
    ////////////////////////////////////////////////////////////////////////////////////////////////////
//use variable
    internal var handler = Handler()
    internal var OffActivity = false

    internal var width = 300 // 축소시킬 너비
    internal var height = 300 // 축소시킬 높이
    lateinit var realm: Realm
    internal var conf: Bitmap.Config = Bitmap.Config.ARGB_8888
    internal var giving_image = Bitmap.createBitmap(width, height, conf)
////////////////////////////////////////////////////////////////////////////////////////////////////

    override var isChildActivity: Boolean = true
    lateinit var res : JsonObject
    override fun setupView() {
        super.setupView()
        Realm.init(this)
        val infoCCTV : CCTV

        if (intent.hasExtra("cctv")){
            infoCCTV = intent.getParcelableExtra<CCTV>("cctv")
        }else{
            infoCCTV = CCTV(1, "현관")
        }

        var detected_info = ""
        if (intent.hasExtra("Detected_Event") && intent.getStringExtra("Detected_Event").equals("")){
            detected_info = intent.getStringExtra("Detected_Event")
        }
        detectedInfoTextView.setText(detected_info)

        realm = Realm.getDefaultInstance()
        var viewItem =realm.where<CCTVRealmStruct>(CCTVRealmStruct::class.java).equalTo("Location",infoCCTV.name)?.findFirst()
        Toast.makeText(this,viewItem.toString(), Toast.LENGTH_LONG).show()

        val iv = findViewById(R.id.video_view) as ImageView
        val t = Thread(Runnable {
            try {
                val tt = object : TimerTask() {
                    override fun run() {
                        while (true) {
                            try {

                                val url = URL("http://112.169.29.116:25001/cgi-bin/viewer/video.jpg?resolution=640x480")

//                                Log.d("myapp", "kkk: " + iv.width)
//                                Log.d("myapp", "kkk : "+ iv.height)

                                val t_connection = url.openConnection()
                                t_connection.readTimeout = 10000
                                val `is` = t_connection.getInputStream()
                                val bm = BitmapFactory.decodeStream(`is`)
//
//                                Log.d("myapp", "kkk: " + iv.width)
//                                Log.d("myapp", "kkk : "+ iv.height)

                                val width = 700 // 축소시킬 너비
                                val height = 525 // 축소시킬 높이

                                val resized = Bitmap.createScaledBitmap(bm, iv.width, iv.height, true)
                                val resized2 = Bitmap.createScaledBitmap(bm, width, height, true)

                                giving_image = resized2

                                handler.post {
                                    // 화면에 그려줄 작업
                                    //iv.setImageBitmap(resized);
                                    iv.setImageBitmap(resized)
                                }

                                iv.setImageBitmap(resized) //비트맵 객체로 보여주기
                                iv.invalidate()
                            } catch (e: Exception) {
                                Log.d("myapp", "Error : $e")
                            }
                        }
                    }
                }
                val timer = Timer()
                timer.schedule(tt, 0, 500)
            } catch (e: Exception) {
            }
        })
        t.start()
        moreNavButton.setOnClickListener {
            ////////////////////////////////////////////////////////////////////////////////////////////////////
//버튼 클릭 리스너
            val bStream = ByteArrayOutputStream()
            giving_image.compress(Bitmap.CompressFormat.PNG, 0, bStream)
            val byteArray = bStream.toByteArray()
            val i = Intent(this, CCTVSetDomainActivity::class.java)
            i.putExtra("image", byteArray)
            i.putExtra("cctv", infoCCTV)
            OffActivity = true
            realm.close()
            startActivity(i)

////////////////////////////////////////////////////////////////////////////////////////////////////
        }
    }

}