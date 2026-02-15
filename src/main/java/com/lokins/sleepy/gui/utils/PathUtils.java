package com.lokins.sleepy.gui.utils;

import java.io.File;

public class PathUtils {
    private static final String DATA_DIR = System.getProperty("user.home") + File.separator + ".sleepy";

    public static String getDataPath(String... subPaths) {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs(); // 自动创建 .sleepy 文件夹

        StringBuilder fullPath = new StringBuilder(DATA_DIR);
        for (String sub : subPaths) {
            fullPath.append(File.separator).append(sub);
        }
        return fullPath.toString();
    }
}