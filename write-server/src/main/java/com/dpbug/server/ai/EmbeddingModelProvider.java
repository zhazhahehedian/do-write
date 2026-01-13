package com.dpbug.server.ai;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
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
 * <p>
 * 支持的类型：
 * <ul>
 *   <li>OPENAI - OpenAI 官方 API</li>
 *   <li>OLLAMA - 本地 Ollama 服务</li>
 *   <li>DASHSCOPE - 阿里云百炼 (text-embedding-v4 等)</li>
 *   <li>CUSTOM - OpenAI 兼容的第三方 API (如 Azure OpenAI, one-api 等)</li>
 * </ul>
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
            UserApiConfig systemConfig = userApiConfigService.getSystemConfig();
            log.info("系统Embedding模型配置已加载: apiType={}, model={}",
                    systemConfig.getApiType(), systemConfig.getEmbeddingModel());
        } catch (Exception e) {
            log.warn("系统Embedding模型配置未找到，请在数据库中创建 user_id=0 的配置记录");
        }
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        UserApiConfig config = userApiConfigService.getSystemConfig();
        String apiType = config.getApiType().toUpperCase();

        log.info("初始化 EmbeddingModel: apiType={}, model={}", apiType, config.getEmbeddingModel());

        return switch (apiType) {
            case "OPENAI" -> createOpenAiEmbeddingModel(config);
            case "OLLAMA" -> createOllamaEmbeddingModel(config);
            case "DASHSCOPE" -> createDashScopeEmbeddingModel(config);
            case "CUSTOM" -> createCustomEmbeddingModel(config);
            default -> throw new IllegalArgumentException("不支持的Embedding模型类型: " + apiType);
        };
    }

    /**
     * 创建 OpenAI EmbeddingModel
     */
    private EmbeddingModel createOpenAiEmbeddingModel(UserApiConfig config) {
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.getApiKey());

        if (hasValue(config.getBaseUrl())) {
            apiBuilder.baseUrl(OpenAiBaseUrlNormalizer.normalize(config.getBaseUrl()));
        }

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(getOrDefault(config.getEmbeddingModel(), "text-embedding-3-large"))
                .build();

        return new OpenAiEmbeddingModel(apiBuilder.build(), MetadataMode.EMBED, options);
    }

    /**
     * 创建 Ollama EmbeddingModel
     */
    private EmbeddingModel createOllamaEmbeddingModel(UserApiConfig config) {
        String baseUrl = getOrDefault(config.getBaseUrl(), "http://localhost:11434");

        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(getOrDefault(config.getEmbeddingModel(), "nomic-embed-text"))
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * 创建阿里云百炼 DashScope EmbeddingModel
     * 支持 text-embedding-v4, text-embedding-v2 等模型
     *
     * 注意：DashScopeApi 使用 spring.ai.dashscope.api-key 配置，
     * 不支持通过 builder().apiKey() 动态传入（库的限制）
     */
    private EmbeddingModel createDashScopeEmbeddingModel(UserApiConfig config) {
        // 注意：DashScopeApi 的 API Key 从 spring.ai.dashscope.api-key 配置读取
        // builder().apiKey() 传入的值会被忽略，这是 spring-ai-alibaba 库的行为
        // 因此需要在 yml 中配置正确的 API Key

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(config.getApiKey())  // 仍然传入，但实际使用配置文件的值
                .build();

        String model = getOrDefault(config.getEmbeddingModel(), "text-embedding-v4");
        log.info("创建 DashScope EmbeddingModel: model={}", model);

        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .withModel(model)
                .build();

        return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, options);
    }

    /**
     * 创建 CUSTOM 类型 EmbeddingModel
     * 适用于 OpenAI 兼容的第三方 API (Azure OpenAI, one-api 等)
     * 注意：不适用于阿里云百炼、DeepSeek 等非 OpenAI 兼容的服务
     */
    private EmbeddingModel createCustomEmbeddingModel(UserApiConfig config) {
        if (!hasValue(config.getEmbeddingModel())) {
            log.warn("CUSTOM 类型未配置 embeddingModel，Embedding 功能将不可用");
            return new StubEmbeddingModel();
        }

        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.getApiKey());

        if (hasValue(config.getBaseUrl())) {
            apiBuilder.baseUrl(OpenAiBaseUrlNormalizer.normalize(config.getBaseUrl()));
        }

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(config.getEmbeddingModel())
                .build();

        return new OpenAiEmbeddingModel(apiBuilder.build(), MetadataMode.EMBED, options);
    }

    private String trimTrailingSlash(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean hasValue(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private String getOrDefault(String value, String defaultValue) {
        return hasValue(value) ? value : defaultValue;
    }
}
