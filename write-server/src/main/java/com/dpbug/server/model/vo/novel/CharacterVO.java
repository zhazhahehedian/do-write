package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author dpbug
 * @description 角色响应
 */
@Data
public class CharacterVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 角色id
     */
    private Long id;
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 角色名称
     */
    private String name;
    /**
     * 是否组织
     */
    private Integer isOrganization;

    // 角色信息
    /**
     * 角色类型
     */
    private String roleType;
    /**
     * 年龄
     */
    private Integer age;
    /**
     * 性别
     */
    private String gender;
    /**
     * 外貌
     */
    private String appearance;
    /**
     * 性格
     */
    private String personality;
    /**
     * 背景
     */
    private String background;
    /**
     * 关系网
     */
    private Map<String, Object> relationships;

    // 组织信息
    /**
     * 组织类型
     */
    private String organizationType;
    /**
     * 组织目的
     */
    private String organizationPurpose;
    /**
     * 组织成员
     */
    private List<String> organizationMembers;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
