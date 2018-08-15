package com.fta.menlo.fragment;

import android.Manifest;
import android.app.Activity;
import android.arch.core.util.Function;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.fta.menlo.R;
import com.fta.menlo.chat.ChatMessage;
import com.fta.menlo.chat.ChatViewModel;
import com.fta.menlo.chat.MessageListAdapter;
import com.fta.menlo.chat.RawMessage;
import com.fta.menlo.chat.ScrollSpeedLinearLayoutManger;
import com.fta.menlo.common.Constant;
import com.fta.menlo.databinding.ChatFragmentBinding;
import com.fta.menlo.model.handler.IntentHandler;
import com.fta.menlo.ui.common.PermissionChecker;
import com.fta.menlo.ui.common.widget.PopupWindowFactory;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.CompositePermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class ChatFragment extends Fragment implements PermissionChecker {

    private static final String TAG = "ChatFragment";

    public static final Pattern emptyPattern = Pattern.compile("^\\s+$", Pattern.DOTALL);

    private ChatFragmentBinding mChatBinding;
    private MessageListAdapter mMsgAdapter;

    //当前状态，取值参考Constant中STATE定义
    private int mState;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private ChatViewModel mMessageModel;
    private ImageView VolumeView;
    //按住录音动画控制类
    private PopupWindowFactory mVoicePop;
    //是否检测到前端点，提示 ’为说话‘ 时判断使用
    private boolean mVadBegin = false;

    //当前所有交互消息列表
    protected List<ChatMessage> mInteractMessages;

    //唤醒波浪动画
    private boolean mWaveAnim = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        AndroidSupportInjection.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mChatBinding = DataBindingUtil.inflate(inflater, R.layout.chat_fragment, container, false);
        return mChatBinding.getRoot();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Dexter.withActivity(getActivity())
                .withPermissions(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                mMessageModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(ChatViewModel.class);

                if (!report.areAllPermissionsGranted()) {
                    //请重启应用允许请求的权限
                    mMessageModel.fakeAIUIResult(0, "permsisson", "请重启应用允许请求的权限");
                }

                //所有权限通过，初始化界面
                onPermissionChecked();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();

    }


    @Override
    public void onResume() {
        super.onResume();
        mChatBinding.visualizer.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mChatBinding.visualizer.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChatBinding.visualizer.release();
    }

    @CallSuper
    protected void onPermissionChecked() {
        initChatView();
        initSendAction();
        initVoiceAction();
        setInputState(Constant.STATE_VOICE);
    }


    private void initChatView() {
        //初始化交互消息展示列表
        ScrollSpeedLinearLayoutManger layout = new ScrollSpeedLinearLayoutManger(getActivity());
        layout.setSpeedSlow();
        layout.setStackFromEnd(true);

        mChatBinding.chatList.setLayoutManager(layout);
        mMsgAdapter = new MessageListAdapter(this);
        mChatBinding.chatList.setAdapter(mMsgAdapter);
        mChatBinding.chatList.setClipChildren(true);
        mChatBinding.chatList.setVerticalScrollBarEnabled(true);
        mChatBinding.chatList.getItemAnimator().setChangeDuration(0);

        mMsgAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                mChatBinding.chatList.smoothScrollToPosition(positionStart);
            }
        });


        //获取交互消息，更新展示
        Transformations.map(mMessageModel.getInteractMessages(), new Function<List<RawMessage>, List<ChatMessage>>() {
            @Override
            public List<ChatMessage> apply(List<RawMessage> input) {
                List<ChatMessage> interactMessages = new ArrayList<>();
                for (RawMessage message : input) {
                    interactMessages.add(new ChatMessage(message, ChatFragment.this, mMessageModel));
                }
                return interactMessages;
            }
        }).observe(this, new Observer<List<ChatMessage>>() {
            @Override
            public void onChanged(@Nullable List<ChatMessage> chatMessages) {
                Log.i(TAG, "ChatFragment-onChanged: 聊天信息大小：" + chatMessages.size());
                mInteractMessages = chatMessages;
                mMsgAdapter.replace(chatMessages);
                mChatBinding.executePendingBindings();

                //无任何消息的交互提示
                if (chatMessages.size() == 0) {
                    StringBuilder hello = new StringBuilder();
                    hello.append("你好，很高兴见到你 :D");
                    hello.append(IntentHandler.NEWLINE_NO_HTML);
                    hello.append(IntentHandler.NEWLINE_NO_HTML);
                    hello.append("你可以文本或者语音跟我对话");
                    mMessageModel.fakeAIUIResult(0, "hello", hello.toString());
                }
            }
        });


        //监听VAD事件获取音量及前端后端点事件
        mMessageModel.getVADEvent().observe(this, new Observer<AIUIEvent>() {
            @Override
            public void onChanged(@Nullable AIUIEvent aiuiEvent) {
                if (aiuiEvent.eventType == AIUIConstant.EVENT_VAD) {
                    switch (aiuiEvent.arg1) {
                        case AIUIConstant.VAD_BOS:
                            mVadBegin = true;
                            break;

                        //前端点超时消息
                        case 3:
                        case AIUIConstant.VAD_EOS:
                            //唤醒状态下检测到后端点自动进入待唤醒模式
                            if (mState == Constant.STATE_WAKEUP) {
                                onWaitingWakeUp();
                            }
                            break;


                        //音量消息
                        case AIUIConstant.VAD_VOL:
                            int level = 5000 + 8000 * aiuiEvent.arg2 / 100;

                            //更新居中的音量信息
                            if (VolumeView != null && VolumeView.getDrawable().setLevel(level)) {
                                VolumeView.getDrawable().invalidateSelf();
                            }

                            //唤醒状态下更新底部的音量波浪动画
                            if (mState == Constant.STATE_WAKEUP) {
                                mChatBinding.visualizer.setVolume(level);
                            }
                    }
                }
            }
        });
    }

    private void onWakeUp() {
        //唤醒自动停止播放
//        mPlayerViewModel.pause();
        setInputState(Constant.STATE_WAKEUP);
        if (!mWaveAnim) {
            //底部音量动画
            mChatBinding.visualizer.startAnim();
            mWaveAnim = true;
        }
    }


    private void onWaitingWakeUp() {
        //进入待唤醒状态
        setInputState(Constant.STATE_WAITING_WAKEUP);
        mChatBinding.visualizer.stopAnim();
        mWaveAnim = false;
    }

    private void initSendAction() {
        //文本语义按钮监听
        mChatBinding.emotionSend.setOnClickListener((view) -> {
            doSend();
        });

        mChatBinding.emotionSend.setOnKeyListener((view, keyCode, keyEvent) -> {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                doSend();
                return true;
            }
            return false;
        });
    }

    private void doSend() {
        //文本语义
        String msg = mChatBinding.editText.getText().toString().trim();
        if (!TextUtils.isEmpty(msg) && !emptyPattern.matcher(msg).matches()) {
            mMessageModel.sendText(msg);
            mChatBinding.editText.setText("");
        } else {
            Toast.makeText(getContext(), "发送内容不能为空", Toast.LENGTH_SHORT).show();
        }
    }

    private void initVoiceAction() {
        //根据左下角图标切换输入状态
        mChatBinding.emotionVoice.setOnClickListener((view) -> {
            setInputState(mState == Constant.STATE_VOICE ? Constant.STATE_TEXT : Constant.STATE_VOICE);
            //隐藏键盘
            dismissKeyboard(view.getWindowToken());
        });

        //初始化居中显示的按住说话动画
        View view = View.inflate(getActivity(), R.layout.layout_microphone, null);
        VolumeView = view.findViewById(R.id.iv_recording_icon);

        mVoicePop = new PopupWindowFactory(getActivity(), view);

        //按住说话按钮
        mChatBinding.voiceText.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mMessageModel.stopTTS();
                    mVadBegin = false;
//                    mPlayerViewModel.pause();
                    mChatBinding.voiceText.setPressed(true);
                    if (mChatBinding.voiceText.isPressed()) {
                        mVoicePop.showAtLocation(v, Gravity.CENTER, 0, 0);
                        setInputState(Constant.STATE_VOICE_INPUTTING);
                        mMessageModel.startRecord();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (!mVadBegin) {
                        Toast.makeText(getContext(), "您好像并没有开始说话", Toast.LENGTH_LONG).show();
                    }
                    mChatBinding.voiceText.setPressed(false);
                    mVoicePop.dismiss();
                    setInputState(Constant.STATE_VOICE);
                    mMessageModel.stopRecord();
                    break;
            }
            return true;
        });


        mVoicePop.getPopupWindow().setOnDismissListener(() -> {
            mChatBinding.voiceText.setPressed(false);
            mMessageModel.stopRecord();
        });


    }

    private void dismissKeyboard(IBinder windowToken) {
        Activity activity = getActivity();
        if (activity != null) {
            InputMethodManager imm = ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE));
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }

    private void setInputState(int state) {
        mState = state;
        mChatBinding.setState(state);
        mChatBinding.executePendingBindings();
    }

    @Override
    public void checkPermission(String permission, Runnable success, Runnable failed) {
        Dexter.withActivity(getActivity()).withPermission(permission)
                .withListener(new CompositePermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        super.onPermissionGranted(response);
                        if (success != null) {
                            success.run();
                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        super.onPermissionDenied(response);
                        if (failed != null) {
                            failed.run();
                        }
                    }
                }).check();
    }
}
