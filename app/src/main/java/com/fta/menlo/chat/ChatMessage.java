package com.fta.menlo.chat;


import com.fta.menlo.ui.common.PermissionChecker;

/**
 * 聊天界面交互消息
 */

public class ChatMessage {
    private RawMessage mMsgImpl;
    private ChatMessageHandler mHandler;
    private ChatViewModel mModel;

    //    public ChatMessage(RawMessage message, PermissionChecker checker, ChatViewModel viewModel, PlayerViewModel player) {
    public ChatMessage(RawMessage message, PermissionChecker checker, ChatViewModel viewModel) {
        mModel = viewModel;
        mMsgImpl = message;
        mHandler = new ChatMessageHandler(viewModel, checker, message);
    }

    public RawMessage getMessage() {
        return mMsgImpl;
    }

    public ChatMessageHandler getHandler() {
        return mHandler;
    }

    public String getDisplayText() {
        if (mMsgImpl.cacheContent == null) {
            mMsgImpl.cacheContent = mHandler.getFormatMessage();
            mModel.updateMessage(mMsgImpl);
        }

        return mMsgImpl.cacheContent;
    }
}
