package com.worthlessapps.www.dailyduckface;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

/**
 * Created by Fletcher on 26/04/2015.
 */
public class ColourAdapter extends BaseAdapter {

    private Context mContext;
    private int[] mColours;

    public ColourAdapter(Context context) {
        mContext = context;
        mColours = createColourArray();
    }

    @Override
    public int getCount() {
        return mColours.length;
    }

    @Override
    public Object getItem(int position) {
        return mColours[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        int colour = mColours[position];
        View v = new ColourView(mContext, colour);
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(
                100, 100);
        v.setLayoutParams(params);
        v.setTag(colour);
        return v;
    }

    private int[] createColourArray() {

        int[] array = new int[8];
        array[0] = Color.BLACK;
        array[1] = Color.WHITE;
        array[2] = Color.RED;
        array[3] = Color.MAGENTA;
        array[4] = Color.BLUE;
        array[5] = Color.CYAN;
        array[6] = Color.GREEN;
        array[7] = Color.YELLOW;
        return array;
    }
}
