package com.lcz.usercenter.model.dto.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 1onetw
 * @version 1.0
 * @Description:
 */
@Data
public class UserVo implements Serializable {
    private static final long serialVersionUID = 3159212217003164350L;
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
     *  用户状态 0 正常 1 ...
     */
    private Integer userStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
