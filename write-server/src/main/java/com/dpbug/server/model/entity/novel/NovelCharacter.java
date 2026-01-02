package com.dpbug.server.model.entity.novel;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 角色/组织表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_character")
public class NovelCharacter extends BaseEntity {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 是否组织 (0=角色, 1=组织)
     */
    @TableField("is_organization")
    private Integer isOrganization;

    /**
     * 角色类型 (protagonist/supporting/antagonist)
     */
    private String roleType;

    /**
     * 角色年龄
     */
    private Integer age;

    /**
     * 性别
     */
    private String gender;

    /**
     * 外貌描写
     */
    private String appearance;

    /**
     * 性格特点
     */
    private String personality;

    /**
     * 背景故事
     */
    private String background;

    /**
     * 关系网(与其他角色的关系)
     * JSON数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> relationships;

    /**
     * 组织类型
     */
    private String organizationType;

    /**
     * 组织目的
     */
    private String organizationPurpose;

    /**
     * 组织成员列表
     * JSON数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> organizationMembers;
}
