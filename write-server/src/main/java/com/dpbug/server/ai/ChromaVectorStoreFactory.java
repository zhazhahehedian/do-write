package com.dpbug.server.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChromaDB Collection 动态工厂
 *
 * <p>功能：</p>
 * <ul>
 *   <li>为每个用户的每个项目创建独立的 Collection</li>
 *   <li>缓存已创建的 VectorStore 实例</li>
 *   <li>使用 SHA256 哈希压缩 ID，符合 ChromaDB 命名规则</li>
 * </ul>
 *
 * <p>命名规则（参考 learn1）：</p>
 * <ul>
 *   <li>Collection 名称：u_{userHash}_p_{projectHash}</li>
 *   <li>userHash/projectHash：ID 的 SHA256 前 8 位</li>
 *   <li>总长度约 30 字符，符合 ChromaDB 的 3-63 字符限制</li>
 * </ul>
 *
 * <p>ChromaDB Collection 命名规则：</p>
 * <ul>
 *   <li>3-63 字符（最重要！）</li>
 *   <li>开头和结尾必须是字母或数字</li>
 *   <li>只能包含字母、数字、下划线或短横线</li>
 *   <li>不能包含连续的点(..)</li>
 *   <li>不能是有效的 IPv4 地址</li>
 * </ul>
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Component
public class ChromaVectorStoreFactory {

    private final ChromaApi chromaApi;
    private final EmbeddingModel embeddingModel;

    /**
     * ChromaDB 租户名称（Spring AI 默认值）
     */
    @Value("${spring.ai.vectorstore.chroma.tenant-name:default_tenant}")
    private String tenantName;

    /**
     * ChromaDB 数据库名称（Spring AI 默认值）
     */
    @Value("${spring.ai.vectorstore.chroma.database-name:default_database}")
    private String databaseName;

    /**
     * 缓存已创建的 VectorStore 实例
     * Key: collectionName
     * Value: VectorStore
     */
    private final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();

    public ChromaVectorStoreFactory(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        this.chromaApi = chromaApi;
        this.embeddingModel = embeddingModel;
        log.info("✅ ChromaVectorStoreFactory 初始化完成");
    }

    /**
     * 获取项目专属的 VectorStore
     *
     * <p>说明：</p>
     * 每个用户的每个项目拥有独立的 ChromaDB Collection，实现数据隔离。
     * 首次调用时会创建 Collection，后续调用从缓存获取。
     *
     * @param userId    用户 ID
     * @param projectId 项目 ID
     * @return VectorStore 实例
     */
    public VectorStore getVectorStore(Long userId, Long projectId) {
        String collectionName = generateCollectionName(userId, projectId);

        return vectorStoreCache.computeIfAbsent(collectionName, name -> {
            log.info("创建项目 VectorStore: userId={}, projectId={}, collection={}",
                    userId, projectId, name);

            VectorStore vectorStore = ChromaVectorStore.builder(chromaApi, embeddingModel)
                    .collectionName(name)
                    .tenantName(tenantName)
                    .databaseName(databaseName)
                    .initializeSchema(true)
                    .build();

            log.info("✅ VectorStore 创建成功: {}", name);
            return vectorStore;
        });
    }

    /**
     * 生成 Collection 名称
     *
     * <p>格式：u_{userHash}_p_{projectHash}</p>
     * <p>示例：u_a1b2c3d4_p_e5f6g7h8</p>
     *
     * @param userId    用户 ID
     * @param projectId 项目 ID
     * @return Collection 名称
     */
    public String generateCollectionName(Long userId, Long projectId) {
        String userHash = sha256Hash(userId.toString()).substring(0, 8);
        String projectHash = sha256Hash(projectId.toString()).substring(0, 8);
        return String.format("u_%s_p_%s", userHash, projectHash);
    }

    /**
     * 删除项目的 Collection
     *
     * <p>说明：</p>
     * 当项目被删除时，应调用此方法清理向量数据。
     *
     * @param userId    用户 ID
     * @param projectId 项目 ID
     * @return 是否删除成功
     */
    public boolean deleteCollection(Long userId, Long projectId) {
        String collectionName = generateCollectionName(userId, projectId);

        try {
            // ChromaApi.deleteCollection 需要 tenantName, databaseName, collectionName
            chromaApi.deleteCollection(tenantName, databaseName, collectionName);
            vectorStoreCache.remove(collectionName);
            log.info("✅ Collection 已删除: {}", collectionName);
            return true;
        } catch (Exception e) {
            // Collection 不存在时忽略
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                log.info("Collection 不存在，无需删除: {}", collectionName);
                vectorStoreCache.remove(collectionName);
                return true;
            }
            log.error("❌ 删除 Collection 失败: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 检查 Collection 是否存在（基于缓存）
     *
     * @param userId    用户 ID
     * @param projectId 项目 ID
     * @return 是否在缓存中存在
     */
    public boolean collectionExistsInCache(Long userId, Long projectId) {
        String collectionName = generateCollectionName(userId, projectId);
        return vectorStoreCache.containsKey(collectionName);
    }

    /**
     * 清除缓存（用于测试或重置）
     */
    public void clearCache() {
        vectorStoreCache.clear();
        log.info("VectorStore 缓存已清除");
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的 VectorStore 数量
     */
    public int getCacheSize() {
        return vectorStoreCache.size();
    }

    /**
     * SHA256 哈希
     *
     * @param input 输入字符串
     * @return 十六进制哈希值
     */
    private String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是标准算法，不应该抛出此异常
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
