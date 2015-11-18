package com.worthlessapps.www.dailyduckface;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Database definition
 *
 * This SQLiteOpenHelper class defines the database and methods to access the data
 *
 * Created by pablo on 8/04/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "selfie_database";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "selfie_path_table";

    public static final String _ID = "_id";
    public static final String THUMBNAIL_PATH = "thumbnail_path";
    public static final String IMAGE_PATH = "image_path";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + THUMBNAIL_PATH + " TEXT, "
            + IMAGE_PATH + " TEXT);";

    private Context mContext;
    private SQLiteDatabase mDb;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        onCreate(db);
    }

    public long createEntry(File original) {

        // Copy the main photo to internal storage
        String fileName = String.valueOf(new Date().getTime());
        File imageFile = new File(mContext.getFilesDir(), fileName + ".png");
        File thumbFile = new File(mContext.getFilesDir(), "thumb" + fileName + ".png");
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(original);
            out = new FileOutputStream(imageFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                original.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Calculate dimensions and decode the thumbnail
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getPath(), bounds);
        if ((bounds.outWidth == -1) || (bounds.outHeight == -1)) {
            return -1;
        }
        bounds.inSampleSize = Math.max(bounds.outHeight / 400, bounds.outWidth / 400);
        bounds.inJustDecodeBounds = false;
        Bitmap thumbnail = BitmapFactory.decodeFile(imageFile.getPath(), bounds);

         /*
         On some devices, the camera app saves the image in the correct orientation, however, on
         other devices, including some Samsung phones, the orientation of a photo is added as
         Exif information. I have chosen to leave the full size image as it is for now, but to
         rotate the thumbnail if necessary before saving it. PhotoActivity must therefore
         rotate the image before passing it to the ImageView.
         */
        thumbnail = Bitmap.createBitmap(thumbnail, 0, 0,
                thumbnail.getWidth(), thumbnail.getHeight(),
                Utils.getMatrixToProperlyRotatePhotoOnAnnoyingPhones(imageFile.getPath()), true);

        // Save the thumbnail
        try {
            out = new FileOutputStream(thumbFile);
            thumbnail.compress(Bitmap.CompressFormat.PNG, 0, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.IMAGE_PATH, imageFile.getPath());
        values.put(DatabaseHelper.THUMBNAIL_PATH, thumbFile.getPath());

        mDb = getWritableDatabase();
        try {
            return mDb.insert(TABLE_NAME, null, values);
        } finally {
            close();
        }
    }

    public Cursor getAllEntries() {

        mDb = getReadableDatabase();
        String[] columns = {_ID, THUMBNAIL_PATH};
        return mDb.query(TABLE_NAME, columns, null, null, null, null, _ID);
    }

    public Cursor getEntry(long id) {

        mDb = getReadableDatabase();
        String[] columns = {IMAGE_PATH, THUMBNAIL_PATH};
        String whereClause = _ID + "=?";
        String[] whereArgs = {String.valueOf(id)};
        return mDb.query(TABLE_NAME, columns, whereClause, whereArgs, null, null, null);
    }

    public int deleteEntry(long id) {

        // Delete the image and thumbnail files
        Cursor c = getEntry(id);
        if (c.moveToFirst()) {
            new File(mContext.getFilesDir(),
                    c.getString(c.getColumnIndex(IMAGE_PATH)))
                    .delete();
            new File(mContext.getFilesDir(),
                    c.getString(c.getColumnIndex(THUMBNAIL_PATH)))
                    .delete();
        }
        // Delete the database entry
        mDb = getWritableDatabase();
        String whereClause = _ID + "=?";
        String[] whereArgs = {String.valueOf(id)};
        try {
            return mDb.delete(TABLE_NAME, whereClause, whereArgs);
        } finally {
            close();
        }
    }

    public void updateEntry(long id, Bitmap bitmap) {

        Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
        Cursor c = getEntry(id);
        if (c.moveToFirst()) {
            File imageFile = new File(c.getString(c.getColumnIndex(IMAGE_PATH)));
            File thumbnailFile = new File(c.getString(c.getColumnIndex(THUMBNAIL_PATH)));
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, new FileOutputStream(imageFile));
                thumbnail.compress(Bitmap.CompressFormat.PNG, 0, new FileOutputStream(thumbnailFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
