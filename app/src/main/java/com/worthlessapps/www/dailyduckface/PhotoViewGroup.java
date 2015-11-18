package com.worthlessapps.www.dailyduckface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Created by pablo on 10/04/15.
 */
public class PhotoViewGroup extends ViewGroup {

    interface PhotoClickListener {
        void onItemClick(long id);
        void onItemLongPress(long id);
    }

    private static final int SINGLE_TAP_EVENT = 1;
    private static final int LONG_PRESS_EVENT = 2;

    private static final int PIXEL_TO_POSITION_RATIO = 400;
    private static final float CHILD_HEIGHT_RATIO = 0.45f;
    private static final float CHILD_WIDTH_RATIO = 0.75f;
    private static final float FLING_VELOCITY_THRESHOLD = 0.02f;

    //private Context mContext;
    private Adapter mAdapter;
    private GestureDetector mGestureDetector;
    /**
     * mViewCache provides removed Views to mAdapter to be used in getView() as convertView
     */
    private LinkedList<View> mViewCache = new LinkedList<>();
    private PhotoClickListener mListener;
    /**
     * mScrollPosition has a minimum value of 0 and a maximum value of getChildCount()
     * Views are positioned according to this value
     */
    private float mScrollPosition = 0;
    private int mFirstChildPosition;
    private int mLastChildPosition;

    private int mChildHeight;
    private int mChildWidth;
    private float mScrollVelocity;

    private Camera mCamera;
    private Matrix mMatrix;
    private Paint mPaint;

    private int mPositionDeleting = -1;
    private float mDeleteAnimationProgress = 0;

    private Thread mFlingThread;
    private Thread mSnapThread;
    private Thread mDeleteThread;

    public PhotoViewGroup(Context context, AttributeSet attr) {
        super(context, attr);

        mGestureDetector = new GestureDetector(context, new GestureDetectorListener());
        mCamera = new Camera();
        mMatrix = new Matrix();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
    }

    public void setAdapter(Adapter adapter) {
        Log.v("", "setAdapter()");
        mAdapter = adapter;
        removeAllViews();
        requestLayout();
    }

    public void setOnItemClickListener(PhotoClickListener listener) {
        mListener = listener;
    }

    public float getScrollPosition() {
        return mScrollPosition;
    }

    public void setScrollPosition(float position) {
        mScrollPosition = position == -1 ? mAdapter.getCount() - 1 : position;
        requestLayout();
    }

    public void scrollToPosition(int position) {
        mSnapThread = new SnapThread(position == -1 ? mAdapter.getCount() - 1 : position);
        mSnapThread.start();
    }

    public void deleteItem(int position) {

        mDeleteThread = new DeleteThread(position);
        mDeleteThread.start();
    }

    private View getView(int position) {

        // This is an important optimisation for an adapter view.
        // Maintain a cache of views that have been removed and pass
        // them to the adapter to update when you need a new one.
        View convertView;
        try {
            convertView = mViewCache.getFirst();
            mViewCache.removeFirst();
        } catch (NoSuchElementException e) {
            convertView = null;
        }
        return mAdapter.getView(position, convertView, this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        // If mAdapter is null or empty, return.
        if (mAdapter == null || mAdapter.getCount() == 0) {
            return;
        }
        // Use layout dimensions to set appropriate child height and width
        mChildHeight = (int) ((bottom - top) * CHILD_HEIGHT_RATIO);
        mChildWidth = (int) ((right - left) * CHILD_WIDTH_RATIO);

        updateChildren();

        RectF rect;
        for (int i = 0; i < getChildCount(); i++) {
            rect = getChildDimensions(mFirstChildPosition + i);
            getChildAt(i).layout((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
        }
        invalidate();
        for (int position = mFirstChildPosition; position < mFirstChildPosition + getChildCount(); position++) {
            rect = getChildAdjustedDimensions(position);
            Log.v("", "isVisible(" + position + ") = " + (rect.bottom >= 0 && rect.top <= getHeight()) + " rect(" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + ")");

        }
    }

    private void addAndMeasureChild(View child, int index) {

        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        child.setDrawingCacheEnabled(true);
        addView(child, index, params);

        int childWidth = (int) (getWidth() * CHILD_WIDTH_RATIO);
        child.measure(MeasureSpec.EXACTLY | childWidth, MeasureSpec.EXACTLY | mChildHeight);
    }

    private void updateChildren() {

        // Ensure this value is within bounds, may not be after a delete
        keepScrollWithinBounds();
        boolean finished = true;
        while (true) {
            // Layout begins from the centre child.
            if (getChildCount() == 0) {
                // If there aren't any views find the position closest to mScrollPosition.
                mFirstChildPosition = mLastChildPosition = Math.round(mScrollPosition);
                // Get the view at this position and add it as a child.
                addAndMeasureChild(getView(mFirstChildPosition), 0);
            }
            // Check the top, remove if not visible
            // Otherwise, check if the view above should be visible and add if so.
            if (!isVisible(mFirstChildPosition)) {
                Log.v("", "Removing first view");
                mViewCache.add(getChildAt(0));
                removeViewAt(0);
                mFirstChildPosition++;
                finished = false;
            } else if (mFirstChildPosition > 0 && isVisible(mFirstChildPosition - 1)) {
                Log.v("", "Adding view above");
                addAndMeasureChild(getView(--mFirstChildPosition), 0);
                finished = false;
            }
            if (!isVisible(mLastChildPosition)) {
                Log.v("", "Removing last view");
                mViewCache.add(getChildAt(getChildCount() - 1));
                removeViewAt(getChildCount() - 1);
                mLastChildPosition--;
                finished = false;
            } else if (mLastChildPosition < mAdapter.getCount() - 1 && isVisible(mLastChildPosition + 1)) {
                Log.v("", "Adding view below");
                addAndMeasureChild(getView(++mLastChildPosition), -1);
                finished = false;
            }
            if (finished) break;
        }
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {

        final Bitmap bitmap = child.getDrawingCache();
        if (bitmap == null) {
            return super.drawChild(canvas, child, drawingTime);
        }
        int position = (int) child.getTag(R.string.view_tag_position);
        setMatrix(position);
        canvas.drawBitmap(bitmap, mMatrix, mPaint);
        return false;
    }

    private float distanceFromCentre(int position) {

        float positionFromCentre = position - mScrollPosition;
        return (float) (Math.tanh(positionFromCentre * 0.55) * 1.1);
    }

    private RectF getChildDimensions(int position) {

        int halfFrameWidth = getWidth() / 2;
        int halfFrameHeight = getHeight() / 2;
        int halfChildWidth = mChildWidth / 2;
        int halfChildHeight = mChildHeight / 2;

        // Centre the view horizontally
        int left = halfFrameWidth - halfChildWidth;
        // Find where the top should be according to distanceFromCentre and halfChildHeight
        int top = (int) ((halfFrameHeight * distanceFromCentre(position)) + halfFrameHeight - halfChildHeight);

        return new RectF(left, top, left + mChildWidth, top + mChildHeight);
    }

    private RectF getChildAdjustedDimensions(int position) {

        RectF rect = new RectF(0f, 0f, mChildWidth, mChildHeight);
        setMatrix(position);
        mMatrix.mapRect(rect);
        return rect;
    }

    private void setMatrix(int position) {

        final RectF rect = getChildDimensions(position);

        final int halfChildWidth = mChildWidth / 2;
        final int halfChildHeight = mChildHeight / 2;

        float rotation = distanceFromCentre(position) * 90;
        float scale =  1 - Math.abs(distanceFromCentre(position));
        // deleteScale will just equal 1 unless this position is being deleted
        float deleteScale = position == mPositionDeleting ? 1 - mDeleteAnimationProgress : 1;

        mCamera.save();
        mCamera.rotateX(rotation);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        mMatrix.preTranslate(-halfChildWidth, -halfChildHeight);
        mMatrix.postScale(scale * deleteScale, scale * deleteScale);
        mMatrix.postTranslate(halfChildWidth + rect.left, halfChildHeight + rect.top);
    }

    private boolean isVisible(int position) {
        RectF rect = getChildAdjustedDimensions(position);
        return rect.bottom >= 0 && rect.top <= getHeight();
    }

    private boolean intersectsView(int position, float x, float y) {

        /*
        The view as it appears on screen is an isosceles trapezoid, not a rectangle. Therefore,
        the linear functions (y = mx + c) of the left and right sides need to be calculated.  The
        RectF returned by getChildAdjustedDimensions has the same height as the view and the width
        of the view's long side. If (position - mScrollPosition) is positive, the view's bottom will
        be the longer side and vice versa. The variables longSideY and shortSideY assigned to
        rect.bottom or rect.top will allow for these two situations.
         */
        RectF rect = getChildAdjustedDimensions(position);
        if (position - mScrollPosition == 0) {
            // This view is in the centre and therefore a rectangle.
            return rect.contains(x, y);
        }
        float shortSideY;
        float longSideY;
        // Which side of the centre?
        if (position > mScrollPosition) {
            // rect.bottom is the long side
            longSideY = rect.bottom;
            shortSideY = rect.top;
        } else {
            // rect.top is the long side
            longSideY = rect.top;
            shortSideY = rect.bottom;
        }
        // Scale as in setMatrix()
        float averageWidth = mChildWidth * (1 - Math.abs(distanceFromCentre(position)));
        float shortSideLeft = rect.right - averageWidth;
        // m = dy/dx = (shortSideY - longSideY) / (shortSideX - longSideX)
        float leftM = (shortSideY - longSideY) / (shortSideLeft - rect.left);
        // rightM = -leftM
        // y = mx + c   =>   c = y - mx
        float leftC = longSideY - (leftM * rect.left);
        float rightC = longSideY - (-leftM * rect.right);
        Log.v("", "y = " + leftM + "x + " + leftC);
        if (y >= rect.top && y <= rect.bottom) {
            // we know y is within view top and bottom, return true if x is between the sides
            // y = mx + c   =>   x = (y - c) / m
            return (x >= (y - leftC) / leftM && x <= (y - rightC) / -leftM);
        } else {
            return false;
        }
    }

    private void delegateClick(float x, float y, int eventType) {

        Log.v("", "layout w, h = " + getWidth() + ", " + getHeight());
        for (int position = mFirstChildPosition; position <= mLastChildPosition; position++) {
            if (intersectsView(position, x, y)) {
                switch (eventType) {
                    case SINGLE_TAP_EVENT:
                        mListener.onItemClick(mAdapter.getItemId(position));
                        break;
                    case LONG_PRESS_EVENT:
                        deleteItem(position);
                        break;
                    default:
                }
                return;
            }
        }
    }

    private void keepScrollWithinBounds() {

        mScrollPosition = mScrollPosition < 0 ? 0 : mScrollPosition;
        mScrollPosition = mScrollPosition > mAdapter.getCount() - 1
                ? mAdapter.getCount() - 1 : mScrollPosition;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mGestureDetector.onTouchEvent(event);
        return true;
    }

    private class GestureDetectorListener
            extends android.view.GestureDetector.SimpleOnGestureListener {


        @Override
        public boolean onDown(MotionEvent e) {
            try {
                mFlingThread.interrupt();
            } catch (Exception ex) {
                // Thread probably null
            }
            try {
                mSnapThread.interrupt();
            } catch (Exception ex) {
                // Thread probably null
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            delegateClick(e.getX(), e.getY(), SINGLE_TAP_EVENT);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            delegateClick(e.getX(), e.getY(), LONG_PRESS_EVENT);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            mScrollPosition += (distanceY / PIXEL_TO_POSITION_RATIO);
            keepScrollWithinBounds();
            requestLayout();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            mScrollVelocity = -velocityY / 80000;
            mFlingThread = new FlingThread();
            mFlingThread.start();
            return true;
        }

    }

    private class FlingThread extends Thread {

        @Override
        public void run() {

            while (Math.abs(mScrollVelocity) > FLING_VELOCITY_THRESHOLD) {
                mScrollVelocity *= 0.99f;

                mScrollPosition += mScrollVelocity;
                keepScrollWithinBounds();
                post(new RequestLayoutRunnable());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
            mSnapThread = new SnapThread(Math.round(mScrollPosition));
            mSnapThread.start();
        }
    }

    private class SnapThread extends Thread {

        int mSnapPosition;

        public SnapThread(int snapPosition) {
            mSnapPosition = snapPosition;
        }

        @Override
        public void run() {

            while (Math.abs((mSnapPosition - mScrollPosition)) > 0.001) {
                mScrollVelocity = (mSnapPosition - mScrollPosition) / 10;
                mScrollPosition += mScrollVelocity;
                post(new RequestLayoutRunnable());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
            mScrollPosition = mSnapPosition;
        }
    }

    private class DeleteThread extends Thread {

        public DeleteThread(int position) {
            mPositionDeleting = position;
        }

        @Override
        public void run() {

            while (mDeleteAnimationProgress < 1) {

                mDeleteAnimationProgress += 0.02;
                post(new RequestLayoutRunnable());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.v("", "ran delete thread");
            post(new Runnable() {
                     public void run() {
                         mListener.onItemLongPress(mAdapter.getItemId(mPositionDeleting));
                         removeAllViews();
                         requestLayout();
                         mDeleteAnimationProgress = 0;
                         mPositionDeleting = -1;
                         mScrollPosition += 0.5f;
                     }
                 });
        }
    }

    private class RequestLayoutRunnable implements Runnable {

        @Override
        public void run() {
            requestLayout();
        }
    }
}
