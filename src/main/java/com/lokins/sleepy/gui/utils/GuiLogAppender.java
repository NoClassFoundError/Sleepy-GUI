package com.lokins.sleepy.gui.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import java.util.function.Consumer;

public class GuiLogAppender extends AppenderBase<ILoggingEvent> {
    private static Consumer<String> logConsumer;

    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        String message = String.format("[%s] %s - %s\n",
                eventObject.getLevel(),
                eventObject.getLoggerName().substring(eventObject.getLoggerName().lastIndexOf(".") + 1),
                eventObject.getFormattedMessage());

        // 分发给所有已订阅的页面（如 LogView）
        LogManager.broadcast(message);
    }
}