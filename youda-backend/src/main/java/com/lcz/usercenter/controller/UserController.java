package com.lcz.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcz.usercenter.common.BaseResponse;
import com.lcz.usercenter.common.ErrorCode;
import com.lcz.usercenter.common.ResultUtils;
import com.lcz.usercenter.exception.BusinessException;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.model.request.UserLoginRequest;
import com.lcz.usercenter.model.request.UserRegisterRequest;
import com.lcz.usercenter.model.request.UserSearchByTagsRequest;
import com.lcz.usercenter.model.request.UserUpdateRequest;
import com.lcz.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
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
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    @PostMapping("login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String userName = userLoginRequest.getUserName();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userName, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User result = userService.userLogin(userName, userPassword, request);
        return ResultUtils.success(result);
    }

    @GetMapping("search")
    public BaseResponse<List<User>> searchUsers(long pageNum, long pageSize, String userName, HttpServletRequest request) {
        /* 1.鉴权 */
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "没有管理员权限");
        }
        /* 2.查询用户列表 */
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 用户名不为空
        if (StringUtils.isNotBlank(userName)) {
            queryWrapper.like(true, "username", userName);
        }
        List<User> userList = userService.page(new Page<>(pageNum, pageSize), queryWrapper).getRecords();
        List<User> result = userList.stream().map(user ->
        {
            // 用户脱敏
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(result);
    }

    @PostMapping("delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        /* 1.鉴权 */
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "没有管理员权限");
        }
        /* 2.删除用户 */
        if (id <= 0) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(id);
        return ResultUtils.success(result);
    }

    @PostMapping("logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        Integer result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @PostMapping("searchUsersByTags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestBody UserSearchByTagsRequest userSearchByTagsRequest) {
        List<String> tags = userSearchByTagsRequest.getTags();
        long pageNum = userSearchByTagsRequest.getPageNum();
        long pageSize = userSearchByTagsRequest.getPageSize();
        // 1.校验
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 2.查询
        List<User> userList = userService.searchUsersByTagsBySql(new Page<User>(pageNum, pageSize), tags);
        return ResultUtils.success(userList);
    }

    @PostMapping("updateUser")
    public BaseResponse<Integer> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        // 1.校验
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未登录");
        }
        // 2.更新
        int result = userService.updateUser(userUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

}
