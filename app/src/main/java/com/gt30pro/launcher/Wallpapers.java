package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

/** Pemuat wallpaper default (render Infinix GT 30 Pro) dari folder assets. */
public final class Wallpapers {

    private static final String ASSET_NAME = "wallpaper_gt30pro.png";

    private Wallpapers() {
    }

    public static Bitmap load(Context context) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(context.getAssets().open(ASSET_NAME), null, bounds);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= dm.widthPixels
                    && bounds.outHeight / (sample * 2) >= dm.heightPixels) {
                sample *= 2;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            return BitmapFactory.decodeStream(context.getAssets().open(ASSET_NAME), null, opts);
        } catch (Exception e) {
            return null;
        }
    }
}
