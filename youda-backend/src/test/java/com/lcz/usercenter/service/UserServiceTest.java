package com.lcz.usercenter.service;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcz.usercenter.UserCenterApplication;
import com.lcz.usercenter.model.domain.User;
import com.lcz.usercenter.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


/**
 * @author 1onetw
 * @version 1.0
 */
@SpringBootTest
public class UserServiceTest {
    @Resource
    private UserService userService;

    @Test
    public void testAddUser(){
        User user = new User();
        user.setUsername("Lczz");
        user.setUserAccount("123");
        user.setAvatarUrl("https://wp-cdn.4ce.cn/v2/7K35VGW.jpeg");
        user.setGender(0);
        user.setUserPassword("123456");
        user.setPhone("18532104295");
        user.setEmail("332879882@qq.com");
        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertTrue(result);
    }

    @Test
    void userRegister(){
        User user = new User();
        String userAccount = "lcz";
        String userPassword = "12345678";
        String checkPassword = "12345678";
        String planetCode = "1";
        long result = userService.userRegister(userAccount,userPassword,checkPassword, planetCode);
        Assertions.assertEquals(-1,result);

        userAccount = "Lczz";
        userPassword = "123456";
        checkPassword = "123456";
        result = userService.userRegister(userAccount,userPassword,checkPassword, planetCode);
        Assertions.assertEquals(-1,result);

        userAccount = "Lczz@";
        userPassword = "12345678";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount,userPassword,checkPassword, planetCode);
        Assertions.assertEquals(-1,result);

        userAccount = "Lczz";
        userPassword = "12345678";
        checkPassword = "123456";
        result = userService.userRegister(userAccount,userPassword,checkPassword, planetCode);
        Assertions.assertEquals(-1,result);

        userAccount = "Lczz";
        userPassword = "12345678";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount,userPassword,checkPassword, planetCode);
        Assertions.assertEquals(-1,result);

        userAccount = "Lhcc";
        userPassword = "12345678";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount,userPassword,checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        userAccount = "lunchangzhou";
        userPassword = "12345678";
        checkPassword = "12345678";
        planetCode = "2";
        result = userService.userRegister(userAccount,userPassword,checkPassword, planetCode);
        Assertions.assertTrue(result > 0);
    }

    // 434, 442, 417
    @Test
    void searchUsersByTagsBySql() {
        List<String> tags = Arrays.asList("Java", "Python");
        List<User> userList = userService.searchUsersByTagsBySql(new Page<>(1, 3), tags);
        Assertions.assertNotNull(userList);
    }

    // 432, 409, 413
    @Test
    void searchUsersByTagsByMemory() {
        List<String> tags = Arrays.asList("Java", "Python");
        List<User> userList = userService.searchUsersByTagsByMemory(new Page<>(1, 2), tags);
        for (User user : userList) {
            System.out.println(user);
        }
        Assertions.assertNotNull(userList);
    }
}