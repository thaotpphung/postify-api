package com.postify.postify.service;

import java.util.Date;
import java.util.List;

import com.postify.postify.entity.FileAttachment;
import com.postify.postify.entity.Post;
import com.postify.postify.repository.FileAttachmentRepository;
import com.postify.postify.repository.PostRepository;
import com.postify.postify.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class PostService {
	
	PostRepository postRepository;
	
	UserService userService;
	
	FileAttachmentRepository fileAttachmentRepository;
	
	FileService fileService;

	public PostService(PostRepository postRepository, UserService userService,
					   FileAttachmentRepository fileAttachmentRepository, FileService fileService) {
		super();
		this.postRepository = postRepository;
		this.userService = userService;
		this.fileAttachmentRepository = fileAttachmentRepository;
		this.fileService = fileService;
	}
	
	public Post save(User user, Post post) {
		post.setTimestamp(new Date());
		post.setUser(user);
		if(post.getAttachment() != null) {
			FileAttachment inDB = fileAttachmentRepository.findById(post.getAttachment().getId()).get();
			inDB.setPost(post);
			post.setAttachment(inDB);
		}
		return postRepository.save(post);
	}

	public Page<Post> getAllPosts(Pageable pageable) {
		return postRepository.findAll(pageable);
	}

	public Page<Post> getPostsOfUser(String username, Pageable pageable) {
		User inDB = userService.getByUsername(username);
		return postRepository.findByUser(inDB, pageable);
	}

	public Page<Post> getOldPosts(long id, String username, Pageable pageable) {
		Specification<Post> spec = Specification.where(idLessThan(id));
		if(username != null) {			
			User inDB = userService.getByUsername(username);
			spec = spec.and(userIs(inDB));
		}
		return postRepository.findAll(spec, pageable);
	}


	public List<Post> getNewPosts(long id, String username, Pageable pageable) {
		Specification<Post> spec = Specification.where(idGreaterThan(id));
		if(username != null) {			
			User inDB = userService.getByUsername(username);
			spec = spec.and(userIs(inDB));
		}
		return postRepository.findAll(spec, pageable.getSort());
	}

	public long getNewPostsCount(long id, String username) {
		Specification<Post> spec = Specification.where(idGreaterThan(id));
		if(username != null) {			
			User inDB = userService.getByUsername(username);
			spec = spec.and(userIs(inDB));
		}
		return postRepository.count(spec);
	}

	private Specification<Post> userIs(User user){
		return (root, query, criteriaBuilder) -> {
			return criteriaBuilder.equal(root.get("user"), user);
		};
	}

	private Specification<Post> idLessThan(long id){
		return (root, query, criteriaBuilder) -> {
			return criteriaBuilder.lessThan(root.get("id"), id);
		};
	}

	private Specification<Post> idGreaterThan(long id){
		return (root, query, criteriaBuilder) -> {
			return criteriaBuilder.greaterThan(root.get("id"), id);
		};
	}

	public void deletePost(long id) {
		Post post = postRepository.getOne(id);
		if(post.getAttachment() != null) {
			fileService.deleteAttachmentImage(post.getAttachment().getName());
		}
		postRepository.deleteById(id);
		
	}
	
	
	

}
