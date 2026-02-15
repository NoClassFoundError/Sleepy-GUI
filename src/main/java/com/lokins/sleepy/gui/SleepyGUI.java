package com.lokins.sleepy.gui;

import com.lokins.sleepy.gui.utils.ConfigManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class SleepyGUI extends Application {
    private static final Logger logger = LoggerFactory.getLogger(SleepyGUI.class);
    private Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;

        // 1. 关键配置：防止 JavaFX 在最后一个窗口关闭时退出进程
        Platform.setImplicitExit(false);

        // 2. 解析启动参数：判定是否为静默启动
        Parameters params = getParameters();
        boolean startMinimized = params.getRaw().contains("--minimized");

        // 3. 初始化系统托盘 (AWT 线程)
        createTrayIcon();

        // 4. 加载 JavaFX 界面
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lokins/sleepy/gui/main-view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        // 设置图标和基础属性
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
        stage.setTitle("Sleepy Client");
        stage.setScene(scene);

        // 5. 根据参数决定是否显示主窗口
        if (startMinimized) {
            logger.info("检测到 --minimized 参数，程序将以静默模式启动到托盘。");
            // 注意：此时不调用 stage.show()
        } else {
            stage.show();
            stage.toFront();
        }

        // 6. 关闭请求监听：处理“最小化到托盘”还是“直接退出”
        stage.setOnCloseRequest(event -> {
            event.consume(); // 拦截默认关闭行为
            handleCloseRequest();
        });
    }

    /**
     * 处理窗口关闭逻辑
     */
    private void handleCloseRequest() {
        try {
            ConfigManager config = ConfigManager.getInstance();
            boolean shouldMinimize = Boolean.parseBoolean(config.get("settings", "minimize_to_tray", "true"));

            if (shouldMinimize) {
                stage.hide();
                logger.info("窗口已隐藏至系统托盘。");
            } else {
                logger.info("正在退出程序...");
                shutdown();
            }
        } catch (IOException e) {
            logger.error("读取配置失败，执行安全退出。");
            shutdown();
        }
    }

    /**
     * 彻底关闭程序的方法
     */
    private void shutdown() {
        Platform.exit();
        System.exit(0);
    }

    private void createTrayIcon() {
        if (!SystemTray.isSupported()) {
            logger.warn("当前系统不支持系统托盘。");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                URL imageURL = getClass().getResource("/images/icon.png");
                java.awt.Image image = Toolkit.getDefaultToolkit().getImage(imageURL);

                PopupMenu popup = new PopupMenu();
                MenuItem showItem = new MenuItem("Open Window");
                MenuItem exitItem = new MenuItem("Exit");

                showItem.addActionListener(e -> Platform.runLater(() -> {
                    stage.show();
                    stage.toFront();
                }));

                exitItem.addActionListener(e -> shutdown());

                popup.add(showItem);
                popup.addSeparator();
                popup.add(exitItem);

                TrayIcon trayIcon = new TrayIcon(image, "Sleepy Client", popup);
                trayIcon.setImageAutoSize(true);

                // 双击托盘图标打开窗口
                trayIcon.addActionListener(e -> Platform.runLater(() -> {
                    stage.show();
                    stage.toFront();
                }));

                tray.add(trayIcon);
            } catch (Exception e) {
                logger.error("创建托盘图标失败", e);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}