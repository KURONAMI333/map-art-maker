package com.kuronami.mapartmaker.client;

import com.kuronami.mapartmaker.network.MapArtFeedbackPayload;

import net.minecraft.network.chat.Component;

/**
 * 直近の生成結果を1件だけ保持する。開いている画面が無い時に届いても捨てず、
 * 次に画面が開いた時に読めるようにするための最小の受け皿。
 */
public final class MapArtFeedbackHolder {

    private static volatile Component message = Component.empty();
    private static volatile boolean success;

    private MapArtFeedbackHolder() {
    }

    public static void accept(MapArtFeedbackPayload payload) {
        accept(payload.success(), payload.message());
    }

    /** For feedback that never reaches the server, such as a file-drop that failed to decode locally. */
    public static void accept(boolean isSuccess, Component newMessage) {
        message = newMessage;
        success = isSuccess;
    }

    public static Component message() {
        return message;
    }

    public static boolean success() {
        return success;
    }

    public static void clear() {
        message = Component.empty();
        success = false;
    }
}
