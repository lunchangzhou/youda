package com.lcz.usercenter.constant;

/**
* @author lcz
* @version 1.0
*/
public interface UserConstant {
    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;

    /**
     * 用户登录态
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 盐值——混淆密码
     */
    String SALT = "lcz";

}
