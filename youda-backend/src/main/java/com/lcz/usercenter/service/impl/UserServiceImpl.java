package com.lcz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lcz.usercenter.common.ErrorCode;
import com.lcz.usercenter.exception.BusinessException;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.model.request.UserUpdateRequest;
import com.lcz.usercenter.service.UserService;
import com.lcz.usercenter.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.lcz.usercenter.constant.UserConstant.*;

/**
* @author l1853
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-06-20 05:11:17
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper;
    @Autowired
    private Gson gson;

    /**
     * 用户注册
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验码
     * @param planetCode 星球编号
     * @return 新用户 id
     */
    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword, planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户长度小于4位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8位");
        }
        if (planetCode.length() > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号大于5位");
        }

        // 账户不包含特殊字符
        Pattern pattern = Pattern.compile("[!@#$%^&*(){}\\[\\]:;\"',.<>?/~`]");
        Matcher matcher = pattern.matcher(userAccount);
        if (matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户包含特殊字符");
        }

        // 校验密码是否相同
        if (!checkPassword.equals(userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        }

        // 账户不能重复
        QueryWrapper <User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = this.count(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户重复");
        }

        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode",planetCode);
        count = this.count(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号重复");
        }

        // 2.加密 —— 先做一个测试【在项目测试类中测局部代码】
        String encyptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encyptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request HttpServletRequest对象
     * @return 脱敏后用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        /* 1.校验 */
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户长度小于4位");
        }
        if (userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8位");
        }

        // 账户不包含特殊字符
        Pattern pattern = Pattern.compile("[!@#$%^&*(){}\\[\\]:;\"',.<>?/~`]");
        Matcher matcher = pattern.matcher(userAccount);
        if (matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户包含特殊字符");
        }

        /* 2.查询用户是否存在 */
        // 加密
        String encyptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encyptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null){
            log.info("user login failed, userPassword is not correct.");
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }

        /* 3.用户脱敏 */
        User safetyUser = getSafetyUser(user);

        /* 4.记录用户登录态（session）*/
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 判断是否为管理员
     * @param request HttpServletRequest对象
     * @return 布尔值
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User safetyUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (safetyUser == null || safetyUser.getUserRole() != ADMIN_ROLE){
            return false;
        }
        return true;
    }

    /**
     * 判断是否为管理员
     * @param loginUser 当前登录用户
     * @return 布尔值
     */
    @Override
    public boolean isAdmin(User loginUser) {
        if (loginUser == null || loginUser.getUserRole() != ADMIN_ROLE){
            return false;
        }
        return true;
    }

    /**
     * 获取脱敏用户
     * @param originUser 未脱敏用户
     * @return 脱敏用户
     */
    @Override
    public User getSafetyUser(User originUser) {
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request HttpServletRequest对象
     * @return 1 注销成功
     */
    @Override
    public Integer userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签查询用户（SQL 版）
     * @param tags 标签列表
     * @return 用户列表
     */
    @Deprecated
    @Override
    public List<User> searchUsersByTagsBySql(Page<User> userPage, List<String> tags) {
        // start 计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 1.校验
        if (CollectionUtils.isEmpty(tags)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 2.构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tag : tags){
            queryWrapper.like("tags", tag);
        }
        // 3.执行查询，获取用户列表
        List<User> userList = userMapper.selectPage(userPage, queryWrapper).getRecords();
        // 4.进行脱敏，返回脱敏用户列表
        userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
        // stop 计时
        stopWatch.stop();
        log.info("基于SQL根据标签查询用户：用户列表条数（{}），耗时（{}）", userList.size(), stopWatch.getTotalTimeMillis());
        return userList;
    }

    /**
     * 根据标签查询用户（内存查询版）
     * @param tags 标签列表
     * @return 用户列表
     */
    @Override
    public List<User> searchUsersByTagsByMemory(Page<User> userPage, List<String> tags) {
        // start 计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 构造最终查询用户列表
        List<User> userList = new ArrayList<>();
        // 1.校验
        if (CollectionUtils.isEmpty(tags)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        while (userList.size() < userPage.getSize()){
            // 2.查询所有用户
            List<User> userListTemp = userMapper.selectPage(userPage, new QueryWrapper<>()).getRecords();
            // 3.内存中根据标签查询用户,再进行脱敏并返回用户列表
            userListTemp = userListTemp.stream().filter(user -> {
                String tagStr = user.getTags();
                if (StringUtils.isEmpty(tagStr)) {
                    return false;
                }
                Set<String> tagNameSet = gson.fromJson(tagStr, new TypeToken<Set<String>>() {
                }.getType());
                for (String tag : tags) {
                    if (!tagNameSet.contains(tag)) {
                        return false;
                    }
                }
                return true;
            }).map(this::getSafetyUser).collect(Collectors.toList());
            userList.addAll(userListTemp);
        }
        userList = userList.subList(0, (int) userPage.getSize());
        // stop 计时
        stopWatch.stop();
        log.info("基于内存根据标签查询用户：用户列表条数（{}），耗时（{}）", userList.size(), stopWatch.getTotalTimeMillis());
        return userList;
    }

    @Override
    public int updateUser(UserUpdateRequest userUpdateRequest, User loginUser) {
        // 确保要修改的用户存在
        Long userId = userUpdateRequest.getId();
        User usernew = userMapper.selectById(userId);
        if (usernew == null){
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求数据为空");
        }
        // 根据要修改的用户信息生成新的用户对象
        usernew.setUsername(userUpdateRequest.getUsername());
        usernew.setAvatarUrl(userUpdateRequest.getAvatarUrl());
        usernew.setGender(userUpdateRequest.getGender());
        usernew.setPhone(userUpdateRequest.getPhone());
        usernew.setEmail(userUpdateRequest.getEmail());
        usernew.setTags(userUpdateRequest.getTags());
        // 若为管理员，允许修改所有用户
        // 若不为管理员，只允许修改当前用户
        if (!isAdmin(loginUser) && !userId.equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH, "没有管理员权限");
        }
        return userMapper.updateById(usernew);
    }
}




