package com.maghfoormalkana.fungai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
public class JSONHelper {
    public static String[] getClassNamesFromJson(Context context, String jsonFileName) {
        try {
            InputStream is = context.getAssets().open(jsonFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.names();
            String[] classNames = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                classNames[i] = jsonArray.getString(i);
            }
            return classNames;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
}
