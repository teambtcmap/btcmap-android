package com.bubelov.coins.database;

import android.content.Context;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 24/02/16 21:56
 */

public class AssetSqlQuery {
    public static String EXTENSION = ".sql";

    private Context context;

    private String fileName;

    public AssetSqlQuery(Context context, String assetFileName) {
        if (!assetFileName.contains(EXTENSION)) {
            assetFileName += EXTENSION;
        }

        this.context = context;
        this.fileName = assetFileName;
    }

    @Override
    public String toString() {
        try {
            StringBuilder builder =new StringBuilder();
            InputStream input = context.getAssets().open(String.format("sql/%s", fileName));
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

            String buffer;

            while ((buffer=reader.readLine()) != null) {
                builder.append(buffer).append(" ");
            }

            reader.close();
            return builder.toString();
        } catch (Exception e) {
            Crashlytics.logException(e);
            return null;
        }
    }

    public List<String> getStatements() {
        List<String> statements = new ArrayList<>();
        String[] parts = toString().split(";");

        for (String part : parts) {
            if (!TextUtils.isEmpty(part.trim())) {
                statements.add(part);
            }
        }

        return statements;
    }
}
