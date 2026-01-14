package com.lcz.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author lcz
 * @version 1.0
 * 根据标签查询用户请求体
 */
@Data
public class UserSearchByTagsRequest implements Serializable {

    /**
     * 当前页数
     */
    private long pageNum;

    /**
     * 每页大小
     */
    private long pageSize;

    /**
     * 用户标签
     */
    private List<String> tags;
}
