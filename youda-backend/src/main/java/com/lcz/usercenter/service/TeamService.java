package com.lcz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcz.usercenter.model.domain.Team;
import com.lcz.usercenter.model.dto.request.ListTeamsRequest;
import com.lcz.usercenter.model.dto.vo.TeamUserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author l1853
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2026-01-28 10:20:37
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     * @param team 队伍
     * @return 队伍 id
     */
    public Long addTeam(Team team, HttpServletRequest request);

    /**
     * 查询队伍列表
     * @param listTeamsRequest 请求参数封装类
     * @param request 请求对象
     * @return 队伍及关联用户信息
     */
    List<TeamUserVo> listTeams(ListTeamsRequest listTeamsRequest, HttpServletRequest request);
}
