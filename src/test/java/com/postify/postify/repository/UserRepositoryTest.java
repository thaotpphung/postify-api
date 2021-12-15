package com.postify.postify.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.postify.postify.TestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.postify.postify.entity.User;
import com.postify.postify.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {
	
	@Autowired
	TestEntityManager testEntityManager;
	
	@Autowired
	UserRepository userRepository;
	
	@Test
	public void findByUsername_whenUserExists_returnsUser() {
		testEntityManager.persist(TestUtil.createValidUser());
		
		User inDB = userRepository.findByUsername("test-user");
		assertThat(inDB).isNotNull();
	}
	
	@Test
	public void findByUsername_whenUserDoesNotExist_returnsNull() {
		User inDB = userRepository.findByUsername("nonexistinguser");
		assertThat(inDB).isNull();
	}

}
