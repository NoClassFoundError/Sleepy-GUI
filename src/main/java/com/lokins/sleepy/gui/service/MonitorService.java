package com.lokins.sleepy.gui.service;

import com.lokins.sleepy.gui.network.SleepyClient;
import com.lokins.sleepy.gui.utils.ConfigManager;
import com.lokins.sleepy.gui.utils.Win32WindowUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);

    private final SleepyClient client;
    private final Consumer<String> onAppChanged;
    private ScheduledExecutorService scheduler;

    private String lastApp = "";

    public MonitorService(SleepyClient client, Consumer<String> onAppChanged) {
        this.client = client;
        this.onAppChanged = onAppChanged;
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Sleepy-Monitor-Thread");
            t.setDaemon(true);
            return t;
        });

        // 关键改动：不再使用固定的 scheduleAtFixedRate
        // 改为调用一个自适应的调度方法
        adaptiveSchedule();
        logger.info("Monitor Service started with adaptive interval.");
    }

    /**
     * 自适应调度逻辑：每次执行完任务后，根据最新配置决定下一次什么时候执行
     */
    private void adaptiveSchedule() {
        if (scheduler == null || scheduler.isShutdown()) return;

        // 1. 执行核心检测逻辑
        checkCurrentWindow();

        // 2. 从配置管理器获取最新的间隔时间
        int delay = 5; // 默认 5 秒
        try {
            String intervalStr = ConfigManager.getInstance().get("settings", "interval", "5");
            delay = Integer.parseInt(intervalStr);
            // 限制最小值，防止用户输入 0 导致 CPU 飙升
            if (delay < 1) delay = 1;
        } catch (Exception e) {
            logger.warn("Failed to read interval config, using default 5s");
        }

        // 3. 递归调度下一次任务
        scheduler.schedule(this::adaptiveSchedule, delay, TimeUnit.SECONDS);
    }

    private void checkCurrentWindow() {
        try {
            String currentApp = Win32WindowUtil.getActiveWindowTitle();
            if (currentApp == null || currentApp.isEmpty()) return;

            if (!currentApp.equals(lastApp)) {
                logger.info("Detected app change: {}", currentApp);
                client.sendReport(currentApp);

                if (onAppChanged != null) {
                    onAppChanged.accept(currentApp);
                }
                lastApp = currentApp;
            }
        } catch (Exception e) {
            logger.error("Error during monitoring: {}", e.getMessage());
        }
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            logger.info("Monitor Service stopped.");
        }
    }
}