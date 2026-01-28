package com.lcz.usercenter.service.impl;
import java.util.Date;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcz.usercenter.common.ErrorCode;
import com.lcz.usercenter.exception.BusinessException;
import com.lcz.usercenter.mapper.TeamMapper;
import com.lcz.usercenter.model.domain.Team;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.model.domain.UserTeam;
import com.lcz.usercenter.model.enums.TeamStatusEnum;
import com.lcz.usercenter.service.TeamService;
import com.lcz.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
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
}




