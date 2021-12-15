package com.postify.postify.controller;

import com.postify.postify.validation.constraint.CurrentUser;
import com.postify.postify.model.UserVM;
import com.postify.postify.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoginController {
	
	@PostMapping("/api/1.0/login")
    UserVM handleLogin(@CurrentUser User loggedInUser) {
		log.info("user login", loggedInUser.toString());
		return new UserVM(loggedInUser);
	}
	
}
