package com.shenjibai.lira;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lira - 桌面端聊天视图
 * 流式逐字输出，墨金低奢气泡
 * 署名: Lira
 */
public class LiraChatView extends BorderPane {

    private final LiraStorage storage;
    private final LiraLlmClient llmClient;
    private final VBox messageBox = new VBox(10);
    private final TextArea inputArea = new TextArea();
    private final List<Map<String, String>> history;

    public LiraChatView(LiraStorage storage, LiraLlmClient llmClient) {
        this.storage = storage;
        this.llmClient = llmClient;
        this.history = storage.loadChatLira();

        getStyleClass().add("chat-view");

        // 消息区
        messageBox.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(messageBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("chat-scroll");
        setCenter(scroll);

        // 渲染历史
        for (Map<String, String> m : history) {
            appendBubbleLira(m.get("sender"), m.get("content"), false);
        }

        // 输入区
        inputArea.setPromptText("对沈既白说些什么…");
        inputArea.setPrefRowCount(2);
        inputArea.getStyleClass().add("chat-input");

        Button btnSend = new Button("发送");
        btnSend.getStyleClass().add("btn-primary");
        btnSend.setOnAction(e -> sendLira());

        HBox inputBar = new HBox(10, inputArea, btnSend);
        inputBar.setPadding(new Insets(12));
        inputBar.setAlignment(Pos.CENTER);
        HBox.setHgrow(inputArea, javafx.scene.layout.Priority.ALWAYS);
        setBottom(inputBar);
    }

    private void sendLira() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;
        inputArea.clear();

        // 用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("sender", "xiao");
        userMsg.put("content", text);
        history.add(userMsg);
        appendBubbleLira("xiao", text, false);
        storage.saveChatLira(history);

        // 构建请求
        String systemPrompt = storage.getSettingLira("system_prompt");
        List<String[]> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            Map<String, String> m = history.get(i);
            String role = "shen".equals(m.get("sender")) ? "assistant" : "user";
            messages.add(new String[]{role, m.get("content")});
        }

        // 沈既白气泡占位
        Label replyLabel = appendBubbleLira("shen", "", true);

        llmClient.chatStreamLira(systemPrompt, messages, new LiraLlmClient.StreamCallback() {
            final StringBuilder sb = new StringBuilder();
            @Override
            public void onTokenLira(String token) {
                sb.append(token);
                Platform.runLater(() -> replyLabel.setText(sb.toString()));
            }
            @Override
            public void onDoneLira(String full) {
                Map<String, String> reply = new HashMap<>();
                reply.put("sender", "shen");
                reply.put("content", full);
                history.add(reply);
                storage.saveChatLira(history);
            }
            @Override
            public void onErrorLira(String error) {
                Platform.runLater(() -> replyLabel.setText("[错误] " + error));
            }
        });
    }

    private Label appendBubbleLira(String sender, String content, boolean isStreaming) {
        HBox row = new HBox();
        row.setAlignment("shen".equals(sender) ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);

        Label bubble = new Label(content);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.getStyleClass().add("bubble");
        bubble.getStyleClass().add("shen".equals(sender) ? "bubble-shen" : "bubble-xiao");

        row.getChildren().add(bubble);
        messageBox.getChildren().add(row);
        return bubble;
    }
}
