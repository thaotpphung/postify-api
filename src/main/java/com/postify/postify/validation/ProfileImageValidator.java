package com.postify.postify.validation;

import java.util.Base64;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.postify.postify.validation.constraint.ProfileImage;
import org.springframework.beans.factory.annotation.Autowired;

import com.postify.postify.service.FileService;

public class ProfileImageValidator implements ConstraintValidator<ProfileImage, String>{
	
	@Autowired
	FileService fileService;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if(value == null) {
			return true;
		}
		
		byte[] decodedBytes = Base64.getDecoder().decode(value);
		String fileType = fileService.detectType(decodedBytes);
		if(fileType.equalsIgnoreCase("image/png") || fileType.equalsIgnoreCase("image/jpeg")) {
			return true;
		}
		
		return false;
	}

}
