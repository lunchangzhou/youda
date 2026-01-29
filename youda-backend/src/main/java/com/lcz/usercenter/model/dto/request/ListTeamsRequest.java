package com.lcz.usercenter.model.dto.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 1onetw
 * @version 1.0
 * @Description:
 */
@Data
public class ListTeamsRequest implements Serializable {

    private static final long serialVersionUID = -6953610722883320487L;

    /**
     * 搜索关键词（支持根据关键词同时对队伍名称和描述搜索）
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 用户id
     */
    private Long userId;

}
