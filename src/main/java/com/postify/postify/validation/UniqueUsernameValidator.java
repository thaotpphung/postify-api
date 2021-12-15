package com.postify.postify.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.postify.postify.entity.User;
import com.postify.postify.repository.UserRepository;
import com.postify.postify.validation.constraint.UniqueUsername;
import org.springframework.beans.factory.annotation.Autowired;

public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String>{
	
	@Autowired
	UserRepository userRepository;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		
		User inDB = userRepository.findByUsername(value);
		if(inDB == null) {
			return true;
		}

		return false;
	}

}
