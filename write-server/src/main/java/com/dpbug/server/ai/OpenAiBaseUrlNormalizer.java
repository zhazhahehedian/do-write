package com.dpbug.server.ai;

/**
 * OpenAI 兼容接口的 baseUrl 规范化工具。
 *
 * <p>Spring AI 的 OpenAiApi 会自动拼接 {@code /v1/...} 路径，因此这里会把用户输入的 baseUrl
 * 末尾多余的 {@code /v1}（包括重复的 {@code /v1/v1}）去掉，避免最终请求变成 {@code /v1/v1/...}。</p>
 */
public final class OpenAiBaseUrlNormalizer {

    private OpenAiBaseUrlNormalizer() {
    }

    /**
     * 规范化 OpenAI 兼容接口的 baseUrl。
     *
     * <ul>
     *   <li>trim 空白</li>
     *   <li>去掉末尾多余的 {@code /}</li>
     *   <li>循环去掉末尾多余的 {@code /v1}</li>
     * </ul>
     *
     * @param baseUrl 原始 baseUrl
     * @return 规范化后的 baseUrl；若入参为空白则返回 null
     */
    public static String normalize(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        String normalized = baseUrl.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        normalized = trimTrailingSlash(normalized);

        // 处理 /v1、/v1/v1、/v1/v1/v1... 这类重复路径
        while (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
            normalized = trimTrailingSlash(normalized);
        }

        return normalized;
    }

    /**
     * 仅去掉末尾多余的斜杠，保留其它路径。
     */
    public static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

