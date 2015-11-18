package com.worthlessapps.www.dailyduckface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by pablo on 10/04/15.
 */
public class DrawableView extends ImageView {

    private Bitmap mScaledBitmap;
    private Canvas mCanvas;
    private Bitmap mDrawableBitmap;
    private Paint mPaint;

    private List<List<Runnable>> mHistory;
    private float mMoveStartX;
    private float mMoveStartY;

    public DrawableView(Context context, AttributeSet attr) {
        super(context, attr);
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(5f);
        mHistory = new ArrayList<>();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {

        int bitmapWidth = bm.getWidth();
        int bitmapHeight = bm.getHeight();
        float scale = Math.min(getWidth() / (float) bitmapWidth, getHeight() / (float) bitmapHeight);

        mScaledBitmap = Bitmap.createScaledBitmap(bm, (int) (bitmapWidth * scale),
                (int) (bitmapHeight * scale), true);

        bitmapWidth = mScaledBitmap.getWidth();
        bitmapHeight = mScaledBitmap.getHeight();

        mCanvas = new Canvas(mDrawableBitmap = Bitmap.createBitmap(bitmapWidth,
                bitmapHeight, Bitmap.Config.ARGB_8888));
        mCanvas.drawBitmap(mScaledBitmap, (getWidth() / 2f) - (bitmapWidth / 2f),
                (getHeight() / 2f) - (bitmapHeight / 2f), null);

        super.setImageBitmap(mDrawableBitmap);
    }

    public Bitmap getImageBitmap() {
        return mDrawableBitmap;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {

        if (mCanvas == null) {
            return;
        }
        canvas.drawBitmap(mDrawableBitmap, 0, 0, null);
    }

    public void undo() {

        // remove the last List of drawRunnables
        if (mHistory.size() > 0) mHistory.remove(mHistory.size() - 1);

        // refresh the original bitmap
        setImageBitmap(mScaledBitmap);

        // This looks time consuming, but it only runs if the undo button is pushed, it just
        // runs all of the drawRunnables except for the last List which was removed
        for (List<Runnable> drawRunnableList : mHistory) {
            for (Runnable runnable : drawRunnableList) {
                runnable.run();
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMoveStartX = event.getX();
                mMoveStartY = event.getY();
                mHistory.add(new ArrayList<Runnable>());
                break;
            case MotionEvent.ACTION_MOVE:

                final float moveFinishX = event.getX();
                final float moveFinishY = event.getY();
                if (moveFinishX != mMoveStartX || moveFinishY != mMoveStartY) {
                    Runnable drawRunnable = new Runnable() {

                        final float startX = mMoveStartX;
                        final float startY = mMoveStartY;
                        final Paint paint = mPaint;

                        @Override
                        public void run() {

                            mCanvas.drawLine(startX, startY,
                                    moveFinishX, moveFinishY, paint);
                        }
                    };
                    drawRunnable.run();
                    mHistory.get(mHistory.size() - 1).add(drawRunnable);
                    invalidate();
                    mMoveStartX = moveFinishX;
                    mMoveStartY = moveFinishY;
                }
                break;
            case MotionEvent.ACTION_UP:
                mPaint = new Paint(mPaint);
                break;
            default:
                return false;
        }
        return true;
    }

    public void setPaintColour(int colour) {
        mPaint.setColor(colour);
    }
}
