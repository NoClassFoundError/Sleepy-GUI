package com.lokins.sleepy.gui.controller;

import com.lokins.sleepy.gui.utils.ConfigManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.UnaryOperator;

public class SettingsViewController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsViewController.class);

    @FXML private CheckBox autoStartCheck;
    @FXML private CheckBox silentStartCheck;
    @FXML private CheckBox minimizeToTrayCheck;
    @FXML private TextField appPathField;
    @FXML private TextField intervalField;

    @FXML
    public void initialize() {
        // 1. 限制间隔输入框只能输入数字
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("\\d*")) {
                return change;
            }
            return null;
        };
        intervalField.setTextFormatter(new TextFormatter<>(filter));

        // 2. 加载配置
        try {
            ConfigManager config = ConfigManager.getInstance();

            autoStartCheck.setSelected(Boolean.parseBoolean(config.get("settings", "autostart", "false")));
            minimizeToTrayCheck.setSelected(Boolean.parseBoolean(config.get("settings", "minimize_to_tray", "true")));
            silentStartCheck.setSelected(Boolean.parseBoolean(config.get("settings", "silent_start", "false")));

            // UI 联动：只有勾选了自启动，静默启动选项才可用
            silentStartCheck.disableProperty().bind(autoStartCheck.selectedProperty().not());

            // 优化：获取当前程序实际路径作为默认值
            String defaultPath = "";
            try {
                // 尝试获取当前运行的 JAR 文件路径
                defaultPath = new File(SettingsViewController.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).getAbsolutePath();
            } catch (Exception e) {
                defaultPath = System.getProperty("user.dir");
            }

            appPathField.setText(config.get("settings", "app_path", defaultPath));
            intervalField.setText(config.get("settings", "interval", "5"));

            logger.info("设置页面加载完成。配置读取自隐藏目录 .sleepy");
        } catch (IOException e) {
            logger.error("加载设置失败", e);
        }
    }

    @FXML
    private void saveSettings() {
        try {
            ConfigManager config = ConfigManager.getInstance();

            // 获取当前 UI 的值
            String intervalValue = intervalField.getText().isEmpty() ? "5" : intervalField.getText();
            String path = appPathField.getText();
            boolean isAutoStart = autoStartCheck.isSelected();
            boolean isSilent = silentStartCheck.isSelected();
            boolean isMinimizeToTray = minimizeToTrayCheck.isSelected();

            // 1. 保存到配置内存 (ConfigManager 现在指向 .sleepy/config.ini)
            config.set("settings", "app_path", path);
            config.set("settings", "autostart", String.valueOf(isAutoStart));
            config.set("settings", "silent_start", String.valueOf(isSilent));
            config.set("settings", "minimize_to_tray", String.valueOf(isMinimizeToTray));
            config.set("settings", "interval", intervalValue);

            // 2. 写入文件
            config.save();

            // 3. 同步系统注册表
            updateRegistry(isAutoStart, isSilent, path);

            logger.info("配置保存成功！自启动: {}, 静默: {}", isAutoStart, isSilent);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "设置已保存", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();

        } catch (Exception e) {
            logger.error("保存设置失败", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void updateRegistry(boolean enable, boolean silent, String path) {
        String appName = "SleepyClient";
        try {
            if (enable) {
                // 关键修复：Reg add 命令的 /d 参数如果包含空格，必须用双引号包裹内容
                // 最终效果应该是：reg add ... /d "\"C:\Path With Spaces\app.jar\" --minimized" /f
                String escapedPath = "\"" + path + "\"";
                String data = silent ? escapedPath + " --minimized" : escapedPath;

                // 使用 ProcessBuilder 处理复杂的命令行参数更安全
                ProcessBuilder pb = new ProcessBuilder(
                        "reg", "add", "HKEY_CURRENT_VALUE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", appName, "/t", "REG_SZ", "/d", data, "/f"
                );
                pb.start();

                logger.info("注册表自启动项已更新: {}", data);
            } else {
                Runtime.getRuntime().exec("reg delete \"HKEY_CURRENT_VALUE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v " + appName + " /f");
                logger.info("注册表自启动项已移除");
            }
        } catch (IOException e) {
            logger.error("操作注册表时发生异常", e);
        }
    }

    @FXML
    private void handleBrowsePath() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择程序执行文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("执行文件", "*.exe", "*.jar"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );

        // 如果当前路径有效，则打开其父目录
        String currentPath = appPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            File parent = currentFile.getParentFile();
            if (parent != null && parent.exists()) {
                fileChooser.setInitialDirectory(parent);
            }
        }

        File selectedFile = fileChooser.showOpenDialog(appPathField.getScene().getWindow());
        if (selectedFile != null) {
            appPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    public void handleAutoStartChange(ActionEvent actionEvent) {
        // 可以在这里做即时反馈，但目前逻辑已在 saveSettings 处理
    }
}