package com.shenjibai.loveroom.util;

import android.content.Context;
import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;

/**
 * Lira - 录音工具：录音后转文字发送（转文字由大模型处理）
 * 动态申请录音权限，拒绝则禁用
 * 署名: Lira
 */
public class AudioRecorder {

    private final Context context;
    private MediaRecorder recorder;
    private File outputFile;
    private boolean recording = false;

    public AudioRecorder(Context context) {
        this.context = context;
    }

    public File startLira() {
        if (recording) return outputFile;
        try {
            File dir = new File(context.getCacheDir(), "voice");
            if (!dir.exists()) dir.mkdirs();
            outputFile = new File(dir, "voice_" + System.currentTimeMillis() + ".m4a");

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(64000);
            recorder.setAudioSamplingRate(16000);
            recorder.setOutputFile(outputFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recording = true;
            return outputFile;
        } catch (IOException e) {
            return null;
        }
    }

    public File stopLira() {
        if (!recording) return outputFile;
        try {
            recorder.stop();
            recorder.release();
        } catch (Exception ignored) {}
        recorder = null;
        recording = false;
        return outputFile;
    }

    public boolean isRecordingLira() { return recording; }

    /**
     * Lira - 录音转文字：将音频上传到对话大模型的多模态接口
     * 简化实现：将音频转 base64 交给调用方处理
     */
    public String toBase64Lira() {
        if (outputFile == null || !outputFile.exists()) return "";
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(outputFile);
            byte[] bytes = new byte[(int) outputFile.length()];
            fis.read(bytes);
            fis.close();
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}
