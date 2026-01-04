package com.dpbug.server.ai.prompt;

import com.dpbug.server.ai.prompt.model.WritingStyle;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 写作风格管理器
 * <p>
 * 管理预设写作风格，提供风格查询和应用功能。
 * </p>
 */
@Component
public class WritingStyleManager {

    /**
     * 获取预设风格
     *
     * @param styleCode 风格编码
     * @return 风格配置，如果不存在返回 Optional.empty()
     */
    public Optional<WritingStyleConfig> getPresetStyle(String styleCode) {
        WritingStyle style = WritingStyle.fromCode(styleCode);
        if (style == null) {
            return Optional.empty();
        }
        return Optional.of(new WritingStyleConfig(
                style.getCode(),
                style.getName(),
                style.getDescription(),
                style.getPromptContent()
        ));
    }

    /**
     * 获取所有预设风格
     *
     * @return 风格配置列表
     */
    public List<WritingStyleConfig> getAllPresets() {
        List<WritingStyleConfig> configs = new ArrayList<>();
        for (WritingStyle style : WritingStyle.values()) {
            configs.add(new WritingStyleConfig(
                    style.getCode(),
                    style.getName(),
                    style.getDescription(),
                    style.getPromptContent()
            ));
        }
        return configs;
    }

    /**
     * 获取所有预设风格（Map形式，key为styleCode）
     *
     * @return 风格配置Map
     */
    public Map<String, WritingStyleConfig> getAllPresetsAsMap() {
        Map<String, WritingStyleConfig> map = new LinkedHashMap<>();
        for (WritingStyle style : WritingStyle.values()) {
            map.put(style.getCode(), new WritingStyleConfig(
                    style.getCode(),
                    style.getName(),
                    style.getDescription(),
                    style.getPromptContent()
            ));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * 将写作风格应用到基础提示词
     *
     * @param basePrompt   基础提示词
     * @param styleContent 风格核心指令内容
     * @return 组合后的提示词
     */
    public String applyStyleToPrompt(String basePrompt, String styleContent) {
        if (styleContent == null || styleContent.isBlank()) {
            return basePrompt;
        }
        return basePrompt + "\n\n" + styleContent +
                "\n\n请直接输出章节正文内容，不要包含章节标题和其他说明文字。";
    }

    /**
     * 将写作风格应用到基础提示词（通过风格枚举）
     *
     * @param basePrompt 基础提示词
     * @param style      风格枚举
     * @return 组合后的提示词
     */
    public String applyStyleToPrompt(String basePrompt, WritingStyle style) {
        if (style == null) {
            return basePrompt;
        }
        return applyStyleToPrompt(basePrompt, style.getPromptContent());
    }

    /**
     * 写作风格配置
     */
    @Data
    @AllArgsConstructor
    public static class WritingStyleConfig {
        /**
         * 风格编码
         */
        private String code;

        /**
         * 风格名称
         */
        private String name;

        /**
         * 风格描述
         */
        private String description;

        /**
         * 风格核心指令（提示词内容）
         */
        private String promptContent;
    }
}
