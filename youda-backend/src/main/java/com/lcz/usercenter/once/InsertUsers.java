package com.lcz.usercenter.once;

import com.lcz.usercenter.model.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author 1onetw
 * @version 1.0
 */
@Component
public class InsertUsers {
    @Scheduled(fixedRate = 60000)
    public void doInsertUsers() {
        User user = new User();
    }
}
