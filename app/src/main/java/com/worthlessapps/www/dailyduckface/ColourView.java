package com.worthlessapps.www.dailyduckface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 *
 * Created by Fletcher on 26/04/2015.
 */
public class ColourView extends View {

    private Paint mFillPaint;
    private Paint mStrokePaint;

    public ColourView(Context context, int colour) {
        super(context);
        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(colour);

        mStrokePaint = new Paint();
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(2);
        mStrokePaint.setColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;
        int radius = (int) (Math.min(halfWidth, halfHeight) * .8);
        canvas.drawCircle(halfWidth, halfHeight, radius, mFillPaint);
        canvas.drawCircle(halfWidth, halfHeight, radius, mStrokePaint);

        super.onDraw(canvas);
    }
}
