package com.lokins.sleepy.gui.utils;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Win32WindowUtil {
    private static final Logger logger = LoggerFactory.getLogger(Win32WindowUtil.class);

    /**
     * 获取当前系统最前台活跃窗口的标题
     * @return 窗口标题，如果获取失败则返回空字符串
     */
    public static String getActiveWindowTitle() {
        // 1. 获取前台窗口句柄 (Handle)
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return "";
        }

        // 2. 准备一个缓冲区来接收标题文字
        char[] buffer = new char[1024 * 2]; // 支持长标题

        // 3. 调用 User32 接口获取标题
        int length = User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);

        if (length > 0) {
            return Native.toString(buffer);
        }

        return "";
    }
}