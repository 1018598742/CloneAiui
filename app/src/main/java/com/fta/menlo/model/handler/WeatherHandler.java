package com.fta.menlo.model.handler;

import android.Manifest;
import android.util.Log;

import com.fta.menlo.chat.ChatViewModel;
import com.fta.menlo.model.data.SemanticResult;
import com.fta.menlo.ui.common.PermissionChecker;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 天气技能处理，高德地图API调用和上传位置信息示例
 */

public class WeatherHandler extends IntentHandler {

    private static final String TAG = "WeatherHandler";

    private static boolean notified = false;
//    public WeatherHandler(ChatViewModel model, PlayerViewModel player, PermissionChecker checker, SemanticResult result) {
//        super(model, player, checker, result);
//    }


    public WeatherHandler(ChatViewModel model, PermissionChecker checker, SemanticResult result) {
        super(model, checker, result);
    }

    @Override
    public String getFormatContent() {
        Log.i(TAG, "WeatherHandler-getFormatContent: 获取天气行为的格式化内容");
        if (notified) {
            return result.answer;
        } else {
            StringBuilder answer = new StringBuilder(result.answer);
            JSONArray slots = result.semantic.optJSONArray("slots");
            for (int index = 0; index < slots.length(); index++) {
                JSONObject item = slots.optJSONObject(index);
                if (item.optString("name").contains("location")) {
                    // 问法意图中不包含具体的城市名，提示可使用定位让城市信息更准确
                    if (item.optString("value").contains("CURRENT_CITY")) {
                        answer.append(NEWLINE);
                        answer.append(NEWLINE);
                        answer.append("<a href=\"use_loc\">使用定位让天气信息更准确</a>");
                        notified = true;
                        break;
                    }
                }
            }
            return answer.toString();
        }
    }

    @Override
    public boolean urlClicked(String url) {
        if ("use_loc".equals(url)) {
            mPermissionChecker.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, new Runnable() {
                @Override
                public void run() {
                    //上传手机位置信息
                    mMessageViewModel.useLocationData();
                }
            }, null);
        }

        return true;
    }
}
