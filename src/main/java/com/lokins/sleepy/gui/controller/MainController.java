package com.lokins.sleepy.gui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private StackPane contentArea;

    // 对应 FXML 中的 fx:id
    @FXML private ToggleButton navConnect;
    @FXML private ToggleButton navLog;
    @FXML private ToggleButton navSettings;

    private final ToggleGroup navGroup = new ToggleGroup();

    @FXML
    public void initialize() {
        // 将按钮加入 ToggleGroup，这样每次只能选中一个（HMCL风格）
        navConnect.setToggleGroup(navGroup);
        navLog.setToggleGroup(navGroup);
        navSettings.setToggleGroup(navGroup);

        // 默认选中第一个并加载
        navConnect.setSelected(true);
        loadView("/com/lokins/sleepy/gui/ConnectView.fxml");
    }

    @FXML
    private void switchView(ActionEvent event) {
        ToggleButton btn = (ToggleButton) event.getSource();
        // 只有在按钮被选中时才切换，防止重复点击取消选中
        if (!btn.isSelected()) {
            btn.setSelected(true);
            return;
        }

        String text = btn.getText();
        logger.info("切换视图到: {}", text);

        switch (text) {
            case "连接服务器" -> loadView("/com/lokins/sleepy/gui/ConnectView.fxml");
            case "实时日志" -> loadView("/com/lokins/sleepy/gui/LogView.fxml");
            case "设置" -> loadView("/com/lokins/sleepy/gui/SettingsView.fxml");
        }
    }

    /**
     * 修复 FXML 报错：添加 handleExit 方法
     */
    @FXML
    private void handleExit(ActionEvent event) {
        logger.info("程序正在退出...");
        Platform.exit();
        System.exit(0);
    }

    private void loadView(String fxmlPath) {
        try {
            // 注意：请核对这里的路径是否与你 resources 下的文件路径完全一致
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                logger.error("找不到 FXML 文件: {}", fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            logger.error("页面加载异常: {}", fxmlPath, e);
        }
    }
}