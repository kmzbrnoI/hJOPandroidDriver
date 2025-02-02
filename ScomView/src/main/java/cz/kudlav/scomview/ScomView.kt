package cz.kudlav.scomview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.view.animation.Animation
import android.view.animation.AlphaAnimation
import android.widget.Toast

class ScomView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val yellowTop: Light
    private val green: Light
    private val red: Light
    private val white: Light
    private val yellowBottom: Light
    private val slowBlink: Animation = AlphaAnimation(0.2f, 1.0f)

    var title = ""
        private set

    var code = -1
        set(code) {
            if (this.code != code) {
                field = code
                yellowTop.off()
                green.off()
                red.off()
                white.off()
                yellowBottom.off()
                when (code) {
                    0 -> {
                        title = "Stůj"
                        red.on()
                    }
                    1 -> {
                        title = "Volno"
                        green.on()
                    }
                    2 -> {
                        title = "Výstraha"
                        yellowTop.on()
                    }
                    3 -> {
                        title = "Očekávej 40 km/h"
                        yellowTop.blink(slowBlink)
                    }
                    4 -> {
                        title = "40 km/h a volno"
                        green.on()
                        yellowBottom.on()
                    }
                    6 -> {
                        title = "40 km/h a výstraha"
                        yellowTop.on()
                        yellowBottom.on()
                    }
                    7 -> {
                        title = "40 km/h, očekávej 40 km/h"
                        yellowTop.blink(slowBlink)
                        yellowBottom.on()
                    }
                    8 -> {
                        title = "Přivolávací návěst"
                        red.on()
                        white.blink(slowBlink)
                    }
                    9 -> {
                        title = "Dovolen zajištěný posun"
                        white.on()
                    }
                    10 -> {
                        title = "Dovolen nezajištěný posun"
                        red.on()
                        white.on()
                    }
                    11 -> {
                        title = "Opakování návěsti volno"
                        green.on()
                        white.on()
                    }
                    12 -> {
                        title = "Opakování návěsti výstraha"
                        yellowTop.on()
                        white.on()
                    }
                    14 -> {
                        title = "Opakování návěsti Očekávej 40 km/h"
                        yellowTop.blink(slowBlink)
                        white.on()
                    }
                    15 -> {
                        title = "40 km/h, opakování návěsti Výstraha"
                        yellowTop.on()
                        white.on()
                        yellowBottom.on()
                    }
                    16 -> {
                        title = "40 km/h, opakování návěsti očekávej 40 km/h"
                        yellowTop.blink(slowBlink)
                        white.on()
                        yellowBottom.on()
                    }
                    else -> title = ""
                }
            }
        }

    init {
        inflate(context, R.layout.scom_view, this)
        yellowTop = findViewById(R.id.yellow_top)
        green = findViewById(R.id.green)
        red = findViewById(R.id.red)
        white = findViewById(R.id.white)
        yellowBottom = findViewById(R.id.yellow_bottom)

        slowBlink.repeatMode = Animation.REVERSE
        slowBlink.repeatCount = Animation.INFINITE
        slowBlink.duration = 100 // 54x per minute
        slowBlink.startOffset = 400

        val root = findViewById<View>(R.id.root)
        root.setOnClickListener {
            if (title.isNotEmpty()) Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
        }
    }
}
