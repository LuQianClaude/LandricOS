package com.shenjibai.loveroom.skill;

import android.content.Context;

import com.google.gson.JsonObject;

/**
 * Lira - Skill 抽象接口
 * 署名: Lira
 */
public interface Skill {
    void runLira(Context context, JsonObject args, SkillExecutor.SkillResult cb);
}
