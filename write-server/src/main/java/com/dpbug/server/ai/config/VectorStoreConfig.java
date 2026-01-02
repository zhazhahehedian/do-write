package com.dpbug.server.ai.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * VectorStore 向量数据库配置
 *
 * <p>功能：</p>
 * <ul>
 *   <li>配置 SimpleVectorStore（基于文件的向量存储）</li>
 *   <li>用于存储小说故事记忆的向量数据</li>
 *   <li>支持语义检索（RAG）</li>
 * </ul>
 *
 * <p>说明：</p>
 * SimpleVectorStore 适合开发和小规模使用，生产环境建议使用：
 * <ul>
 *   <li>PgVectorStore - PostgreSQL + pgvector 扩展</li>
 *   <li>RedisVectorStore - Redis Stack</li>
 *   <li>ChromaVectorStore - Chroma 向量数据库</li>
 * </ul>
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${ai.vector-store.file-path:./data/vector-store.json}")
    private String vectorStoreFilePath;

    /**
     * VectorStore 实例（用于 @PreDestroy 保存）
     */
    private SimpleVectorStore vectorStore;

    /**
     * 创建 VectorStore Bean
     *
     * @param embeddingModel 嵌入模型
     * @return VectorStore 实例
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        log.info("初始化 SimpleVectorStore: filePath={}", vectorStoreFilePath);

        // 使用 Builder 模式创建 SimpleVectorStore
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // 尝试从文件加载已有的向量数据
        File vectorFile = new File(vectorStoreFilePath);
        if (vectorFile.exists()) {
            try {
                this.vectorStore.load(vectorFile);
                log.info("VectorStore 数据已从文件加载");
            } catch (Exception e) {
                log.warn("VectorStore 文件加载失败，将创建新的存储: {}", e.getMessage());
            }
        } else {
            log.info("VectorStore 文件不存在，将在首次使用时创建");
            // 确保目录存在
            File parentDir = vectorFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        }

        return this.vectorStore;
    }

    /**
     * 应用关闭时保存 VectorStore 到文件
     *
     * <p>说明：</p>
     * <ul>
     *   <li>自动在应用关闭时将内存中的向量数据持久化到文件</li>
     *   <li>防止应用正常关闭时数据丢失</li>
     *   <li>异常崩溃时仍可能丢失数据，建议在关键操作后手动调用 save()</li>
     * </ul>
     */
    @PreDestroy
    public void saveVectorStore() {
        if (vectorStore != null) {
            try {
                vectorStore.save(new File(vectorStoreFilePath));
                log.info("✅ VectorStore 数据已保存到文件: {}", vectorStoreFilePath);
            } catch (Exception e) {
                log.error("❌ VectorStore 保存失败", e);
            }
        }
    }
}