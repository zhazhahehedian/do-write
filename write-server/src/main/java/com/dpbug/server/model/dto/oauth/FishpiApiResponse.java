package com.dpbug.server.model.dto.oauth;

import lombok.Data;

/**
 * FishPi API 响应包装类
 *
 * FishPi 的 API 返回格式为：
 * {
 *     "msg": "",
 *     "code": 0,
 *     "data": { ... }
 * }
 *
 * @author dpbug
 */
@Data
public class FishpiApiResponse {

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应码（0 表示成功）
     */
    private Integer code;

    /**
     * 用户信息数据
     */
    private FishpiUserInfo data;
}
