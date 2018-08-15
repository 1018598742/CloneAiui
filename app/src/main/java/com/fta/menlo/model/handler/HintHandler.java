package com.fta.menlo.model.handler;

import android.text.TextUtils;

import com.fta.menlo.chat.ChatViewModel;
import com.fta.menlo.model.data.SemanticResult;
import com.fta.menlo.ui.common.PermissionChecker;

/**
 * 拒识（rc = 4）结果处理
 */

public class HintHandler extends IntentHandler {
    private final StringBuilder defaultAnswer;

//    public HintHandler(ChatViewModel model, PlayerViewModel player, PermissionChecker checker, SemanticResult result) {
//        super(model, player, checker, result);
//        defaultAnswer = new StringBuilder();
//        defaultAnswer.append("你好，我不懂你的意思");
//        defaultAnswer.append(IntentHandler.NEWLINE_NO_HTML);
//        defaultAnswer.append(IntentHandler.NEWLINE_NO_HTML);
//        defaultAnswer.append("在后台添加更多技能让我变得更强大吧 :D");
//    }

    public HintHandler(ChatViewModel model, PermissionChecker checker, SemanticResult result) {
        super(model, checker, result);
        defaultAnswer = new StringBuilder();
        defaultAnswer.append("你好，我不懂你的意思");
        defaultAnswer.append(IntentHandler.NEWLINE_NO_HTML);
        defaultAnswer.append(IntentHandler.NEWLINE_NO_HTML);
        defaultAnswer.append("在后台添加更多技能让我变得更强大吧 :D");
    }

    @Override
    public String getFormatContent() {
        if (TextUtils.isEmpty(result.answer)) {
            return defaultAnswer.toString();
        } else {
            return result.answer;
        }
    }
}
