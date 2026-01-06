package com.dpbug.server.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChromaDB 向量数据库配置
 *
 * <p>功能：</p>
 * <ul>
 *   <li>配置 ChromaDB 向量存储（专业向量数据库）</li>
 *   <li>用于存储小说故事记忆的向量数据</li>
 *   <li>支持语义检索（RAG）和复杂元数据过滤</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>ChromaApi 由 spring-ai-starter-vector-store-chroma 自动配置</li>
 *   <li>EmbeddingModel 由 EmbeddingModelProvider 提供</li>
 *   <li>这里创建一个默认的 VectorStore Bean 用于系统级操作</li>
 *   <li>业务层通过 ChromaVectorStoreFactory 动态创建项目级 Collection</li>
 * </ul>
 *
 * <p>ChromaDB 部署：</p>
 * <pre>
 * docker run -d --name chromadb -p 8000:8000 \
 *   -v chroma_data:/chroma/chroma \
 *   -e IS_PERSISTENT=TRUE \
 *   chromadb/chroma:latest
 * </pre>
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.chroma.collection-name:novel-memories}")
    private String defaultCollectionName;

    /**
     * 创建默认 VectorStore Bean（系统级）
     *
     * <p>用途：</p>
     * <ul>
     *   <li>全局操作，如嵌入模型测试</li>
     *   <li>系统级向量存储需求</li>
     * </ul>
     *
     * <p>注意：</p>
     * 业务层（如故事记忆）应使用 ChromaVectorStoreFactory 获取项目专属的 VectorStore，
     * 以实现用户-项目级别的数据隔离。
     *
     * @param chromaApi      ChromaDB API 客户端（自动注入）
     * @param embeddingModel 嵌入模型
     * @return VectorStore 实例
     */
    @Bean
    public VectorStore vectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        log.info("初始化 ChromaVectorStore: collectionName={}", defaultCollectionName);

        VectorStore vectorStore = ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName(defaultCollectionName)
                .initializeSchema(true)
                .build();

        log.info("✅ ChromaVectorStore 初始化成功");
        return vectorStore;
    }
}
