package com.dpbug.server.ai;

import com.dpbug.server.model.entity.user.UserApiConfig;
import com.dpbug.server.service.user.UserApiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * ChatClient 动态工厂
 * 根据用户的API配置动态创建 ChatClient 实例
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>根据用户配置创建不同类型的 ChatClient（OpenAI, Ollama, 自定义端点）</li>
 *   <li>支持流式和非流式调用</li>
 *   <li>自动应用用户配置的参数（temperature, maxTokens等）</li>
 * </ul>
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClientFactory {

    private final UserApiConfigService userApiConfigService;

    /**
     * 角色生成等复杂任务的最小 maxTokens
     * 7个角色的JSON大约需要8000+ tokens
     * 设大一点先
     */
    private static final int MIN_TOKENS_FOR_COMPLEX_TASKS = 8192;

    /**
     * 为指定用户创建 ChatClient（使用默认配置）
     *
     * @param userId 用户ID
     * @return ChatClient 实例
     */
    public ChatClient createForUser(Long userId) {
        UserApiConfig config = userApiConfigService.getDefaultConfig(userId);
        return createChatClient(config, null);
    }

    /**
     * 为指定用户创建 ChatClient（使用更高的 maxTokens）
     * 适用于角色生成、大纲生成等需要长输出的任务
     *
     * @param userId    用户ID
     * @param minTokens 最小 token 数量
     * @return ChatClient 实例
     */
    public ChatClient createForUserWithMinTokens(Long userId, int minTokens) {
        UserApiConfig config = userApiConfigService.getDefaultConfig(userId);
        return createChatClient(config, minTokens);
    }

    /**
     * 为复杂任务创建 ChatClient（自动使用较高的 maxTokens）
     *
     * @param userId 用户ID
     * @return ChatClient 实例
     */
    public ChatClient createForComplexTask(Long userId) {
        return createForUserWithMinTokens(userId, MIN_TOKENS_FOR_COMPLEX_TASKS);
    }

    /**
     * 获取用户当前使用的模型名称
     *
     * @param userId 用户ID
     * @return 模型名称
     */
    public String getCurrentModelName(Long userId) {
        UserApiConfig config = userApiConfigService.getDefaultConfig(userId);
        return config != null ? config.getModelName() : "unknown";
    }

    /**
     * 使用指定配置创建 ChatClient
     *
     * @param userId   用户ID
     * @param configId 配置ID
     * @return ChatClient 实例
     */
    public ChatClient createForUser(Long userId, Long configId) {
        UserApiConfig config = userApiConfigService.getConfigById(userId, configId);
        return createChatClient(config, null);
    }

    /**
     * 根据配置创建 ChatClient（核心方法）
     *
     * @param config    API配置
     * @param minTokens 最小 token 数量（可选）
     * @return ChatClient 实例
     */
    private ChatClient createChatClient(UserApiConfig config, Integer minTokens) {
        log.debug("创建 ChatClient: apiType={}, modelName={}, minTokens={}",
                config.getApiType(), config.getModelName(), minTokens);

        return switch (config.getApiType().toUpperCase()) {
            case "OPENAI", "AZURE_OPENAI", "CUSTOM" -> createOpenAiChatClient(config, minTokens);
            case "OLLAMA" -> createOllamaChatClient(config, minTokens);
            default -> throw new IllegalArgumentException("不支持的API类型: " + config.getApiType());
        };
    }

    /**
     * 规范化 baseUrl，移除可能导致路径重复的部分
     * Spring AI 的 OpenAiApi 会自动拼接 /v1/chat/completions
     * 所以用户配置的 baseUrl 如果已经包含 /v1，需要去掉
     *
     * @param baseUrl 原始 baseUrl
     * @return 规范化后的 baseUrl
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }

        // 去掉末尾的斜杠
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // 如果以 /v1 结尾，去掉它（因为 Spring AI 会自动添加）
        while (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            log.debug("规范化 baseUrl: {} -> {}", baseUrl, normalized);
        }

        return normalized;
    }

    /**
     * 计算实际使用的 maxTokens
     */
    private Integer calculateMaxTokens(Integer configMaxTokens, Integer minTokens) {
        if (minTokens == null) {
            return configMaxTokens;
        }
        if (configMaxTokens == null) {
            return minTokens;
        }
        return Math.max(configMaxTokens, minTokens);
    }

    /**
     * 创建 OpenAI ChatClient
     *
     * @param config    用户配置
     * @param minTokens 最小 token 数量
     * @return ChatClient
     */
    private ChatClient createOpenAiChatClient(UserApiConfig config, Integer minTokens) {
        // 构建 OpenAI API
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.getApiKey());

        // 如果有自定义 baseUrl，则使用（支持Azure OpenAI、国内代理等）
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            // 规范化 baseUrl，避免路径重复
            String baseUrl = normalizeBaseUrl(config.getBaseUrl());
            if (baseUrl != null && !baseUrl.isEmpty()) {
                apiBuilder.baseUrl(baseUrl);
            }
        }

        OpenAiApi openAiApi = apiBuilder.build();

        // 构建 ChatModel Options
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(config.getModelName());

        if (config.getTemperature() != null) {
            optionsBuilder.temperature(config.getTemperature().doubleValue());
        }

        // 计算实际使用的 maxTokens
        Integer actualMaxTokens = calculateMaxTokens(config.getMaxTokens(), minTokens);
        if (actualMaxTokens != null) {
            optionsBuilder.maxTokens(actualMaxTokens);
        }

        // 创建 ChatModel（使用 Builder 模式）
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .build();

        // 返回 ChatClient
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 创建 Ollama ChatClient
     *
     * @param config    用户配置
     * @param minTokens 最小 token 数量
     * @return ChatClient
     */
    private ChatClient createOllamaChatClient(UserApiConfig config, Integer minTokens) {
        // Ollama 默认 baseUrl 为 http://localhost:11434
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl()
                : "http://localhost:11434";

        // 构建 Ollama API（使用 Builder 模式）
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        // 构建 Options
        OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder()
                .model(config.getModelName());

        if (config.getTemperature() != null) {
            optionsBuilder.temperature(config.getTemperature().doubleValue());
        }

        // Ollama 使用 numPredict 而不是 maxTokens
        Integer actualMaxTokens = calculateMaxTokens(config.getMaxTokens(), minTokens);
        if (actualMaxTokens != null) {
            optionsBuilder.numPredict(actualMaxTokens);
        }

        // 创建 ChatModel（使用 Builder 模式）
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(optionsBuilder.build())
                .build();

        // 返回 ChatClient
        return ChatClient.builder(chatModel).build();
    }
}
