package com.postify.postify.service;

import java.util.Optional;

import com.postify.postify.entity.Post;
import com.postify.postify.entity.User;
import com.postify.postify.repository.PostRepository;
import org.springframework.stereotype.Service;

@Service
public class PostSecurityService {
	
	PostRepository postRepository;
	
	public PostSecurityService(PostRepository postRepository) {
		super();
		this.postRepository = postRepository;
	}

	public boolean isAllowedToDelete(long postId, User loggedInUser) {
		Optional<Post> optionalPost = postRepository.findById(postId);
		if(optionalPost.isPresent()) {
			Post inDB = optionalPost.get();
			return inDB.getUser().getId() == loggedInUser.getId();
		}
		return false;
	}

}
