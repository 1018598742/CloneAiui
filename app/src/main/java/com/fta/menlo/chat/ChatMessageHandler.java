package com.fta.menlo.chat;


import android.util.Log;

import com.fta.menlo.model.data.SemanticResult;
import com.fta.menlo.model.handler.DefaultHandler;
import com.fta.menlo.model.handler.HintHandler;
import com.fta.menlo.model.handler.IntentHandler;
import com.fta.menlo.model.handler.WeatherHandler;
import com.fta.menlo.model.handler.personality.DishSkillHandler;
import com.fta.menlo.model.handler.personality.DynamicEntityHandler;
import com.fta.menlo.model.handler.personality.OrderMenuHandler;
import com.fta.menlo.model.handler.personality.TelephoneHandler;
import com.fta.menlo.repository.TtsRepository;
import com.fta.menlo.ui.common.PermissionChecker;
import com.zzhoujay.richtext.callback.OnUrlClickListener;
import com.zzhoujay.richtext.callback.OnUrlLongClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 聊天消息处理类
 * <p>
 * 通过service将解析分配到不同的IntentHandler，通过getFormatMessage返回处理后的格式化内容
 */

public class ChatMessageHandler implements OnUrlClickListener, OnUrlLongClickListener {
    private static final String TAG = "ChatMessageHandler";

    private static final String KEY_SEMANTIC = "semantic";
    private static final String KEY_OPERATION = "operation";
    private static final String SLOTS = "slots";
    private static Map<String, Class> handlerMap = new HashMap<>();

    static {
        handlerMap.put("FOOBAR.DishSkill", DishSkillHandler.class);
        handlerMap.put("FOOBAR.MenuSkill", OrderMenuHandler.class);
//        handlerMap.put("musicX", MusicHandler.class);
//        handlerMap.put("cmd", MusicHandler.class);
//        handlerMap.put("story", StoryHandler.class);
//        handlerMap.put("joke", JokeHandler.class);
//        handlerMap.put("radio", RadioDisposer.class);
        handlerMap.put("telephone", TelephoneHandler.class);
        handlerMap.put("weather", WeatherHandler.class);
        handlerMap.put("dynamic", DynamicEntityHandler.class);
        handlerMap.put("unknown", HintHandler.class);
//        handlerMap.put("notification", NotificationHandler.class);
    }


    private ChatViewModel mViewModel;
    //    private PlayerViewModel mPlayer;
    private PermissionChecker mPermissionChecker;
    private RawMessage mMessage;
    private SemanticResult parsedSemanticResult;
    private IntentHandler mHandler;

//    public ChatMessageHandler(ChatViewModel viewModel, PlayerViewModel player, PermissionChecker checker, RawMessage message) {
//        this.mViewModel = viewModel;
//        this.mPlayer = player;
//        this.mPermissionChecker = checker;
//        this.mMessage = message;
//    }

    public ChatMessageHandler(ChatViewModel viewModel, PermissionChecker checker, RawMessage message) {
        this.mViewModel = viewModel;
        this.mPermissionChecker = checker;
        this.mMessage = message;
    }

    public String getFormatMessage() {
        if (mMessage.fromType == RawMessage.FromType.USER) {
            //用户消息
            if (mMessage.msgType == RawMessage.MsgType.TEXT) {
                return new String(mMessage.msgData);
            } else {
                return "";
            }
        } else {
            //AIUI消息
            initHandler();
            if (mHandler != null) {

                return mHandler.getFormatContent();
            } else {
                return "错误";
            }
        }

    }

    @Override
    public boolean urlClicked(String url) {
        initHandler();
        return mHandler != null && mHandler.urlClicked(url);
    }

    @Override
    public boolean urlLongClick(String url) {
        initHandler();
        return mHandler != null && mHandler.urlLongClick(url);
    }

    private void initHandler() {
        Log.i(TAG, "ChatMessageHandler-initHandler: 初始化行为");
        if (mMessage.fromType == RawMessage.FromType.USER) {
            return;
        }

        initSemanticResult();

        if (mHandler == null) {
            Log.i(TAG, "ChatMessageHandler-initHandler: 是哪个服务：" + parsedSemanticResult.service);
            //根据语义结果的service查找对应的IntentHandler，并实例化
            Class handlerClass = handlerMap.get(parsedSemanticResult.service);
            if (handlerClass == null) {
                Log.i(TAG, "ChatMessageHandler-initHandler: 取到的类是空");
                handlerClass = DefaultHandler.class;
            }
            try {
                //反射获取构造
                Constructor constructor = handlerClass.getConstructor(ChatViewModel.class, PermissionChecker.class, SemanticResult.class);
                //获取实例
                mHandler = (IntentHandler) constructor.newInstance(mViewModel, mPermissionChecker, parsedSemanticResult);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void initSemanticResult() {
        if (parsedSemanticResult != null) return;
        // 解析语义结果
        JSONObject semanticResult;
        parsedSemanticResult = new SemanticResult();
        try {
            semanticResult = new JSONObject(new String(mMessage.msgData));
            parsedSemanticResult.rc = semanticResult.optInt("rc");
            if (parsedSemanticResult.rc == 4) {
                parsedSemanticResult.service = "unknown";
            } else if (parsedSemanticResult.rc == 1) {
                parsedSemanticResult.service = semanticResult.optString("service");
                parsedSemanticResult.answer = "语义错误";
            } else {
                parsedSemanticResult.service = semanticResult.optString("service");
                parsedSemanticResult.answer = semanticResult.optJSONObject("answer") == null ?
                        "已为您完成操作" : semanticResult.optJSONObject("answer").optString("text");
                // 兼容3.1和4.0的语义结果，通过判断结果最外层的operation字段
                boolean isAIUI3_0 = semanticResult.has(KEY_OPERATION);
                if (isAIUI3_0) {
                    //将3.1语义格式的语义转换成4.1
                    JSONObject semantic = semanticResult.optJSONObject(KEY_SEMANTIC);
                    if (semantic != null) {
                        JSONObject slots = semantic.optJSONObject(SLOTS);
                        JSONArray fakeSlots = new JSONArray();
                        Iterator<String> keys = slots.keys();
                        while (keys.hasNext()) {
                            JSONObject item = new JSONObject();
                            String name = keys.next();
                            item.put("name", name);
                            item.put("value", slots.get(name));

                            fakeSlots.put(item);
                        }

                        semantic.put(SLOTS, fakeSlots);
                        semantic.put("intent", semanticResult.optString(KEY_OPERATION));
                        parsedSemanticResult.semantic = semantic;
                    }
                } else {
                    parsedSemanticResult.semantic = semanticResult.optJSONArray(KEY_SEMANTIC) == null ?
                            semanticResult.optJSONObject(KEY_SEMANTIC) :
                            semanticResult.optJSONArray(KEY_SEMANTIC).optJSONObject(0);
                }
                parsedSemanticResult.answer = parsedSemanticResult.answer.replaceAll("\\[[a-zA-Z0-9]{2}\\]", "");
                parsedSemanticResult.data = semanticResult.optJSONObject("data");
            }
        } catch (JSONException e) {
            parsedSemanticResult.rc = 4;
            parsedSemanticResult.service = "unknown";
        }
    }
}
