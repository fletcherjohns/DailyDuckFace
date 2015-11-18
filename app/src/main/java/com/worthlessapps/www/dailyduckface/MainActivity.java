package com.worthlessapps.www.dailyduckface;

import android.app.AlarmManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends ActionBarActivity {

    /** Tag for savedInstanceState */
    private static final String SCROLL_POSITION = "scroll_position";
    /** Request code for startActivityForResult */
    private static final int CAMERA_REQUEST_CODE = 10;
    /** File name for camera to save image to */
    private static final String TEMP_FILE = "temp_file.jpg";
    /** Tag for intent extra for PhotoActivity */
    public static final String EXTRA_ITEM_ID = "extra_item_id";
    /** Millisecond interval for alarm */
    public static final int ALARM_INTERVAL = 60000;

    private DatabaseHelper mDatabaseHelper;
    private PhotoViewGroup mListView;
    private AlarmManager mAlarmManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabaseHelper = new DatabaseHelper(this);
        mListView = (PhotoViewGroup) findViewById(R.id.list_view);
        mListView.setOnItemClickListener(new PhotoViewGroup.PhotoClickListener() {
            @Override
            public void onItemClick(long id) {
                itemClicked(id);
            }

            @Override
            public void onItemLongPress(long id) {
                itemLongPressed(id);
            }
        });
        if (savedInstanceState != null) {
            mListView.setScrollPosition(savedInstanceState.getFloat(SCROLL_POSITION));
        }
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    }

    /**
     * This method starts PhotoActivity using an intent with the item id as an extra
     * @param id the id of the item clicked
     */
    private void itemClicked(long id) {
        Intent intent = new Intent(this, PhotoActivity.class);
        Cursor c = mDatabaseHelper.getEntry(id);
        if (c.moveToFirst()) {
            intent.putExtra(EXTRA_ITEM_ID, id);
        }
        startActivity(intent);
    }

    private void itemLongPressed(long id) {
        mDatabaseHelper.deleteEntry(id);
        updateList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateList();
        mAlarmManager.cancel(Utils.getAlarmBroadcastIntent(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_camera) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Uri fileUri = Uri.fromFile(new File(getExternalFilesDir(null), TEMP_FILE));
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            } else {
                makeToast("Insert SD Card to capture photo");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()
                + ALARM_INTERVAL, ALARM_INTERVAL, Utils.getAlarmBroadcastIntent(this));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putFloat(SCROLL_POSITION, mListView.getScrollPosition());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                SaveImageThread saveThread = new SaveImageThread();
                makeToast("Creating thumbnail and saving files");
                saveThread.start();
                break;
            default:
            // No other request codes at this time
        }
    }

    private void updateList() {

        mDatabaseHelper.close();
        mListView.setAdapter(new PhotoAdapter(this, mDatabaseHelper.getAllEntries()));
    }

    private void makeToast(String string) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }

    private class SaveImageThread extends  Thread {

        public SaveImageThread() {
        }

        @Override
        public void run() {

            mDatabaseHelper.createEntry(new File(getExternalFilesDir(null), TEMP_FILE));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateList();
                    mListView.scrollToPosition(-1);
                }
            });
        }
    }


}
