package com.dpbug.server.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.Collections;

/**
 * Stub EmbeddingModel 实现
 * 当系统未配置有效的 Embedding 模型时使用此实现
 * 所有调用都会返回空结果
 *
 * @author dpbug
 */
@Slf4j
public class StubEmbeddingModel implements EmbeddingModel {

    public StubEmbeddingModel() {
        log.warn("StubEmbeddingModel 已初始化 - Embedding 功能不可用，请配置有效的 Embedding 模型");
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        log.debug("StubEmbeddingModel.call() 被调用，返回空结果");
        return new EmbeddingResponse(Collections.emptyList());
    }

    @Override
    public float[] embed(Document document) {
        return new float[0];
    }

    @Override
    public float[] embed(String text) {
        return new float[0];
    }
}
