package com.unibook.publisher.identity.web;

import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.identity.entity.response.UserResponse;
import com.unibook.publisher.identity.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {
    private final UserService userService;

    public GlobalModelAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public UserResponse populateCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null)
            return null;

        return userService.getUserProfile(user.id());
    }
}
