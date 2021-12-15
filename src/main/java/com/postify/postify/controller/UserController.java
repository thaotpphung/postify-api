package com.postify.postify.controller;

import javax.validation.Valid;

import com.postify.postify.entity.User;
import com.postify.postify.validation.constraint.CurrentUser;
import com.postify.postify.model.GenericResponse;
import com.postify.postify.model.UserUpdateVM;
import com.postify.postify.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.postify.postify.model.UserVM;

@RestController
@RequestMapping("/api/1.0")
public class UserController {

	@Autowired
	UserService userService;
	
	@PostMapping("/users")
    GenericResponse createUser(@Valid @RequestBody User user) {
		userService.save(user);
		return new GenericResponse("User saved");
	}
	
	@GetMapping("/users")
	Page<UserVM> getUsers(@CurrentUser User loggedInUser, Pageable page) {
		return userService.getUsers(loggedInUser, page).map(UserVM::new);
	}
	
	@GetMapping("/users/{username}")
	UserVM getUserByName(@PathVariable String username) {
		User user = userService.getByUsername(username);
		return new UserVM(user);
	}
	
	@PutMapping("/users/{id:[0-9]+}")
	@PreAuthorize("#id == principal.id")
	UserVM updateUser(@PathVariable long id, @Valid @RequestBody(required = false) UserUpdateVM userUpdate) {
		User updated = userService.update(id, userUpdate);
		return new UserVM(updated);
	}
}
