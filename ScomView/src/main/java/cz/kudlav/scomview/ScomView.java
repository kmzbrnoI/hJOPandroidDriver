package cz.kudlav.scomview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ScomView extends LinearLayout {

    private int code = -1;
    private String title = "";

    private final Light yellowTop;
    private final Light green;
    private final Light red;
    private final Light white;
    private final Light yellowBottom;

    private final Animation slowBlink = new AlphaAnimation(0.2f, 1.0f);

    public ScomView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        
        inflate(context, R.layout.scom_view, this);

        yellowTop = findViewById(R.id.yellow_top);
        green = findViewById(R.id.green);
        red = findViewById(R.id.red);
        white = findViewById(R.id.white);
        yellowBottom = findViewById(R.id.yellow_bottom);

        slowBlink.setRepeatMode(Animation.REVERSE);
        slowBlink.setRepeatCount(Animation.INFINITE);
        slowBlink.setDuration(100); // 54x per minute
        slowBlink.setStartOffset(400);

        View root = findViewById(R.id.root);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!title.isEmpty()) Toast.makeText(context, title, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setCode(int code) {
        if (this.code != code) {
            this.code = code;

            yellowTop.off();
            green.off();
            red.off();
            white.off();
            yellowBottom.off();

            switch (code) {
                case 0:
                    title = "Stůj";
                    red.on();
                    break;
                case 1:
                    title = "Volno";
                    green.on();
                    break;
                case 2:
                    title = "Výstraha";
                    yellowTop.on();
                    break;
                case 3:
                    title = "Očekávej 40 km/h";
                    yellowTop.blink(slowBlink);
                    break;
                case 4:
                    title = "40 km/h a volno";
                    green.on();
                    yellowBottom.on();
                    break;
                case 6:
                    title = "40 km/h a výstraha";
                    yellowTop.on();
                    yellowBottom.on();
                    break;
                case 7:
                    title = "40 km/h, očekávej 40 km/h";
                    yellowTop.blink(slowBlink);
                    yellowBottom.on();
                    break;
                case 8:
                    title = "Přivolávací návěst";
                    red.on();
                    white.blink(slowBlink);
                    break;
                case 9:
                    title = "Dovolen zajištěný posun";
                    red.on();
                    white.on();
                    break;
                case 10:
                    title = "Dovolen nezajištěný posun";
                    white.on();
                    break;
                case 11:
                    title = "Opakování návěsti volno";
                    green.on();
                    white.on();
                    break;
                case 12:
                    title = "Opakování návěsti výstraha";
                    yellowTop.on();
                    white.on();
                    break;
                case 14:
                    title = "Opakování návěsti Očekávej 40 km/h";
                    yellowTop.blink(slowBlink);
                    white.on();
                    break;
                case 15:
                    title = "Rychlost 40 km/h, opakování návěsti Výstraha";
                    yellowTop.on();
                    white.on();
                    yellowBottom.on();
                    break;
                default:
                    title = "";
            }
        }
    }

    public int getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
