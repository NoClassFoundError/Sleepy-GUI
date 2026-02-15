package com.lokins.sleepy.gui.utils;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class LogManager {
    private static final CopyOnWriteArrayList<Consumer<String>> subscribers = new CopyOnWriteArrayList<>();
    // 新增：日志缓冲区，保存最近的 200 条日志
    private static final CopyOnWriteArrayList<String> logCache = new CopyOnWriteArrayList<>();
    private static final int MAX_CACHE_SIZE = 200;

    public static void subscribe(Consumer<String> consumer) {
        subscribers.add(consumer);
        // 关键：当新页面订阅时，先把缓存里的历史日志全发给它
        for (String history : logCache) {
            consumer.accept(history);
        }
    }

    public static void broadcast(String message) {
        // 保存到缓存
        logCache.add(message);
        if (logCache.size() > MAX_CACHE_SIZE) {
            logCache.remove(0);
        }

        // 分发给当前在线的订阅者
        for (Consumer<String> subscriber : subscribers) {
            subscriber.accept(message);
        }
    }
}