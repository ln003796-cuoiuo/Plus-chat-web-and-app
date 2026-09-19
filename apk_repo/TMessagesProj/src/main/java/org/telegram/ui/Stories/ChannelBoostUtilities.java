package org.pluschat.ui.Stories;

import android.text.TextUtils;

import org.pluschapluschat.russenger.ChatObject;
import org.pluschapluschat.russenger.MessagesController;
import org.pluschat.tgnet.TLRPC;

public class ChannelBoostUtilities {
    public static String createLink(int currentAccount, long dialogId) {
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        String username = ChatObject.getPublicUsername(chat);
        if (!TextUtils.isEmpty(username)) {
            return "https://pluschat.ru/boost/" + ChatObject.getPublicUsername(chat);
        } else {
            return "https://pluschat.ru/boost/?c=" + -dialogId;
        }
    }
}
