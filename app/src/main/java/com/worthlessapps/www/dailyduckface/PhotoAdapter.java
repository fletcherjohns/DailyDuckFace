package com.worthlessapps.www.dailyduckface;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by pablo on 8/04/15.
 */
public class PhotoAdapter extends BaseAdapter {

    private Context mContext;
    private Cursor mCursor;

    public PhotoAdapter(Context context, Cursor cursor) {

        mContext = context;
        mCursor = cursor;
    }

    @Override
    public int getCount() {

        return mCursor.getCount();
    }

    @Override
    public Cursor getItem(int position) {

        if (mCursor.moveToPosition(position)) {
            return mCursor;
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {

        if (mCursor.moveToPosition(position)) {
            return mCursor.getLong(mCursor.getColumnIndex(DatabaseHelper._ID));
        } else {
            return -1;
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        Holder holder;
        if (view == null) {
            view = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.list_row, parent, false);
            holder = new Holder();
            holder.image = (ImageView) view.findViewById(R.id.image_view);
            view.setTag(R.string.view_tag_holder, holder);
        } else {
            holder = (Holder) view.getTag(R.string.view_tag_holder);
        }
        view.setTag(R.string.view_tag_position, position);
        String imagePath = getItem(position).getString(mCursor
                .getColumnIndex(DatabaseHelper.THUMBNAIL_PATH));
        holder.image.setTag(imagePath);
        new SetImageTask().execute(holder.image);
        return view;
    }

    class Holder {
        ImageView image;
    }

    private class SetImageTask extends AsyncTask<ImageView, Integer, Bitmap> {

        private ImageView mImageView;
        private String mImagePath;
        @Override
        protected Bitmap doInBackground(ImageView... params) {

            mImageView = params[0];
            mImagePath = (String) mImageView.getTag();
            return BitmapFactory.decodeFile(mImagePath);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (mImagePath != (mImageView.getTag())){
                return;
            }
            mImageView.setImageBitmap(bitmap);

            mImageView.invalidate();
        }
    }
}