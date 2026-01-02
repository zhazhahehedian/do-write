package com.dpbug.common.constant;

/**
 * AI 相关常量
 */
public interface AIConstants {

    /**
     * AI 模型类型
     */
    interface ModelType {
        /**
         * OpenAI GPT
         */
        String OPENAI_GPT = "openai";

        /**
         * Ollama 本地模型
         */
        String OLLAMA = "ollama";
    }

    /**
     * AI 任务类型
     */
    interface TaskType {
        /**
         * 文本生成
         */
        String TEXT_GENERATION = "text_generation";

        /**
         * 文本续写
         */
        String TEXT_CONTINUATION = "text_continuation";

        /**
         * 文本改写
         */
        String TEXT_REWRITE = "text_rewrite";

        /**
         * 文本润色
         */
        String TEXT_POLISH = "text_polish";

        /**
         * 文本总结
         */
        String TEXT_SUMMARY = "text_summary";

        /**
         * 文本翻译
         */
        String TEXT_TRANSLATION = "text_translation";
    }

    /**
     * AI 对话角色
     */
    interface Role {
        /**
         * 系统角色
         */
        String SYSTEM = "system";

        /**
         * 用户角色
         */
        String USER = "user";

        /**
         * 助手角色
         */
        String ASSISTANT = "assistant";
    }

    /**
     * AI 请求参数默认值
     */
    interface DefaultParams {
        /**
         * 默认温度参数
         */
        Double TEMPERATURE = 0.7;

        /**
         * 默认最大token数
         */
        Integer MAX_TOKENS = 2000;

        /**
         * 默认top_p参数
         */
        Double TOP_P = 1.0;
    }
}