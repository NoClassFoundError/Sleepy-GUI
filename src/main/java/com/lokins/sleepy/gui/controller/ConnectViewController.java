package com.lokins.sleepy.gui.controller;

import com.lokins.sleepy.gui.network.SleepyClient;
import com.lokins.sleepy.gui.service.MonitorService;
import com.lokins.sleepy.gui.utils.ConfigManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ConnectViewController {
    private static final Logger logger = LoggerFactory.getLogger(ConnectViewController.class);

    @FXML private TextField serverUrlField;
    @FXML private PasswordField secretField;
    @FXML private TextField deviceNameField;
    @FXML private Button startBtn;
    @FXML private Label statusLabel;
    @FXML private Button testConnBtn; // 必须添加这一行

    // 使用静态变量或单例，确保页面切换时服务不被销毁
    private static MonitorService monitorService;
    private static boolean isRunning = false;

    @FXML
    public void initialize() {
        // 恢复之前的配置
        try {
            ConfigManager config = ConfigManager.getInstance();
            serverUrlField.setText(config.get("server", "url", ""));
            secretField.setText(config.get("auth", "secret", ""));
            deviceNameField.setText(config.get("device", "name", "My-PC"));
        } catch (IOException e) {
            logger.error("加载配置失败", e);
        }

        // 恢复按钮状态（防止从日志页切回来时按钮变回“开始”）
        updateUIState();
    }

    @FXML
    private void handleStart() {
        try {
            ConfigManager config = ConfigManager.getInstance();
            config.set("server", "url", serverUrlField.getText());
            config.set("auth", "secret", secretField.getText());
            config.set("device", "name", deviceNameField.getText());

            // ！！！必须加这一行，否则文件不会更新 ！！！
            config.save();

            logger.info("配置已持久化到磁盘");
        } catch (IOException e) {
            logger.error("无法保存配置", e);
        }

        if (!isRunning) {
            startMonitoring();
        } else {
            stopMonitoring();
        }
    }

    private void startMonitoring() {
        String url = serverUrlField.getText();
        String secret = secretField.getText();
        String device = deviceNameField.getText();

        if (url.isEmpty() || secret.isEmpty() || device.isEmpty()) {
            logger.warn("配置不完整，取消启动");
            showAlert(Alert.AlertType.ERROR, "启动失败", "配置信息不完整。请确保服务器地址、密钥和设备名称都已填写。");
            return;
        }

        // 启动逻辑
        SleepyClient client = new SleepyClient(url, secret, device);
        monitorService = new MonitorService(client, (appName) -> {
            Platform.runLater(() -> statusLabel.setText("正在运行: " + appName));
        });

        monitorService.start();
        isRunning = true;
        updateUIState();
        logger.info(">>> 监控已启动，设备名: {}", device);
    }

    private void stopMonitoring() {
        if (monitorService != null) monitorService.stop();
        isRunning = false;
        updateUIState();
        logger.info(">>> 监控已手动停止");
    }

    private void updateUIState() {
        if (isRunning) {
            startBtn.setText("停止监控");
            startBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            statusLabel.setText("状态: 运行中");
        } else {
            startBtn.setText("开始监控");
            startBtn.setStyle(""); // 恢复 CSS 默认
            statusLabel.setText("状态: 已停止");
        }
    }

    @FXML
    private void handleTestConnection() {
        String url = serverUrlField.getText().trim();

        if (url.isEmpty()) {
            showStatus("请输入服务器地址", "red");
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            serverUrlField.setText(url);
        }

        // 禁用按钮防止重复点击
        testConnBtn.setDisable(true);
        testConnBtn.setText("测试中...");

        String finalUrl1 = url;
        new Thread(() -> {
            boolean success = false;
            try {
                // 2. 使用锁定的 url 变量
                SleepyClient testClient = new SleepyClient(finalUrl1, "", "");
                success = testClient.ping();
            } catch (Exception e) {
                System.err.println("测试连接失败: " + e.getMessage());
            }

            // 3. 回到主线程更新 UI
            final boolean finalSuccess = success;
            Platform.runLater(() -> {
                testConnBtn.setDisable(false);
                testConnBtn.setText("测试连接");

                if (finalSuccess) {
                    showAlert(Alert.AlertType.INFORMATION, "成功", "服务器连接正常！");
                } else {
                    showAlert(Alert.AlertType.ERROR, "失败", "无法连接到服务器，请检查地址或网络。");
                }
            });
        }).start();
    }

    private void showStatus(String message, String color) {
        System.out.println("Status: " + message);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void handleSaveConfig() {
        String url = serverUrlField.getText().trim();
        String secret = secretField.getText().trim();
        String deviceName = deviceNameField.getText().trim();

        if (url.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "校验失败", "服务器地址不能为空");
            return;
        }

        try {
            ConfigManager config = ConfigManager.getInstance();
            // 写入配置内存
            config.set("connection", "server_url", url);
            config.set("connection", "secret", secret);
            config.set("connection", "device_name", deviceName);

            // 执行持久化，写入 .sleepy/config.ini
            config.save();

            showAlert(Alert.AlertType.INFORMATION, "保存成功", "连接配置已持久化到本地。");
            logger.info("用户手动保存了连接配置: {}", url);
        } catch (IOException e) {
            logger.error("保存配置失败", e);
            showAlert(Alert.AlertType.ERROR, "保存失败", "无法写入配置文件: " + e.getMessage());
        }
    }
}