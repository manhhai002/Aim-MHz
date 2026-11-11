package com.aimsimulator

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.aimsimulator.model.BallObservation
import com.aimsimulator.model.ShotPredictor
import com.aimsimulator.ui.TableView
import com.aimsimulator.ui.TrajectoryOverlayView
import com.aimsimulator.vision.BallDetector
import org.opencv.android.OpenCVLoader
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {
    private lateinit var table: TableView
    private lateinit var overlay: TrajectoryOverlayView
    private lateinit var status: TextView
    private lateinit var aimToggle: Button
    private val cv = BallDetector()
    private val predictor = ShotPredictor()
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isScanning = AtomicBoolean(false)
    private var lastBalls: List<BallObservation> = emptyList()
    private var cvReady = false
    private var destroyed = false

    private val scanTask = object : Runnable {
        override fun run() {
            if (destroyed) return
            if (cvReady && table.width > 0 && table.height > 0 &&
                isScanning.compareAndSet(false, true)) {
                // UI-thread snapshot; background detection. No capture of other apps.
                val picture = table.snapshot()
                worker.execute {
                    val result: List<BallObservation>
                    val error: String?
                    try {
                        result = cv.detect(picture)
                        error = null
                    } catch (t: Exception) {
                        result = emptyList()
                        error = t.localizedMessage ?: t.javaClass.simpleName
                    } finally {
                        picture.recycle()
                    }
                    mainHandler.post {
                        if (!destroyed) {
                            lastBalls = result
                            overlay.detected = result
                            updatePrediction()
                            status.text = when {
                                error != null -> "OpenCV error: $error"
                                result.none { it.id == "cue" } -> "Chưa thấy bi cái • thử đặt lại bàn"
                                else -> "OpenCV: ${result.size} bi • ${if (table.engine.hasMotion()) "Đang chuyển động" else "Sẵn sàng"}"
                            }
                        }
                        isScanning.set(false)
                    }
                }
            }
            mainHandler.postDelayed(this, 220L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        window.navigationBarColor = Color.rgb(9, 16, 25)
        window.statusBarColor = Color.rgb(9, 16, 25)
        cvReady = try { OpenCVLoader.initLocal() } catch (_: Throwable) { false }
        buildUi()
        table.onAimChange = { updatePrediction() }
        table.onPhysicsChange = {
            if (table.engine.hasMotion()) overlay.prediction = null
        }
        if (cvReady) {
            status.text = "OpenCV đã tải • đang nhận diện…"
            mainHandler.post(scanTask)
        } else {
            status.text = "Không tải được OpenCV. Kiểm tra Gradle/ABI."
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(10, 18, 29))
        }
        table = TableView(this)
        overlay = TrajectoryOverlayView(this)
        val arena = FrameLayout(this).apply {
            addView(table, FrameLayout.LayoutParams(-1, -1))
            addView(overlay, FrameLayout.LayoutParams(-1, -1))
        }
        root.addView(arena, LinearLayout.LayoutParams(0, -1, 1f))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.rgb(20, 31, 44))
        }
        val sidebar = ScrollView(this).apply {
            isFillViewport = true
            addView(panel)
            setBackgroundColor(Color.rgb(20, 31, 44))
        }
        root.addView(sidebar, LinearLayout.LayoutParams(dp(255), -1))
        panel.addView(text("AIM TOOL", 22f, Color.rgb(235, 246, 250), true))
        panel.addView(text("SIMULATOR V1.0", 11f, Color.rgb(91, 211, 164), true).apply {
            letterSpacing = 0.13f
        }, margins(bottom = dp(7)))
        panel.addView(text("Chạm / kéo trên bàn để đổi hướng cơ.",
            12f, Color.rgb(167, 182, 196)), margins(bottom = dp(9)))
        status = text("Đang khởi tạo…", 12f, Color.rgb(255, 217, 127))
        panel.addView(status, margins(bottom = dp(13)))

        panel.addView(button("ĐÁNH THỬ", Color.rgb(18, 139, 112)) {
            table.shoot()
            overlay.prediction = null
        }, margins(bottom = dp(8)))
        panel.addView(button("ĐẶT LẠI BÀN", Color.rgb(51, 72, 94)) {
            table.reset(false)
            lastBalls = emptyList()
            overlay.prediction = null
            overlay.detected = emptyList()
        }, margins(bottom = dp(8)))
        panel.addView(button("BÀN NGẪU NHIÊN", Color.rgb(51, 72, 94)) {
            table.reset(true)
            lastBalls = emptyList()
            overlay.prediction = null
            overlay.detected = emptyList()
        }, margins(bottom = dp(8)))
        aimToggle = button("ẨN AIM", Color.rgb(50, 68, 85)) {
            overlay.enabled = !overlay.enabled
            aimToggle.text = if (overlay.enabled) "ẨN AIM" else "HIỆN AIM"
        }
        panel.addView(aimToggle, margins(bottom = dp(12)))
        panel.addView(text("–––  CHÚ GIẢI  –––", 10f, Color.rgb(141, 161, 176), true))
        panel.addView(text("Trắng: đường bi cái\nVàng: bi mục tiêu\nXanh: bi cái sau chạm",
            11f, Color.rgb(188, 208, 213)), margins(top = dp(6)))
        panel.addView(text("Bản mô phỏng độc lập • Không có quyền overlay hệ thống hoặc đọc màn hình game khác.",
            10f, Color.rgb(131, 149, 166)), margins(top = dp(12)))
        setContentView(root)
    }

    private fun updatePrediction() {
        if (table.engine.hasMotion()) { overlay.prediction = null; return }
        overlay.prediction = predictor.predict(lastBalls, table.engine.aim, maxBanks = 2)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.START
        }

    private fun button(label: String, bgColor: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.WHITE)
            isAllCaps = false
            this.background = GradientDrawable().apply {
                cornerRadius = dp(11).toFloat()
                setColor(bgColor)
            }
            setOnClickListener { action() }
        }

    private fun margins(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = top
            bottomMargin = bottom
        }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroy() {
        destroyed = true
        mainHandler.removeCallbacks(scanTask)
        worker.shutdownNow()
        super.onDestroy()
    }
}
