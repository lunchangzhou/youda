package com.lcz.usercenter.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.model.dto.request.UserUpdateRequest;
import com.lcz.usercenter.model.dto.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author l1853
* @description 针对表【user】的数据库操作Service
* @createDate 2025-06-20 05:11:17
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验码
     * @param planetCode 星球编号
     * @return 新用户 id
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request HttpServletRequest对象
     * @return 脱敏后用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 判断是否为管理员
     * @param request HttpServletRequest对象
     * @return 布尔值
     */
    boolean isAdmin(HttpServletRequest request);

    boolean isAdmin(User loginUser);

    /**
     * 获取脱敏用户
     * @param originUser 未脱敏用户
     * @return 脱敏用户
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     *
     * @param request HttpServletRequest对象
     * @return 1 注销成功
     */
    Integer userLogout(HttpServletRequest request);

    /**
     * 根据标签查询用户（SQL 版）
     * @param tags 标签列表
     * @return 用户列表
     */
    List<User> searchUsersByTagsBySql(Page<User> userPage, List<String> tags);

    /**
     * 根据标签查询用户（内存查询版）
     * @param tags 标签列表
     * @return 用户列表
     */
    List<User> searchUsersByTagsByMemory(Page<User> userPage,  List<String> tags);

    /**
     * 更新用户信息
     * @param userUpdateRequest 待更新的用户信息
     * @param loginUser 当前登录的用户信息
     * @return 更新的用户 ID
     */
    int updateUser(UserUpdateRequest userUpdateRequest, User loginUser);

    /**
     * 推荐用户
     * @param pageNum 页数
     * @param pageSize 页大小
     * @return 推荐的用户列表
     */
    Page<User> recommendUsers( long pageNum, long pageSize, HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request 请求对象
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 匹配用户
     * @param num 个数
     * @param request 请求对象
     * @return 匹配的用户列表
     */
    List<UserVo> matchUsers(long num, HttpServletRequest request);
}
