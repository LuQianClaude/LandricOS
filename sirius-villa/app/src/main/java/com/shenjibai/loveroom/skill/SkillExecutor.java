package com.shenjibai.loveroom.skill;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Lira - 简易工具调用解析与执行
 * 解析大模型返回的 JSON 工具调用指令，本地执行 skill，把结果回传
 * 署名: Lira
 */
public class SkillExecutor {

    public interface SkillResult {
        void onResultLira(String toolName, String resultJson);
        void onErrorLira(String toolName, String error);
    }

    private final Context context;
    private final Map<String, Skill> skills = new HashMap<>();

    public SkillExecutor(Context context) {
        this.context = context;
        // 注册内置 skill
        skills.put("tavily_search", new TavilySearchSkill());
        skills.put("amall_taobao", new AmallTaobaoSkill());
        skills.put("generate_selfie", new GenerateSelfieSkill());
        skills.put("get_gps_location", new GpsLocationSkill());
    }

    /**
     * 解析并执行工具调用
     * @param toolName 工具名
     * @param argsJson 参数 JSON 字符串
     */
    public void executeLira(String toolName, String argsJson, SkillResult cb) {
        Skill skill = skills.get(toolName);
        if (skill == null) {
            cb.onErrorLira(toolName, "未知工具: " + toolName);
            return;
        }
        JsonObject args;
        try {
            args = TextUtils.isEmpty(argsJson) ? new JsonObject()
                    : new Gson().fromJson(argsJson, JsonObject.class);
        } catch (Exception e) {
            args = new JsonObject();
        }
        skill.runLira(context, args, cb);
    }

    /** 用户上传/填写的自定义 skill 也可注册 */
    public void registerSkillLira(String name, Skill skill) {
        skills.put(name, skill);
    }
}
