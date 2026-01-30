package com.lcz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lcz.usercenter.common.ErrorCode;
import com.lcz.usercenter.exception.BusinessException;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.model.dto.request.UserUpdateRequest;
import com.lcz.usercenter.model.dto.vo.UserVo;
import com.lcz.usercenter.service.UserService;
import com.lcz.usercenter.mapper.UserMapper;
import com.lcz.usercenter.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
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
        safetyUser.setTags(originUser.getTags());
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
        if (userList.size() > (int) userPage.getSize()){
            userList = userList.subList(0, (int) userPage.getSize());
        }
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

    @Override
    @SuppressWarnings("unchecked")
    public Page<User> recommendUsers(long pageNum, long pageSize, HttpServletRequest request) {
        // 1.参数校验
        if (pageNum <= 0 || pageSize <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页数和页大小需大于 0");
        }
        // 2.鉴权
        User loginUser = this.getLoginUser(request);
        if (loginUser == null){
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        String redisKey = String.format("youda:user:recommendUsers:%s", loginUser.getId());
        // 3.核心业务逻辑
        // 如果有缓存，直接读取
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null){
            return userPage;
        }
        // 没有缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        // 写缓存，10s 过期
        try {
            valueOperations.set(redisKey, userPage,10000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("推荐用户写缓存失败...");
        }
        return userPage;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(USER_LOGIN_STATE);
    }

    @Override
    public List<UserVo> matchUsers(long num, HttpServletRequest request) {
        // 获取当前登录用户的字符列表
        User loginUser = this.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 查询全部用户, 但过滤掉 tags 为空的用户, 且只查询 id 和 tags, 查询性能可提升 30%
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags").select("id", "tags");
        List<User> userList = this.list(queryWrapper);
        // 计算每个用户的匹配相似度, Map 中格式（用户列表的下标:相似度）
        SortedMap<Integer, Long> indexDistanceMap = new TreeMap<>();
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            //无标签的
            if (StringUtils.isBlank(userTags)) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            indexDistanceMap.put(i, distance);
        }
        //下面这个是打印前num个的id和分数
        List<UserVo> userVoList = new ArrayList<>();
        int i = 0;
        for (Map.Entry<Integer, Long> entry : indexDistanceMap.entrySet()) {
            if (i >= num) {
                break;
            }
            User user = userList.get(entry.getKey());
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            System.out.println("用户 id: " + userVo.getId() + "; 用户匹配相似度(越小越相似): " + entry.getValue());
            userVoList.add(userVo);
            i++;
        }
        return userVoList;
    }
}




