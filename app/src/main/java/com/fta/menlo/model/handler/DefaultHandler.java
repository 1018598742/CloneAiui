package com.fta.menlo.model.handler;

import android.text.TextUtils;

import com.fta.menlo.chat.ChatViewModel;
import com.fta.menlo.model.data.SemanticResult;
import com.fta.menlo.ui.common.PermissionChecker;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * 默认处理类，播报answer，播放data中符合格式的可播放内容
 *
 */

public class DefaultHandler extends IntentHandler {
//    public DefaultHandler(ChatViewModel model, PlayerViewModel player, PermissionChecker checker, SemanticResult result) {
//        super(model, player, checker, result);
//    }

    public DefaultHandler(ChatViewModel model, PermissionChecker checker, SemanticResult result) {
        super(model, checker, result);
    }

    @Override
    public String getFormatContent() {
        if(result.data != null) {
//            List<AIUIPlayer.SongInfo> songList = new ArrayList<>();
            JSONArray list = result.data.optJSONArray("result");
            if(list != null){
                for(int index = 0; index < list.length(); index++){
                    JSONObject item = list.optJSONObject(index);
                    int contentType = item.optInt("type", -1);
                    switch (contentType) {
                        //文本内容
                        case 0:{
                            //显示第一条结果
                            result.answer += NEWLINE_NO_HTML + NEWLINE_NO_HTML + item.optString("content");
                            break;
                        }
                        //音频内容(1) 视频内容(2)
                        case 1:
                        case 2: {
                            String audioPath = item.optString("url");
                            String songname = item.optString("title");
                            if(TextUtils.isEmpty(songname)) {
                                songname = item.optString("name");
                            }

//                            songList.add(new AIUIPlayer.SongInfo(songname, songname, audioPath));
                            break;
                        }
                    }
                }
            }

//            if(songList.size() != 0) {
//                if(isTTSEnable()){
//                    mPlayer.setPreSongList(songList);
//                }else {
//                    mPlayer.playList(songList);
//                }
//
//                if(isNeedShowControlTip()) {
//                    result.answer = result.answer + NEWLINE_NO_HTML + NEWLINE_NO_HTML + CONTROL_TIP;
//                }
//            }
        }

        return result.answer;

    }
}
