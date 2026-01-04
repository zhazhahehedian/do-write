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
     * 为指定用户创建 ChatClient（使用默认配置）
     *
     * @param userId 用户ID
     * @return ChatClient 实例
     */
    public ChatClient createForUser(Long userId) {
        UserApiConfig config = userApiConfigService.getDefaultConfig(userId);
        return createChatClient(config);
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
        return createChatClient(config);
    }

    /**
     * 根据配置创建 ChatClient（核心方法）
     *
     * @param config API配置
     * @return ChatClient 实例
     */
    private ChatClient createChatClient(UserApiConfig config) {
        log.debug("创建 ChatClient: apiType={}, modelName={}", config.getApiType(), config.getModelName());

        return switch (config.getApiType().toUpperCase()) {
            case "OPENAI" -> createOpenAiChatClient(config);
            case "OLLAMA" -> createOllamaChatClient(config);
            default -> throw new IllegalArgumentException("不支持的API类型: " + config.getApiType());
        };
    }

    /**
     * 创建 OpenAI ChatClient
     *
     * @param config 用户配置
     * @return ChatClient
     */
    private ChatClient createOpenAiChatClient(UserApiConfig config) {
        // 构建 OpenAI API
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.getApiKey());

        // 如果有自定义 baseUrl，则使用（支持Azure OpenAI、国内代理等）
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            apiBuilder.baseUrl(config.getBaseUrl());
        }

        OpenAiApi openAiApi = apiBuilder.build();

        // 构建 ChatModel Options
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(config.getModelName());

        if (config.getTemperature() != null) {
            optionsBuilder.temperature(config.getTemperature().doubleValue());
        }
        if (config.getMaxTokens() != null) {
            optionsBuilder.maxTokens(config.getMaxTokens());
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
     * @param config 用户配置
     * @return ChatClient
     */
    private ChatClient createOllamaChatClient(UserApiConfig config) {
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
        if (config.getMaxTokens() != null) {
            optionsBuilder.numPredict(config.getMaxTokens());
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