package com.shenjibai.loveroom.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.shenjibai.loveroom.PersonaActivity;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.service.SentinelService;
import com.shenjibai.loveroom.service.WakeWordService;
import com.shenjibai.loveroom.util.PrefsManager;

/**
 * Lira - 设置 Fragment
 * 所有密钥、接口 URL 由用户手动输入，存储本地 SharedPreferences
 * 绝不硬编码，不上传外网
 * 署名: Lira
 */
public class SettingsFragment extends Fragment {

    private PrefsManager prefs;

    private TextInputEditText etLlmUrl, etLlmKey, etLlmModel;
    private TextInputEditText etTtsUrl, etTtsKey;
    private TextInputEditText etSelfieUrl, etSelfieKey;
    private TextInputEditText etTavilyKey, etAmapKey;
    private TextInputEditText etSentinelInterval, etSentinelKey;
    private TextInputEditText etWakeWord;
    private SwitchMaterial swTtsAuto, swSentinel, swSentinelScreen, swWake;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PrefsManager(requireContext());

        // 绑定视图
        etLlmUrl = view.findViewById(R.id.et_llm_url);
        etLlmKey = view.findViewById(R.id.et_llm_key);
        etLlmModel = view.findViewById(R.id.et_llm_model);
        etTtsUrl = view.findViewById(R.id.et_tts_url);
        etTtsKey = view.findViewById(R.id.et_tts_key);
        etSelfieUrl = view.findViewById(R.id.et_selfie_url);
        etSelfieKey = view.findViewById(R.id.et_selfie_key);
        etTavilyKey = view.findViewById(R.id.et_tavily_key);
        etAmapKey = view.findViewById(R.id.et_amap_key);
        etSentinelInterval = view.findViewById(R.id.et_sentinel_interval);
        etSentinelKey = view.findViewById(R.id.et_sentinel_key);
        etWakeWord = view.findViewById(R.id.et_wake_word);
        swTtsAuto = view.findViewById(R.id.sw_tts_auto);
        swSentinel = view.findViewById(R.id.sw_sentinel);
        swSentinelScreen = view.findViewById(R.id.sw_sentinel_screen);
        swWake = view.findViewById(R.id.sw_wake);

        MaterialButton btnPersona = view.findViewById(R.id.btn_persona);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);

        btnPersona.setOnClickListener(v ->
                startActivity(new Intent(getContext(), PersonaActivity.class)));

        btnSave.setOnClickListener(v -> saveSettingsLira());

        loadSettingsLira();

        // 哨兵开关
        swSentinel.setOnCheckedChangeListener((b, checked) -> {
            prefs.putBoolean(PrefsManager.SENTINEL_ENABLE, checked);
            Intent i = new Intent(getContext(), SentinelService.class);
            if (checked) requireContext().startService(i);
            else { i.setAction("STOP"); requireContext().startService(i); }
        });

        // 唤醒开关
        swWake.setOnCheckedChangeListener((b, checked) -> {
            prefs.putBoolean(PrefsManager.WAKE_ENABLE, checked);
            Intent i = new Intent(getContext(), WakeWordService.class);
            if (checked) requireContext().startService(i);
            else { i.setAction("STOP"); requireContext().startService(i); }
        });
    }

    /** Lira - 加载已保存设置 */
    private void loadSettingsLira() {
        etLlmUrl.setText(prefs.getLlmUrl());
        etLlmKey.setText(prefs.getLlmKey());
        etLlmModel.setText(prefs.getLlmModel());
        etTtsUrl.setText(prefs.getTtsUrl());
        etTtsKey.setText(prefs.getTtsKey());
        etSelfieUrl.setText(prefs.getSelfieUrl());
        etSelfieKey.setText(prefs.getSelfieKey());
        etTavilyKey.setText(prefs.getTavilyKey());
        etAmapKey.setText(prefs.getAmapKey());
        etSentinelInterval.setText(String.valueOf(prefs.getSentinelInterval()));
        etSentinelKey.setText(prefs.getSentinelKey());
        etWakeWord.setText(prefs.getString(PrefsManager.WAKE_WORD));
        swTtsAuto.setChecked(prefs.isTtsAuto());
        swSentinel.setChecked(prefs.isSentinelEnabled());
        swSentinelScreen.setChecked(prefs.isSentinelScreenEnabled());
        swWake.setChecked(prefs.isWakeEnabled());
    }

    /** Lira - 保存设置到本地 SharedPreferences */
    private void saveSettingsLira() {
        prefs.putString(PrefsManager.LLM_URL, etLlmUrl.getText().toString());
        prefs.putString(PrefsManager.LLM_KEY, etLlmKey.getText().toString());
        prefs.putString(PrefsManager.LLM_MODEL, etLlmModel.getText().toString());
        prefs.putString(PrefsManager.TTS_URL, etTtsUrl.getText().toString());
        prefs.putString(PrefsManager.TTS_KEY, etTtsKey.getText().toString());
        prefs.putString(PrefsManager.SELFIE_URL, etSelfieUrl.getText().toString());
        prefs.putString(PrefsManager.SELFIE_KEY, etSelfieKey.getText().toString());
        prefs.putString(PrefsManager.TAVILY_KEY, etTavilyKey.getText().toString());
        prefs.putString(PrefsManager.AMAP_KEY, etAmapKey.getText().toString());

        try {
            int interval = Integer.parseInt(etSentinelInterval.getText().toString());
            prefs.putInt(PrefsManager.SENTINEL_INTERVAL, Math.max(30, interval));
        } catch (Exception ignored) {}

        prefs.putString(PrefsManager.SENTINEL_KEY, etSentinelKey.getText().toString());
        prefs.putBoolean(PrefsManager.SENTINEL_SCREEN, swSentinelScreen.isChecked());
        prefs.putString(PrefsManager.WAKE_WORD, etWakeWord.getText().toString());
        prefs.putBoolean(PrefsManager.TTS_AUTO, swTtsAuto.isChecked());

        Toast.makeText(getContext(), "设置已保存", Toast.LENGTH_SHORT).show();
    }
}
