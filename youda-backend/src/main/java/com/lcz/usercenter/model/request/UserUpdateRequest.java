package com.lcz.usercenter.model.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lcz
 * @version 1.0
 * 用户更新请求体
 */
@Data
public class UserUpdateRequest implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 昵称
     */
    private String username;
    
    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 性别 0 男 1 女
     */
    private Integer gender;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 标签列表 json
     */
    private String tags;
}
