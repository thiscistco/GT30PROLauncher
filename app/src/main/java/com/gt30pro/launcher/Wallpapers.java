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
        return loadAsset(context, ASSET_NAME);
    }

    public static Bitmap loadAmberGlow(Context context) {
        return loadAsset(context, "wallpaper_amber_glow.png");
    }

    public static Bitmap loadScreenGlass(Context context) {
        return loadAsset(context, "home_screen_glass.png");
    }

    private static Bitmap loadAsset(Context context, String assetName) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream stream = context.getAssets().open(assetName)) {
                BitmapFactory.decodeStream(stream, null, bounds);
            }

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= dm.widthPixels
                    && bounds.outHeight / (sample * 2) >= dm.heightPixels) {
                sample *= 2;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            try (java.io.InputStream stream = context.getAssets().open(assetName)) {
                return BitmapFactory.decodeStream(stream, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
