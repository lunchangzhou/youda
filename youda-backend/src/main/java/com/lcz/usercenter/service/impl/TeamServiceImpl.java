package com.lcz.usercenter.service.impl;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcz.usercenter.common.ErrorCode;
import com.lcz.usercenter.exception.BusinessException;
import com.lcz.usercenter.mapper.TeamMapper;
import com.lcz.usercenter.model.domain.Team;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.model.domain.UserTeam;
import com.lcz.usercenter.model.dto.enums.TeamStatusEnum;
import com.lcz.usercenter.model.dto.request.ListTeamsRequest;
import com.lcz.usercenter.model.dto.request.UpdateTeamRequest;
import com.lcz.usercenter.model.dto.vo.TeamUserVo;
import com.lcz.usercenter.model.dto.vo.UserVo;
import com.lcz.usercenter.service.TeamService;
import com.lcz.usercenter.service.UserService;
import com.lcz.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.lcz.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author l1853
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2026-01-28 10:20:37
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {
    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;

    @Transactional(rollbackFor = BusinessException.class)
    @Override
    public Long addTeam(Team team, HttpServletRequest request) {
        // 1.鉴权: 是否登录
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        // 2.参数校验:
        // 参数是否为空
        Integer maxNum = team.getMaxNum();
        String description = team.getDescription();
        String name = team.getName();
        Date expireTime = team.getExpireTime();
        Integer status = team.getStatus();
        String password = team.getPassword();
        if (StringUtils.isBlank(description) || StringUtils.isBlank(name)  || expireTime == null || password == null || maxNum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
//         队伍人数 > 1 且 <= 20
        if (maxNum <= 1 || maxNum > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符合要求");
        }
//         队伍标题 <= 20
        if (name.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称不符合要求");
        }
//         描述 <= 512
        if (description.length() > 512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不符合要求");
        }
//         status 是否大于 0，不传默认为 0（公开）
        status = Optional.ofNullable(status).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不符合要求");
        }
//         如果 status 是加密状态，一定要有密码，且密码 <= 32
        if ("加密".equals(statusEnum.getText())){
            if (StringUtils.isBlank(password) || password.length() > 32){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码不符合要求");
            }
        }
//         超时时间 > 当前时间
        if (new Date().getTime() > expireTime.getTime()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不符合要求");
        }
//         校验用户最多创建 5 个队伍
        // todo: 要避免用户可能短时间多次点击创建 100 个队伍，可加锁
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("id", loginUser.getId());
        int hasTeamNum = this.count(teamQueryWrapper);
        if (hasTeamNum >= 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        // 开启事务：插入队伍信息到队伍表；插入用户队伍关系到关系表
        team.setUserId(loginUser.getId());
        boolean save = this.save(team);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        Long teamId = team.getId();
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(loginUser.getId());
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        boolean result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVo> listTeams(ListTeamsRequest listTeamsRequest, HttpServletRequest request) {
        // 1.参数校验
        // 必传参数不能为空
        if (listTeamsRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String searchText = listTeamsRequest.getSearchText();
        String name = listTeamsRequest.getName();
        String description = listTeamsRequest.getDescription();
        Integer maxNum = listTeamsRequest.getMaxNum();
        Integer status = listTeamsRequest.getStatus();
        Long userId = listTeamsRequest.getUserId();
        // 非必传参数是否为空，不为空则作为查询条件
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(searchText)){
            teamQueryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
        }
        if (StringUtils.isNotBlank(name)){
            teamQueryWrapper.like("name", name);
        }
        if (StringUtils.isNotBlank(description)){
            teamQueryWrapper.like("description", description);
        }
        if (maxNum != null) {
            teamQueryWrapper.eq("maxNum", maxNum);
        }
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            statusEnum = TeamStatusEnum.PUBLIC;
        }
        // 2.鉴权：只有管理员才能查看私密队伍
        if (statusEnum == TeamStatusEnum.PRIVATE && !userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        teamQueryWrapper.eq("status", statusEnum.getValue());
        if (userId != null) {
            teamQueryWrapper.eq("userId", userId);
        }
        // 3.核心业务逻辑
        // 不展示已过期的队伍
        teamQueryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        // 查询队伍列表
        ArrayList<TeamUserVo> teamUserVos = new ArrayList<>();
        List<Team> teams = this.list(teamQueryWrapper);
        // 关联查询队伍创建者的信息
        for (Team team : teams) {
            TeamUserVo teamUserVo = new TeamUserVo();
            BeanUtils.copyProperties(team, teamUserVo);
            Long createUserId = team.getUserId();
            if (createUserId == null) {
                continue;
            }
            User createUser = userService.getById(createUserId);
            UserVo userVo = new UserVo();
            if (createUser != null) {
                // 脱敏用户信息
                BeanUtils.copyProperties(createUser, userVo);
                teamUserVo.setUserVo(userVo);
            }
            teamUserVos.add(teamUserVo);
        }
        return teamUserVos;
    }

    @Override
    public Boolean updateTeam(UpdateTeamRequest updateTeamRequest, HttpServletRequest request) {
        // 1.参数校验：必传参数不能为空
        if (updateTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = updateTeamRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 2.鉴权
        // 查询队伍是否存在
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        // 只有管理员或创建者才可修改
        User loginUser = userService.getLoginUser(request);
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new  BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        // 3.核心业务逻辑
        // 如果队伍状态改为加密，则必须要有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(updateTeamRequest.getStatus());
        if (statusEnum == TeamStatusEnum.PRIVATE) {
            if (StringUtils.isBlank(updateTeamRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
        }
        // 修改队伍信息
        Team newTeam = new Team();
        BeanUtils.copyProperties(updateTeamRequest, newTeam);
        return this.updateById(newTeam);
    }
}




