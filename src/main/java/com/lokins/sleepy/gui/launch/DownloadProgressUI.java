package com.lokins.sleepy.gui.launch;

import javax.swing.*;
import java.awt.*;

public class DownloadProgressUI {
    private final JFrame frame;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    public DownloadProgressUI() {
        // 设置为系统原生外观
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        frame = new JFrame("Sleepy Client - 正在准备运行环境");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 120);
        frame.setLocationRelativeTo(null); // 居中
        frame.setLayout(new BorderLayout(10, 10));

        statusLabel = new JLabel("正在检查 JavaFX 组件...", JLabel.CENTER);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);

        frame.add(panel);
    }

    public void show() { frame.setVisible(true); }
    public void close() { frame.dispose(); }

    public void updateStatus(String status, int progress) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            progressBar.setValue(progress);
        });
    }
}