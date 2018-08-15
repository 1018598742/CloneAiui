package com.fta.menlo.chat;


import com.fta.menlo.databinding.ChatItemBinding;

/**
 * 聊天交互内容点击监听
 */
public interface ItemListener {
    public void onMessageClick(ChatMessage msg, ChatItemBinding binding);
}
