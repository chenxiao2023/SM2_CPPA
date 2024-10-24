package com.example.SM2_CPPA.MainUse;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonPut {



    public static JSONObject PutJson(JSONObject jsonObject, String key, String value){
        try {
            jsonObject.put(key, value);
            return jsonObject;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static JSONObject PutJson(JSONObject jsonObject, String key, int value){
        try {
            jsonObject.put(key, value);
            return jsonObject;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
