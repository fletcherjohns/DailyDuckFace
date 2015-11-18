package com.worthlessapps.www.dailyduckface;

import android.app.AlarmManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

public class PhotoActivity extends ActionBarActivity {

    private DatabaseHelper mDatabaseHelper;
    private DrawableView mImageView;
    private AlarmManager mAlarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        mDatabaseHelper = new DatabaseHelper(this);
        mImageView = (DrawableView) findViewById(R.id.image_view);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        View undoButton = findViewById(R.id.button_undo);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageView.undo();
            }
        });
        Spinner colourSelector = (Spinner) findViewById(R.id.colour_selector);
        colourSelector.setAdapter(new ColourAdapter(this));
        colourSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mImageView.setPaintColour((int) view.getTag());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAlarmManager.cancel(Utils.getAlarmBroadcastIntent(this));
        setImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_save:
                Toast.makeText(this, "Saving", Toast.LENGTH_SHORT).show();
                Thread saveThread = new SaveThread();
                saveThread.start();
                return true;
            case R.id.action_cancel:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()
                + MainActivity.ALARM_INTERVAL, MainActivity.ALARM_INTERVAL,
                Utils.getAlarmBroadcastIntent(this));
    }

    private void setImage() {

        // Start a new thread to decode the image with correct dimensions and set it to the
        // DrawableView
        new Thread() {

            Bitmap mBitmap;

            @Override
            public void run() {

                // Wait here until layout is complete
                while (mImageView.getWidth() == 0);
                // Get a cursor containing the image path
                Cursor c = mDatabaseHelper.getEntry(getIntent()
                        .getLongExtra(MainActivity.EXTRA_ITEM_ID, -1));
                // This cursor should only have one line.
                if (c.moveToFirst()) {
                    // If it's not empty:
                    // Get the image path
                    String path = c.getString(c.getColumnIndex(DatabaseHelper.IMAGE_PATH));
                    // Create a set of options for decoding. These also contain results after
                    // decoding
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    // We just want the dimensions of the image, this saves memory
                    options.inJustDecodeBounds = true;
                    // Decode the image using the options
                    BitmapFactory.decodeFile(path, options);

                    // The options now tell us the width and height of the image
                    // Again to save memory, we want to sample down but don't want to overdo it.
                    // Pick the minimum of the two fractions
                    options.inSampleSize = Math.min(options.outWidth/mImageView.getWidth(),
                            options.outHeight/mImageView.getHeight());
                    // This time we actually want a bitmap
                    options.inJustDecodeBounds = false;
                    // Get the bitmap
                    mBitmap = BitmapFactory.decodeFile(path, options);
                    // Some phones don't rotate image, create a matrix to rotate if needed
                    mBitmap = Bitmap.createBitmap(mBitmap, 0, 0,
                            mBitmap.getWidth(), mBitmap.getHeight(),
                            Utils.getMatrixToProperlyRotatePhotoOnAnnoyingPhones(path), true);
                    // Set DrawableView bitmap on the UI thread.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImageView.setImageBitmap(mBitmap);
                        }
                    });
                }
            }
        }.start();
    }

    private class SaveThread extends Thread {

        @Override
        public void run() {

            Bitmap bitmap = mImageView.getImageBitmap();
            mDatabaseHelper.updateEntry(getIntent()
                    .getLongExtra(MainActivity.EXTRA_ITEM_ID, -1), bitmap);
            finish();
        }
    }
}
