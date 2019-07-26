package com.kau.smartbutler.view.main.home.child.cctv
//현 문제점 스레드, 해결방법 두가지 버튼(뒤로가기, 다음으로 갈때 쓰레드를 종료한다.
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
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
import io.realm.com_kau_smartbutler_view_main_home_child_cctv_CCTVRealmStructRealmProxy
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
    internal var coordinates = ArrayList<ArrayList<Float>>()
    internal var imagewidth = 0
    internal var imageheight = 0
    var canvas = Canvas()
    var mPaint = Paint()

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

        var is_area = false
        if (viewItem != null){
            is_area = getArea(viewItem)
        }
        Toast.makeText(this,viewItem.toString(), Toast.LENGTH_LONG).show()

        val iv = findViewById(R.id.video_view) as ImageView
        val vto = iv.getViewTreeObserver()
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mPaint.setColor(Color.BLUE)
                mPaint.setStrokeWidth(10F)
                mPaint.setStyle(Paint.Style.STROKE)
                mPaint.setAntiAlias(true)
                mPaint.setDither(true)

                imageheight = iv.height
                imagewidth = iv.width

                val t = Thread(Runnable {
                    try {
                        val tt = object : TimerTask() {
                            override fun run() {
                                while (true) {
                                    try {
                                        if(isFinishing){
                                            break
                                        }
                                        val url = URL("http://112.169.29.116:25001/cgi-bin/viewer/video.jpg?resolution=640x480")
                                        val t_connection = url.openConnection()
                                        t_connection.readTimeout = 10000
                                        val input_stream = t_connection.getInputStream()
                                        val bm = BitmapFactory.decodeStream(input_stream)
                                        val resized = Bitmap.createScaledBitmap(bm, iv.width, iv.height, true)
                                        val width = 700 // 축소시킬 너비
                                        val height = 525 // 축소시킬 높이
                                        val resized2 = Bitmap.createScaledBitmap(bm, width, height, true)

                                        canvas.setBitmap(resized)

                                        if (is_area){
                                            onDraw(canvas)
                                        }
                                        giving_image = resized2
                                        handler.post {
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

            }
        })
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
            finish()
            startActivity(i)
////////////////////////////////////////////////////////////////////////////////////////////////////
        }
    }
    fun getArea(area_info : CCTVRealmStruct?): Boolean {
        var points_list : List<String> = ArrayList<String>()
        var draw_points = ArrayList<Float>()

        if ( (area_info as com_kau_smartbutler_view_main_home_child_cctv_CCTVRealmStructRealmProxy).`realmGet$Abandon`() != "[]")
        {
            points_list = (area_info as com_kau_smartbutler_view_main_home_child_cctv_CCTVRealmStructRealmProxy).`realmGet$Abandon`().replace("[^0-9.,]".toRegex(),"").split(',')
        } else if ((area_info as com_kau_smartbutler_view_main_home_child_cctv_CCTVRealmStructRealmProxy).`realmGet$Loitering`() != "[]")
        {
            points_list = (area_info as com_kau_smartbutler_view_main_home_child_cctv_CCTVRealmStructRealmProxy).`realmGet$Loitering`().replace("[^0-9.,]".toRegex(),"").split(',')
        } else if ((area_info as com_kau_smartbutler_view_main_home_child_cctv_CCTVRealmStructRealmProxy).`realmGet$Intrusion`() != "[]"){
            points_list = (area_info as com_kau_smartbutler_view_main_home_child_cctv_CCTVRealmStructRealmProxy).`realmGet$Intrusion`().replace("[^0-9.,]".toRegex(),"").split(',')
        }

        if (points_list.isEmpty()){
            return false
        }
        for (index in 0..points_list.size-1){
            draw_points.add(points_list[index].toFloat())

            if (index%2 != 0){
                coordinates.add(draw_points)
                draw_points = ArrayList<Float>()
            }
        }
        return true
    }

    internal fun onDraw(canvas :Canvas) {
        mPaint.setColor(Color.GREEN)
        mPaint.setStrokeWidth(5F)
        mPaint.setStyle(Paint.Style.STROKE)
        mPaint.setAntiAlias(true)
        mPaint.setDither(true)

        var drawX = (coordinates[0][0] * imagewidth)
        var drawY = (coordinates[0][1] * imageheight)
        canvas.drawCircle(drawX, drawY, 10f, mPaint)
        for (i in 1 until coordinates.size) {
            var predrawX = coordinates[i-1][0] * imagewidth
            var predrawY = coordinates[i-1][1] * imageheight
            var nowdrawX = coordinates[i][0] * imagewidth
            var nowdrawY = coordinates[i][1] * imageheight

            canvas.drawCircle(nowdrawX, nowdrawY, 10f, mPaint)
            canvas.drawLine(nowdrawX, nowdrawY, predrawX, predrawY, mPaint)

        }
        canvas.drawLine(drawX, drawY, coordinates[coordinates.size-1][0]*imagewidth, coordinates[coordinates.size-1][1]*imageheight, mPaint) //최종좌표와 시작점 선긋기
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}