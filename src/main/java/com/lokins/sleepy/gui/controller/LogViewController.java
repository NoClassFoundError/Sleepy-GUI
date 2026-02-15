package com.lokins.sleepy.gui.controller;

import com.lokins.sleepy.gui.utils.LogManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class LogViewController {
    @FXML private TextArea logTextArea;

    // 设置最大保存行数，防止内存溢出
    private static final int MAX_LOG_LINES = 1000;

    @FXML
    public void initialize() {
        // 订阅全局日志流
        LogManager.subscribe(message -> {
            // 确保在 JavaFX UI 线程更新
            Platform.runLater(() -> {
                if (logTextArea != null) {
                    // 1. 检查当前行数
                    String[] lines = logTextArea.getText().split("\n");
                    if (lines.length > MAX_LOG_LINES) {
                        // 如果超过限制，删掉前 100 行（分批删除效率更高）
                        int endPos = 0;
                        for (int i = 0; i < 100 && i < lines.length; i++) {
                            endPos += lines[i].length() + 1;
                        }
                        logTextArea.deleteText(0, endPos);
                    }

                    // 2. 追加新日志
                    logTextArea.appendText(message);

                    // 3. 自动滚动到底部
                    logTextArea.setScrollTop(Double.MAX_VALUE);
                }
            });
        });
    }

    @FXML
    private void clearLog() {
        logTextArea.clear();
    }
}