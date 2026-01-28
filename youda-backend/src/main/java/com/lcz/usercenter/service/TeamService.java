package com.lcz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcz.usercenter.model.domain.Team;
import com.lcz.usercenter.model.request.AddTeamRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author l1853
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2026-01-28 10:20:37
*/
public interface TeamService extends IService<Team> {
    /**
     *
     * @param team 队伍
     * @return 队伍 id
     */
    public Long addTeam(Team team, HttpServletRequest request);
}
