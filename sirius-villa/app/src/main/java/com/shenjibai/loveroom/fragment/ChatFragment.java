package com.shenjibai.loveroom.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.adapter.ChatAdapter;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.ChatMessage;
import com.shenjibai.loveroom.entity.Memory;
import com.shenjibai.loveroom.entity.Persona;
import com.shenjibai.loveroom.net.LlmClient;
import com.shenjibai.loveroom.service.TtsService;
import com.shenjibai.loveroom.skill.SkillExecutor;
import com.shenjibai.loveroom.util.AudioRecorder;
import com.shenjibai.loveroom.util.PrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lira - 聊天 Fragment
 * 功能：流式逐字输出、世界书人设 System Prompt、重要记忆标记、
 *       TTS 朗读、录音语音消息、图片消息卡片、MCP/Skill 工具调用、
 *       手机助手唤醒
 * 署名: Lira
 */
public class ChatFragment extends Fragment implements ChatAdapter.OnMessageActionListener {

    private RecyclerView rvChat;
    private EditText etInput;
    private ImageButton btnSend, btnVoice, btnImage;
    private Chip statusChip;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private AppDatabase db;
    private PrefsManager prefs;
    private LlmClient llmClient;
    private SkillExecutor skillExecutor;
    private AudioRecorder audioRecorder;

    private ChatMessage streamingMsg; // 正在流式输出的消息
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<String> recordPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvChat = view.findViewById(R.id.rv_chat);
        etInput = view.findViewById(R.id.et_input);
        btnSend = view.findViewById(R.id.btn_send);
        btnVoice = view.findViewById(R.id.btn_voice);
        btnImage = view.findViewById(R.id.btn_image);
        statusChip = view.findViewById(R.id.status_chip);

        db = AppDatabase.getLira(requireContext());
        prefs = new PrefsManager(requireContext());
        llmClient = new LlmClient();
        skillExecutor = new SkillExecutor(requireContext());
        audioRecorder = new AudioRecorder(requireContext());

        // RecyclerView
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        adapter = new ChatAdapter(messageList, this);
        rvChat.setAdapter(adapter);

        // 加载历史消息
        db.chatMessageDao().getAllLiveLira().observe(getViewLifecycleOwner(), messages -> {
            messageList = messages;
            adapter.setMessagesLira(messages);
            scrollToBottomLira();
        });

        // 发送
        btnSend.setOnClickListener(v -> sendMessageLira());

        // 语音
        btnVoice.setOnClickListener(v -> handleVoiceLira());

        // 图片
        btnImage.setOnClickListener(v -> pickImageLira());

        // 权限 launcher
        recordPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) startRecordingLira();
                    else Toast.makeText(getContext(), R.string.perm_record, Toast.LENGTH_SHORT).show();
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) saveAndSendImageLira(uri);
                    }
                });
    }

    /** Lira - 发送文本消息 */
    private void sendMessageLira() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        etInput.setText("");

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSender("xiao");
        userMsg.setType("text");
        userMsg.setContent(text);
        userMsg.setTimestamp(System.currentTimeMillis());
        new Thread(() -> db.chatMessageDao().insertLira(userMsg)).start();

        callLlmStreamLira(text);
    }

    /** Lira - 调用大模型流式输出 */
    private void callLlmStreamLira(String userText) {
        // 加载人设 System Prompt
        new Thread(() -> {
            Persona persona = db.personaDao().getLira();
            String systemPrompt = persona != null ? persona.getSystemPrompt() : "";

            // 构建历史消息
            List<String[]> history = new ArrayList<>();
            List<ChatMessage> all = db.chatMessageDao().getAllLira();
            int start = Math.max(0, all.size() - 20);
            for (int i = start; i < all.size(); i++) {
                ChatMessage m = all.get(i);
                if (!"text".equals(m.getType())) continue;
                String role = "shen".equals(m.getSender()) ? "assistant" : "user";
                history.add(new String[]{role, m.getContent()});
            }
            // 加入当前用户消息
            history.add(new String[]{"user", userText});

            // 先插入一条空的沈既白消息用于流式更新
            streamingMsg = new ChatMessage();
            streamingMsg.setSender("shen");
            streamingMsg.setType("text");
            streamingMsg.setContent("");
            streamingMsg.setTimestamp(System.currentTimeMillis());
            long id = db.chatMessageDao().insertLira(streamingMsg);
            streamingMsg.setId(id);

            mainHandler.post(() -> {
                adapter.addMessageLira(streamingMsg);
                scrollToBottomLira();
            });

            final StringBuilder fullText = new StringBuilder();
            llmClient.chatStreamLira(prefs, systemPrompt, history, new LlmClient.StreamCallback() {
                @Override
                public void onTokenLira(String token) {
                    fullText.append(token);
                    streamingMsg.setContent(fullText.toString());
                    mainHandler.post(() -> {
                        adapter.notifyItemChanged(adapter.getItemCount() - 1);
                        scrollToBottomLira();
                    });
                }

                @Override
                public void onDoneLira(String fullText) {
                    streamingMsg.setContent(fullText);
                    new Thread(() -> db.chatMessageDao().updateLira(streamingMsg)).start();
                    mainHandler.post(() -> adapter.notifyItemChanged(adapter.getItemCount() - 1));

                    // TTS 自动朗读（仅沈既白回复）
                    if (prefs.isTtsAuto() && !TextUtils.isEmpty(fullText)) {
                        speakLira(fullText);
                    }
                }

                @Override
                public void onErrorLira(String error) {
                    mainHandler.post(() -> Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onToolCallLira(String toolName, String toolArgs) {
                    // 执行 skill 并把结果回传给大模型
                    executeSkillAndReplyLira(toolName, toolArgs);
                }
            });
        }).start();
    }

    /** Lira - 执行 skill 并将结果回传大模型生成最终回复 */
    private void executeSkillAndReplyLira(String toolName, String toolArgs) {
        skillExecutor.executeLira(toolName, toolArgs, new SkillExecutor.SkillResult() {
            @Override
            public void onResultLira(String name, String resultJson) {
                // 生成自拍 -> 插入图片消息
                if ("generate_selfie".equals(name)) {
                    try {
                        com.google.gson.JsonObject obj = new com.google.gson.Gson()
                                .fromJson(resultJson, com.google.gson.JsonObject.class);
                        String path = obj.get("image_path").getAsString();
                        insertImageMessageLira(path, "给昭昭的自拍");
                    } catch (Exception ignored) {}
                }
                // 淘宝商品 -> 插入商品卡片
                else if ("amall_taobao".equals(name)) {
                    try {
                        com.google.gson.JsonObject obj = new com.google.gson.Gson()
                                .fromJson(resultJson, com.google.gson.JsonObject.class);
                        if (obj.has("products") && obj.getAsJsonArray("products").size() > 0) {
                            String firstProduct = obj.getAsJsonArray("products").get(0).toString();
                            insertProductMessageLira(firstProduct);
                        }
                    } catch (Exception ignored) {}
                }
                // 将工具结果作为上下文再请求一次模型生成自然语言回复
                List<String[]> followUp = new ArrayList<>();
                followUp.add(new String[]{"user",
                        "工具 " + name + " 返回结果：" + resultJson + "。请用自然语言告诉昭昭结果。"});
                new Thread(() -> {
                    Persona p = db.personaDao().getLira();
                    String sys = p != null ? p.getSystemPrompt() : "";
                    String reply = llmClient.chatOnceLira(prefs, sys, followUp);
                    if (!TextUtils.isEmpty(reply)) {
                        ChatMessage m = new ChatMessage();
                        m.setSender("shen");
                        m.setType("text");
                        m.setContent(reply);
                        m.setTimestamp(System.currentTimeMillis());
                        long id = db.chatMessageDao().insertLira(m);
                        m.setId(id);
                        mainHandler.post(() -> {
                            adapter.addMessageLira(m);
                            scrollToBottomLira();
                            if (prefs.isTtsAuto()) speakLira(reply);
                        });
                    }
                }).start();
            }

            @Override
            public void onErrorLira(String name, String error) {
                mainHandler.post(() -> Toast.makeText(getContext(),
                        "工具 " + name + " 执行失败: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void insertImageMessageLira(String path, String caption) {
        ChatMessage m = new ChatMessage();
        m.setSender("shen");
        m.setType("image");
        m.setImagePath(path);
        m.setContent(caption);
        m.setTimestamp(System.currentTimeMillis());
        long id = db.chatMessageDao().insertLira(m);
        m.setId(id);
        mainHandler.post(() -> {
            adapter.addMessageLira(m);
            scrollToBottomLira();
        });
    }

    private void insertProductMessageLira(String productJson) {
        ChatMessage m = new ChatMessage();
        m.setSender("shen");
        m.setType("product");
        m.setProductJson(productJson);
        m.setTimestamp(System.currentTimeMillis());
        db.chatMessageDao().insertLira(m);
        mainHandler.post(() -> {
            adapter.addMessageLira(m);
            scrollToBottomLira();
        });
    }

    /** Lira - 录音语音消息 */
    private void handleVoiceLira() {
        if (audioRecorder.isRecordingLira()) {
            File file = audioRecorder.stopLira();
            btnVoice.setImageResource(R.drawable.ic_mic);
            // 录音转文字：将音频发送给大模型多模态接口（简化为发送 base64）
            String b64 = audioRecorder.toBase64Lira();
            if (!TextUtils.isEmpty(b64)) {
                // 实际项目中上传到支持语音的模型；此处简化为提示
                callLlmStreamLira("[语音消息] 昭昭发了一段语音，请回复。");
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else {
                startRecordingLira();
            }
        }
    }

    private void startRecordingLira() {
        audioRecorder.startLira();
        btnVoice.setImageResource(R.drawable.ic_send);
        Toast.makeText(getContext(), "正在录音…再次点击结束", Toast.LENGTH_SHORT).show();
    }

    /** Lira - 选择图片发送 */
    private void pickImageLira() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void saveAndSendImageLira(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            File dir = new File(requireContext().getCacheDir(), "images");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            is.close();
            fos.close();

            ChatMessage m = new ChatMessage();
            m.setSender("xiao");
            m.setType("image");
            m.setImagePath(file.getAbsolutePath());
            m.setTimestamp(System.currentTimeMillis());
            db.chatMessageDao().insertLira(m);

            // 让 AI 回应图片
            callLlmStreamLira("[图片] 昭昭发了一张图片。");
        } catch (Exception e) {
            Toast.makeText(getContext(), "图片保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /** Lira - TTS 朗读 */
    private void speakLira(String text) {
        Intent intent = new Intent(getContext(), TtsService.class);
        intent.setAction(TtsService.ACTION_SPEAK);
        intent.putExtra(TtsService.EXTRA_TEXT, text);
        requireContext().startService(intent);
    }

    private void scrollToBottomLira() {
        rvChat.post(() -> {
            if (adapter.getItemCount() > 0) {
                rvChat.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    // ---- OnMessageActionListener ----
    @Override
    public void onMemoryClickLira(ChatMessage msg) {
        msg.setImportant(!msg.isImportant());
        new Thread(() -> {
            db.chatMessageDao().updateLira(msg);
            if (msg.isImportant()) {
                Memory mem = new Memory();
                mem.setContent(msg.getContent());
                mem.setCreatedAt(System.currentTimeMillis());
                db.memoryDao().insertLira(mem);
            }
            mainHandler.post(() -> adapter.notifyDataSetChanged());
        }).start();
        Toast.makeText(getContext(), msg.isImportant() ? "已标记为重要记忆" : "已取消标记",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTtsClickLira(ChatMessage msg) {
        speakLira(msg.getContent());
    }

    @Override
    public void onImageClickLira(ChatMessage msg) {
        // 跳转到图片预览
        Intent intent = new Intent(getContext(), com.shenjibai.loveroom.ImagePreviewActivity.class);
        intent.putExtra("image_path", msg.getImagePath());
        startActivity(intent);
    }
}
