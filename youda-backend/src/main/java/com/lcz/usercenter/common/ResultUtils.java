package com.lcz.usercenter.common;

/**
 * 返回工具类
 * @author 1onetw
 * @version 1.0
 */
public class ResultUtils {

    /**
     * 成功
     * @param data 数据
     * @return BaseResponse
     * @param <T> 泛型
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "success");
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @return BaseResponse
     * @param <T> 泛型
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    public static <T> BaseResponse<T> error(int code, String message, String description) {
        return new BaseResponse<>(code, message, description);
    }
}
