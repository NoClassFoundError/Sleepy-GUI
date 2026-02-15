package com.lokins.sleepy.gui.launch;

import com.lokins.sleepy.gui.SleepyGUI;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Launcher {
    private static final String JFX_VERSION = "21.0.2";
    private static final String MAVEN_REPO = "https://repo1.maven.org/maven2/org/openjfx/";

    private static final String DATA_DIR = System.getProperty("user.home") + File.separator + ".sleepy";
    private static final String LIB_DIR = DATA_DIR + File.separator + "runtime" + File.separator + "javafx" + File.separator;

    private static final String[] MODULES = {"javafx-base", "javafx-graphics", "javafx-controls", "javafx-fxml"};

    public static void main(String[] args) {
        autoGenerateScripts();

        // 1. 检查本地环境
        try {
            Class.forName("javafx.application.Application");
            System.out.println("检测到内置 JavaFX，直接启动...");
            SleepyGUI.main(args);
            return;
        } catch (ClassNotFoundException e) {
            System.out.println("缺失 JavaFX 运行时，准备显示下载界面...");
        }

        // 2. 初始化并显示 Swing 界面
        DownloadProgressUI ui = new DownloadProgressUI();
        ui.show();

        new Thread(() -> {
            try {
                File libDir = new File(LIB_DIR);
                if (!libDir.exists()) {
                    boolean created = libDir.mkdirs();
                    System.out.println("创建运行环境目录: " + libDir.getAbsolutePath() + " -> " + created);
                }

                String osClassifier = getOsClassifier();
                OkHttpClient client = new OkHttpClient();

                int total = MODULES.length * 2;
                int current = 0;

                for (String module : MODULES) {
                    current++;
                    ui.updateStatus("正在下载组件: " + module, (current * 100) / total);
                    downloadFile(client, module, JFX_VERSION, "", libDir);

                    current++;
                    ui.updateStatus("正在下载原生驱动: " + module, (current * 100) / total);
                    downloadFile(client, module, JFX_VERSION, osClassifier, libDir);
                }

                ui.updateStatus("加载运行环境中...", 100);
                Thread.sleep(500);

                ui.close();
                launchApp(libDir, args);

            } catch (Exception e) {
                e.printStackTrace();
                ui.updateStatus("错误: " + e.getMessage(), 0);
            }
        }).start();
    }

    private static void autoGenerateScripts() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            File currentJar = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!currentJar.getName().endsWith(".jar")) return;

            String jarName = currentJar.getName();

            if (os.contains("win")) {
                File batFile = new File("启动程序.bat");
                if (!batFile.exists()) {
                    // Windows: 使用 javaw 实现无 CMD 窗口
                    String content = "@echo off\r\nstart javaw -jar \"%~dp0" + jarName + "\"\r\nexit";
                    Files.write(batFile.toPath(), content.getBytes("GBK"));
                }
            } else if (os.contains("mac")) {
                File cmdFile = new File("启动程序.command");
                if (!cmdFile.exists()) {
                    // Mac: 使用 nohup 并在启动后尝试关闭终端窗口
                    String content = "#!/bin/bash\ncd \"$(dirname \"$0\")\"\nnohup java -jar \"" + jarName + "\" > /dev/null 2>&1 &\nkillall Terminal";
                    Files.write(cmdFile.toPath(), content.getBytes());
                    cmdFile.setExecutable(true);
                }
            } else if (os.contains("nix") || os.contains("nux")) {
                File shFile = new File("start.sh");
                if (!shFile.exists()) {
                    // Linux: 纯后台运行
                    String content = "#!/bin/bash\nAPP_PATH=$(cd \"$(dirname \"$0\")\"; pwd)\nnohup java -jar \"$APP_PATH/" + jarName + "\" > /dev/null 2>&1 &";
                    Files.write(shFile.toPath(), content.getBytes());
                    shFile.setExecutable(true);
                }
            }
        } catch (Exception e) {
            System.err.println("自动生成脚本失败: " + e.getMessage());
        }
    }

    private static void downloadFile(OkHttpClient client, String module, String version, String classifier, File dir) throws Exception {
        String fileName = module + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + ".jar";
        File targetFile = new File(dir, fileName);

        if (targetFile.exists() && targetFile.length() > 0) return;

        String url = MAVEN_REPO + module + "/" + version + "/" + fileName;
        System.out.println("正在下载: " + fileName + " 至 " + dir.getAbsolutePath());

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("下载失败: " + response.code());
            try (BufferedSink sink = Okio.buffer(Okio.sink(targetFile))) {
                sink.writeAll(response.body().source());
            }
        }
    }

    private static void launchApp(File libDir, String[] args) throws Exception {
        File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        List<URL> urls = new ArrayList<>();
        urls.add(Launcher.class.getProtectionDomain().getCodeSource().getLocation());
        if (jars != null) {
            for (File jar : jars) {
                urls.add(jar.toURI().toURL());
            }
        }

        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Launcher.class.getClassLoader().getParent());
        Thread.currentThread().setContextClassLoader(classLoader);

        Class<?> mainClass = Class.forName("com.lokins.sleepy.gui.SleepyGUI", true, classLoader);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

    private static String getOsClassifier() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "win";
        if (os.contains("mac")) return "mac";
        return "linux";
    }
}