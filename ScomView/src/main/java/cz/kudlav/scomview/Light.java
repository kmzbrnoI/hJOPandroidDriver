package cz.kudlav.scomview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

public class Light extends View {

    private final Paint paint = new Paint();

    public Light(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.Light,
                0, 0);

        int color = Color.DKGRAY;
        try {
            String colorProperty = a.getString(R.styleable.Light_light_color);
            if (colorProperty != null) color = Color.parseColor(colorProperty);
        } finally {
            a.recycle();
        }

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    public void off() {
        clearAnimation();
        setAlpha(0.2f);
    }

    public void on() {
        clearAnimation();
        setAlpha(1);
    }

    public void blink(Animation animation) {
        setAlpha(1);
        startAnimation(animation);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(heightMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        canvas.translate(getHeight()/2f,getHeight()/2f);
        canvas.drawCircle(0, 0, getHeight()/2f, paint);
    }

}
