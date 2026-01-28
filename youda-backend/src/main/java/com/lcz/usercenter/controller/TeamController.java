package com.lcz.usercenter.controller;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcz.usercenter.common.BaseResponse;
import com.lcz.usercenter.common.ErrorCode;
import com.lcz.usercenter.common.ResultUtils;
import com.lcz.usercenter.exception.BusinessException;
import com.lcz.usercenter.model.domain.Team;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.model.request.*;
import com.lcz.usercenter.service.TeamService;
import com.lcz.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.MapWriterTask;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.lcz.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author lcz
 * @version 2.0
 */
@RestController
@RequestMapping("/api/team")
@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
public class TeamController {
    @Resource
    private TeamService teamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody AddTeamRequest addTeamRequest, HttpServletRequest request) {
        // 1.参数校验
        if (addTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 2.添加用户
        Team team = new Team();
        BeanUtils.copyProperties(addTeamRequest, team);
        Long teamId = teamService.addTeam(team, request);
        return ResultUtils.success(teamId);
    }
}
