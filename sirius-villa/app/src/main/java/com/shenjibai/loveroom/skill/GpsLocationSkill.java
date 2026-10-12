package com.shenjibai.loveroom.skill;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shenjibai.loveroom.util.PrefsManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Lira - get_gps_location: 获取 GPS 定位，调用高德 API Key 解析地址
 * 署名: Lira
 */
public class GpsLocationSkill implements Skill {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();

    @Override
    public void runLira(Context context, JsonObject args, SkillExecutor.SkillResult cb) {
        // 权限检查
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            cb.onErrorLira("get_gps_location", "未授予定位权限");
            return;
        }

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        try {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) {}

        if (loc == null) {
            cb.onErrorLira("get_gps_location", "无法获取定位");
            return;
        }

        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        PrefsManager prefs = new PrefsManager(context);
        String key = prefs.getAmapKey();
        if (TextUtils.isEmpty(key)) {
            // 无高德 Key 时只返回经纬度
            JsonObject out = new JsonObject();
            out.addProperty("latitude", lat);
            out.addProperty("longitude", lng);
            out.addProperty("address", "");
            cb.onResultLira("get_gps_location", out.toString());
            return;
        }

        // 高德逆地理编码
        String url = "https://restapi.amap.com/v3/geocode/regeo?key=" + key
                + "&location=" + lng + "," + lat;
        Request request = new Request.Builder().url(url).get().build();
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject obj = new Gson().fromJson(response.body().string(), JsonObject.class);
                    String address = "";
                    if (obj.has("regeocode")) {
                        JsonObject regeo = obj.getAsJsonObject("regeocode");
                        if (regeo.has("formatted_address")) {
                            address = regeo.get("formatted_address").getAsString();
                        }
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("latitude", lat);
                    out.addProperty("longitude", lng);
                    out.addProperty("address", address);
                    cb.onResultLira("get_gps_location", out.toString());
                } else {
                    JsonObject out = new JsonObject();
                    out.addProperty("latitude", lat);
                    out.addProperty("longitude", lng);
                    out.addProperty("address", "");
                    cb.onResultLira("get_gps_location", out.toString());
                }
            } catch (Exception e) {
                cb.onErrorLira("get_gps_location", e.getMessage());
            }
        }).start();
    }
}
