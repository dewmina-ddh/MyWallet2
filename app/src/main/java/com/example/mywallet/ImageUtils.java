package com.example.mywallet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

/**
 * ImageUtils provides helper functions to crop and mask bitmaps into smooth circular avatars.
 */
public class ImageUtils {

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        int xOffset = (width - size) / 2;
        int yOffset = (height - size) / 2;
        Rect srcRect = new Rect(xOffset, yOffset, xOffset + size, yOffset + size);
        Rect destRect = new Rect(0, 0, size, size);
        canvas.drawBitmap(bitmap, srcRect, destRect, paint);

        return output;
    }
}
