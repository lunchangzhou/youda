package com.lcz.usercenter.model.dto.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * @author 1onetw
 * @version 1.0
 * @Description: 加入队伍请求体
 */
@Data
public class JoinTeamRequest implements Serializable {
    private static final long serialVersionUID = -7097518719411015663L;

    /**
     * id
     */
    private Long id;

    /**
     * 密码
     */
    private String password;
}
