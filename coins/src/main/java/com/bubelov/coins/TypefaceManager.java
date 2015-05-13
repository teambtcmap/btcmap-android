package com.bubelov.coins;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypefaceManager {
    private final static Map<String, Typeface> cache = new ConcurrentHashMap<String, Typeface>();

    public static Typeface getTypeface(Context context, int id) {
        String fileName = null;

        switch (id) {
            case 0:
                fileName = "Roboto-Medium.ttf";
                break;
            case 1:
                fileName = "Roboto-Regular.ttf";
                break;
            case 2:
                fileName = "Roboto-Bold.ttf";
                break;
        }

        return getTypeface(context, fileName);
    }

    public static Typeface getTypeface(Context context, String fileName) {
        if (fileName != null) {
            if (context != null) {
                String fontPath = String.format("fonts/%s", fileName);

                if (cache.containsKey(fontPath)) {
                    return cache.get(fontPath);
                } else {
                    Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);

                    if (typeface != null) {
                        cache.put(fontPath, typeface);

                        return typeface;
                    } else {
                        throw new IllegalStateException(String.format("Could not load %s.", fontPath));
                    }
                }
            } else {
                throw new IllegalStateException("Context could not be null.");
            }
        }

        return null;
    }
}