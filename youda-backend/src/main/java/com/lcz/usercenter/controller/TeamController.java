package com.lcz.usercenter.controller;

import com.lcz.usercenter.common.BaseResponse;
import com.lcz.usercenter.common.ErrorCode;
import com.lcz.usercenter.common.ResultUtils;
import com.lcz.usercenter.exception.BusinessException;
import com.lcz.usercenter.model.domain.Team;
import com.lcz.usercenter.model.dto.request.AddTeamRequest;
import com.lcz.usercenter.model.dto.request.ListTeamsRequest;
import com.lcz.usercenter.model.dto.vo.TeamUserVo;
import com.lcz.usercenter.service.TeamService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    @PostMapping("/list")
    public BaseResponse<List<TeamUserVo>> listTeams(@RequestBody ListTeamsRequest listTeamsRequest, HttpServletRequest request) {
        // 1.参数校验
        if (listTeamsRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 2.查询队伍列表
        List<TeamUserVo> teamUserVos = teamService.listTeams(listTeamsRequest, request);
        return ResultUtils.success(teamUserVos);
    }
}
