package com.dpbug.server.controller.user;

import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.user.UserApiConfigRequest;
import com.dpbug.server.model.vo.user.UserApiConfigVO;
import com.dpbug.server.service.user.UserApiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户API配置Controller
 *
 * @author dpbug
 */
@Slf4j
@RestController
@RequestMapping("/api/userConfig")
@RequiredArgsConstructor
public class UserApiConfigController {

    private final UserApiConfigService userApiConfigService;

    /**
     * 保存API配置
     *
     * @param dto 配置DTO
     * @return 配置ID
     */
    @PostMapping("/save")
    public Result<Long> saveConfig(@Validated @RequestBody UserApiConfigRequest dto) {
        log.info("用户{}保存API配置", dto.getUserId());
        Long configId = userApiConfigService.saveConfig(dto);
        return Result.success(configId);
    }

    /**
     * 更新API配置
     *
     * @param dto 配置DTO
     * @return 是否成功
     */
    @PostMapping("/update")
    public Result<Boolean> updateConfig(@Validated @RequestBody UserApiConfigRequest dto) {
        log.info("用户{}更新API配置，配置ID：{}", dto.getUserId(), dto.getId());
        boolean result = userApiConfigService.updateConfig(dto);
        return Result.success(result);
    }

    /**
     * 删除API配置
     *
     * @param dto 配置DTO（需要userId和id）
     * @return 是否成功
     */
    @PostMapping("/delete")
    public Result<Boolean> deleteConfig(@RequestBody UserApiConfigRequest dto) {
        log.info("用户{}删除API配置，配置ID：{}", dto.getUserId(), dto.getId());
        boolean result = userApiConfigService.deleteConfig(dto.getUserId(), dto.getId());
        return Result.success(result);
    }

    /**
     * 设置默认配置
     *
     * @param dto 配置DTO（需要userId和id）
     * @return 是否成功
     */
    @PostMapping("/setDefault")
    public Result<Boolean> setDefaultConfig(@RequestBody UserApiConfigRequest dto) {
        log.info("用户{}设置默认API配置，配置ID：{}", dto.getUserId(), dto.getId());
        boolean result = userApiConfigService.setDefaultConfig(dto.getUserId(), dto.getId());
        return Result.success(result);
    }

    /**
     * 获取用户的所有API配置
     *
     * @param userId 用户ID
     * @return 配置列表
     */
    @GetMapping("/list")
    public Result<List<UserApiConfigVO>> listUserConfigs(@RequestParam Long userId) {
        log.info("查询用户{}的所有API配置", userId);
        List<UserApiConfigVO> configs = userApiConfigService.listUserConfigs(userId);
        return Result.success(configs);
    }

    /**
     * 验证API配置
     *
     * @param dto 配置DTO
     * @return 是否有效
     */
    @PostMapping("/validate")
    public Result<Boolean> validateConfig(@Validated @RequestBody UserApiConfigRequest dto) {
        log.info("验证API配置");
        boolean result = userApiConfigService.validateConfig(dto);
        return Result.success(result);
    }
}