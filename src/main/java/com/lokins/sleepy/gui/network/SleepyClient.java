package com.lokins.sleepy.gui.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepyClient {
    private static final Logger logger = LoggerFactory.getLogger(SleepyClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final String secret;
    private final String deviceName;
    private final String deviceId; // 建议增加一个 ID

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SleepyClient(String serverUrl, String secret, String deviceName) {
        this.baseUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        this.secret = secret;
        this.deviceName = deviceName;
        // 使用设备名的 hash 或处理后的字符串作为唯一 ID
        this.deviceId = deviceName.toLowerCase().replaceAll("\\s+", "-");

        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public boolean ping() {
        try {
            // 假设你的服务器有一个健康检查接口，或者直接对根路径发 GET
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful(); // 200-299 状态码返回 true
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void sendReport(String appName) {
        try {
            // 1. 按照文档构建 Payload (不包含 secret)
            ReportPayload payload = new ReportPayload(deviceId, deviceName, appName);
            String json = objectMapper.writeValueAsString(payload);

            // 2. 将 secret 放入 URL Query 中: /api/device/set?secret=xxx
            HttpUrl url = HttpUrl.parse(baseUrl + "api/device/set")
                    .newBuilder()
                    .addQueryParameter("secret", secret)
                    .build();

            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    logger.error("Network Error: {}", e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    try (response) {
                        if (response.isSuccessful()) {
                            logger.info("Report Success: {}", appName);
                        } else {
                            // 打印详细错误方便调试
                            logger.warn("Server Error: {} | Response: {}", response.code(), response.body().string());
                        }
                    }
                }
            });
        } catch (Exception e) {
            logger.error("JSON Error: {}", e.getMessage());
        }
    }
}