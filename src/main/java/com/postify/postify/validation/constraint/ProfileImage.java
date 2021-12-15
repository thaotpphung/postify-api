package com.postify.postify.validation.constraint;

import com.postify.postify.validation.ProfileImageValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = ProfileImageValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProfileImage {

	String message() default "{postify.constraints.image.ProfileImage.message}";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };
}
