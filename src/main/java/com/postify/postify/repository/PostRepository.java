package com.postify.postify.repository;

import com.postify.postify.entity.Post;
import com.postify.postify.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post>{
	
	Page<Post> findByUser(User user, Pageable pageable);
}
