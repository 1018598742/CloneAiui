package com.fta.menlo.chat;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.support.annotation.Nullable;

import com.fta.menlo.repository.AIUIRepository;
import com.fta.menlo.repository.LocationRepo;
import com.fta.menlo.repository.SettingsRepo;
import com.fta.menlo.repository.Storage;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.location.result.GPSLocResult;
import com.iflytek.location.result.NetLocResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * 聊天界面模型
 */
public class ChatViewModel extends ViewModel {

    private AIUIRepository mRepository;

    private Storage mStorage;

    private SettingsRepo mSettingsRepo;

    private Context mContext;

    private LocationRepo mLocRepo;

    @Inject
    public ChatViewModel(Context context, SettingsRepo settingsRepo, Storage storage, AIUIRepository messagesRepository) {
        mContext = context;
        mSettingsRepo = settingsRepo;
        mRepository = messagesRepository;
        mStorage = storage;
    }

    //模拟AIUI结果信息，用于展示如欢迎语或者操作结果信息
    public RawMessage fakeAIUIResult(int rc, String service, String answer) {
        return mRepository.fakeAIUIResult(rc, service, answer, null, null);
    }


    //发送文本交互消息
    public void sendText(String msg) {
        mRepository.writeText(msg);
    }

    public void stopTTS() {
        mRepository.stopTTS();
    }

    //AIUI开始录音
    public void startRecord() {
        mRepository.startRecord();
    }

    //AIUI停止录音
    public void stopRecord() {
        mRepository.stopRecord();
    }

    public LiveData<List<RawMessage>> getInteractMessages() {
        return mRepository.getInteractMessages();
    }

    public LiveData<Boolean> getTTSEnableState() {
        return mSettingsRepo.getTTSEnableState();
    }

    //更新消息列表中特定的消息内容
    public void updateMessage(RawMessage msg) {
        mRepository.updateMessage(msg);
    }

    public String readAssetFile(String filename){
        return mStorage.readAssetFile(filename);
    }


    public void useLocationData() {
        mLocRepo.getNetLoc().observeForever(new Observer<NetLocResult>() {
            @Override
            public void onChanged(@Nullable NetLocResult netLoc) {
                mRepository.setLoc(netLoc.getLon(), netLoc.getLat());

                String location = String.format("net location city %s, lon %f lat %f", netLoc.getCity(), netLoc.getLon(), netLoc.getLat());
                Map<String, String> data = new HashMap<>();
                data.put("netLoc", location);
                mRepository.fakeAIUIResult(0, "fake.Loc", "已获取使用最新的网络位置信息", null, data);
            }
        });

        mLocRepo.getGPSLoc().observeForever(new Observer<GPSLocResult>() {
            @Override
            public void onChanged(@Nullable GPSLocResult gpsLoc) {
                mRepository.setLoc(gpsLoc.getLon(), gpsLoc.getLat());

                String location = String.format("GPS location lon %f lat %f", gpsLoc.getLon(), gpsLoc.getLat());
                Map<String, String> data = new HashMap<>();
                data.put("gpsLoc", location);
                mRepository.fakeAIUIResult(0, "fake.Loc", "已获取使用最新的GPS位置", null, data);
            }
        });
    }

    public void useNewAppID(String appid, String key, String scene) {
        mSettingsRepo.config(appid, key, scene);
    }


    public LiveData<AIUIEvent> getVADEvent() {
        return mRepository.getVADEvent();
    }
}
