package com.worthlessapps.www.dailyduckface;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.media.ExifInterface;


/**
 * Created by Fletcher on 18/04/2015.
 */
public class Utils {

    public static PendingIntent getAlarmBroadcastIntent(Context context) {

        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static Matrix getMatrixToProperlyRotatePhotoOnAnnoyingPhones(String imagePath) {

        // If exif information is stored in the file, use it to create a matrix
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                default:
                    // If none of these cases are true, leave rotation at 0
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);

        return matrix;
    }
}
