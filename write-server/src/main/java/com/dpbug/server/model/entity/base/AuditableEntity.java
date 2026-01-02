package com.dpbug.server.model.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可审计实体基类
 * <p>
 * 在 BaseEntity 基础上增加：createBy、updateBy、remark
 * <p>
 * 适用于需要记录操作人的实体，如 User 表
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AuditableEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 备注
     */
    private String remark;
}
