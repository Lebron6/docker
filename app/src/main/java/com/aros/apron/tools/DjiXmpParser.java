package com.aros.apron.tools;

import android.util.Log;

import com.google.gson.Gson;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DjiXmpParser {

    public static DjiMetaData parse(String xml) {

        try {

            Map<String, String> map = new HashMap<>();

            Pattern pattern = Pattern.compile("(\\S+?)=\"(.*?)\"");

            Matcher matcher = pattern.matcher(xml);

            while (matcher.find()) {

                String key = matcher.group(1);

                String value = matcher.group(2);

                // 去掉 namespace
                if (key.contains(":")) {
                    key = key.substring(key.indexOf(":") + 1);
                }

                map.put(key, value);

                Log.d("XMP", key + " = " + value);
            }

            Gson gson = new Gson();

            String json = gson.toJson(map);

            Log.d("XMP_JSON", json);

            return gson.fromJson(json, DjiMetaData.class);

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }}