package com.dpbug.server.ai;

import com.dpbug.server.model.entity.user.UserApiConfig;
import com.dpbug.server.service.user.UserApiConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EmbeddingModel 提供者配置
 *
 * <p>功能：</p>
 * <ul>
 *   <li>从系统配置（user_id=0）读取 Embedding 模型配置</li>
 *   <li>创建全局唯一的 EmbeddingModel Bean</li>
 *   <li>用于 VectorStore 进行向量化存储和检索</li>
 * </ul>
 *
 * <p>说明：</p>
 * Embedding 模型需要系统级稳定配置，不像 ChatClient 那样频繁切换，
 * 因此使用 user_id=0 的系统配置，确保所有用户使用统一的向量空间。
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmbeddingModelProvider {

    private final UserApiConfigService userApiConfigService;

    @PostConstruct
    public void init() {
        try {
            // 检查系统配置是否存在
            UserApiConfig systemConfig = userApiConfigService.getSystemConfig();
            log.info("✅ 系统Embedding模型配置已加载: apiType={}, embeddingModel={}",
                    systemConfig.getApiType(), systemConfig.getEmbeddingModel());
        } catch (Exception e) {
            log.warn("⚠️  系统Embedding模型配置未找到，请在数据库中创建 user_id=0 的配置记录");
        }
    }

    /**
     * 创建全局 EmbeddingModel Bean
     *
     * @return EmbeddingModel 实例
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        UserApiConfig config = userApiConfigService.getSystemConfig();

        log.info("初始化 EmbeddingModel: apiType={}, model={}",
                config.getApiType(), config.getEmbeddingModel());

        return switch (config.getApiType().toUpperCase()) {
            case "OPENAI" -> createOpenAiEmbeddingModel(config);
            case "OLLAMA" -> createOllamaEmbeddingModel(config);
            default -> throw new IllegalArgumentException("不支持的Embedding模型类型: " + config.getApiType());
        };
    }

    /**
     * 创建 OpenAI EmbeddingModel
     *
     * @param config 系统配置
     * @return OpenAiEmbeddingModel
     */
    private EmbeddingModel createOpenAiEmbeddingModel(UserApiConfig config) {
        // 构建 OpenAI API
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.getApiKey());

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            apiBuilder.baseUrl(config.getBaseUrl());
        }

        OpenAiApi openAiApi = apiBuilder.build();

        // 构建 Embedding Options
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(config.getEmbeddingModel() != null ? config.getEmbeddingModel() : "text-embedding-3-large")
                .build();

        // OpenAiEmbeddingModel 需要3个参数：API、MetadataMode、Options
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    /**
     * 创建 Ollama EmbeddingModel
     *
     * @param config 系统配置
     * @return OllamaEmbeddingModel
     */
    private EmbeddingModel createOllamaEmbeddingModel(UserApiConfig config) {
        String baseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()
                ? config.getBaseUrl()
                : "http://localhost:11434";

        // 构建 Ollama API（使用 Builder 模式）
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        // Ollama Embedding Options
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(config.getEmbeddingModel() != null ? config.getEmbeddingModel() : "nomic-embed-text")
                .build();

        // OllamaEmbeddingModel 需要4个参数，使用 Builder 模式
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }
}