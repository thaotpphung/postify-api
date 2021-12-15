package com.postify.postify.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import com.postify.postify.util.TestPage;
import com.postify.postify.TestUtil;
import com.postify.postify.entity.FileAttachment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import com.postify.postify.configuration.AppConfiguration;
import com.postify.postify.exception.ApiError;
import com.postify.postify.repository.FileAttachmentRepository;
import com.postify.postify.service.FileService;
import com.postify.postify.entity.Post;
import com.postify.postify.repository.PostRepository;
import com.postify.postify.service.PostService;
import com.postify.postify.model.PostVM;
import com.postify.postify.model.GenericResponse;
import com.postify.postify.entity.User;
import com.postify.postify.repository.UserRepository;
import com.postify.postify.service.UserService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PostControllerTest {

	private static final String API_1_0_POSTS = "/api/1.0/posts";

	@Autowired
	TestRestTemplate testRestTemplate;
	
	@Autowired
	UserService userService;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	PostRepository postRepository;
	
	@Autowired
	PostService postService;
	
	@Autowired
	FileAttachmentRepository fileAttachmentRepository;
	
	@Autowired
	FileService fileService;
	
	@Autowired
	AppConfiguration appConfiguration;
	
	@PersistenceUnit
	private EntityManagerFactory entityManagerFactory;
	
	@BeforeEach
	public void cleanup() throws IOException {
		fileAttachmentRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.deleteAll();
		testRestTemplate.getRestTemplate().getInterceptors().clear();
		FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
	}
	
	@Test
	public void postPost_whenPostIsValidAndUserIsAuthorized_receiveOk() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = TestUtil.createValidPost();
		ResponseEntity<Object> response = postPost(post, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	

	@Test
	public void postPost_whenPostIsValidAndUserIsUnauthorized_receiveUnauthorized() {
		Post post = TestUtil.createValidPost();
		ResponseEntity<Object> response = postPost(post, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}
	
	@Test
	public void postPost_whenPostIsValidAndUserIsUnauthorized_receiveApiError() {
		Post post = TestUtil.createValidPost();
		ResponseEntity<ApiError> response = postPost(post, ApiError.class);
		assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}
	
	@Test
	public void postPost_whenPostIsValidAndUserIsAuthorized_postSavedToDatabase() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = TestUtil.createValidPost();
		postPost(post, Object.class);
		
		assertThat(postRepository.count()).isEqualTo(1);
	}
	
	@Test
	public void postPost_whenPostIsValidAndUserIsAuthorized_postSavedToDatabaseWithTimestamp() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = TestUtil.createValidPost();
		postPost(post, Object.class);
		
		Post inDB = postRepository.findAll().get(0);
		
		assertThat(inDB.getTimestamp()).isNotNull();
	}
	
	@Test
	public void postPost_whenPostContentNullAndUserIsAuthorized_receiveBadRequest() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = new Post();
		ResponseEntity<Object> response = postPost(post, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void postPost_whenPostContentLessThan10CharactersAndUserIsAuthorized_receiveBadRequest() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = new Post();
		post.setContent("123456789");
		ResponseEntity<Object> response = postPost(post, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
	
	@Test
	public void postPost_whenPostContentIs5000CharactersAndUserIsAuthorized_receiveOk() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = new Post();
		String veryLongString = IntStream.rangeClosed(1, 5000).mapToObj(i -> "x").collect(Collectors.joining());
		post.setContent(veryLongString);
		ResponseEntity<Object> response = postPost(post, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	@Test
	public void postPost_whenPostContentMoreThan5000CharactersAndUserIsAuthorized_receiveBadRequest() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = new Post();
		String veryLongString = IntStream.rangeClosed(1, 5001).mapToObj(i -> "x").collect(Collectors.joining());
		post.setContent(veryLongString);
		ResponseEntity<Object> response = postPost(post, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
	

	@Test
	public void postPost_whenPostContentNullAndUserIsAuthorized_receiveApiErrorWithValidationErrors() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = new Post();
		ResponseEntity<ApiError> response = postPost(post, ApiError.class);
		Map<String, String> validationErrors = response.getBody().getValidationErrors();
		assertThat(validationErrors.get("content")).isNotNull();
	}
	
	@Test
	public void postPost_whenPostIsValidAndUserIsAuthorized_postSavedWithAuthenticatedUserInfo() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = TestUtil.createValidPost();
		postPost(post, Object.class);
		
		Post inDB = postRepository.findAll().get(0);
		
		assertThat(inDB.getUser().getUsername()).isEqualTo("user1");
	}
	
	@Test
	public void postPost_whenPostIsValidAndUserIsAuthorized_postCanBeAccessedFromUserEntity() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = TestUtil.createValidPost();
		postPost(post, Object.class);
		
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		
		User inDBUser = entityManager.find(User.class, user.getId());
		assertThat(inDBUser.getPosts().size()).isEqualTo(1);
		
	}
	

	@Test
	public void postPost_whenPostIsValidAndUserIsAuthorized_receivePostVM() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = TestUtil.createValidPost();
		ResponseEntity<PostVM> response = postPost(post, PostVM.class);
		assertThat(response.getBody().getUser().getUsername()).isEqualTo("user1");
	}

	@Test
	public void postPost_whenPostHasFileAttachmentAndUserIsAuthorized_fileAttachmentPostRelationIsUpdatedInDatabase() throws IOException {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		
		MultipartFile file = createFile();
		
		FileAttachment savedFile = fileService.saveAttachment(file);
		
		Post post = TestUtil.createValidPost();
		post.setAttachment(savedFile);
		ResponseEntity<PostVM> response = postPost(post, PostVM.class);
		
		FileAttachment inDB = fileAttachmentRepository.findAll().get(0);
		assertThat(inDB.getPost().getId()).isEqualTo(response.getBody().getId());
	}

	@Test
	public void postPost_whenPostHasFileAttachmentAndUserIsAuthorized_postFileAttachmentRelationIsUpdatedInDatabase() throws IOException {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		
		MultipartFile file = createFile();
		
		FileAttachment savedFile = fileService.saveAttachment(file);
		
		Post post = TestUtil.createValidPost();
		post.setAttachment(savedFile);
		ResponseEntity<PostVM> response = postPost(post, PostVM.class);
		
		Post inDB = postRepository.findById(response.getBody().getId()).get();
		assertThat(inDB.getAttachment().getId()).isEqualTo(savedFile.getId());
	}

	@Test
	public void postPost_whenPostHasFileAttachmentAndUserIsAuthorized_receivePostVMWithAttachment() throws IOException {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		
		MultipartFile file = createFile();
		
		FileAttachment savedFile = fileService.saveAttachment(file);
		
		Post post = TestUtil.createValidPost();
		post.setAttachment(savedFile);
		ResponseEntity<PostVM> response = postPost(post, PostVM.class);
		
		assertThat(response.getBody().getAttachment().getName()).isEqualTo(savedFile.getName());
	}

	private MultipartFile createFile() throws IOException {
		ClassPathResource imageResource = new ClassPathResource("profile.png");
		byte[] fileAsByte = FileUtils.readFileToByteArray(imageResource.getFile());
		
		MultipartFile file = new MockMultipartFile("profile.png", fileAsByte);
		return file;
	}
	
	@Test
	public void getPosts_whenThereAreNoPosts_receiveOk() {
		ResponseEntity<Object> response = getPosts(new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	@Test
	public void getPosts_whenThereAreNoPosts_receivePageWithZeroItems() {
		ResponseEntity<TestPage<Object>> response = getPosts(new ParameterizedTypeReference<TestPage<Object>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(0);
	}

	@Test
	public void getPosts_whenThereArePosts_receivePageWithItems() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<Object>> response = getPosts(new ParameterizedTypeReference<TestPage<Object>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(3);
	}

	@Test
	public void getPosts_whenThereArePosts_receivePageWithPostVM() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<PostVM>> response = getPosts(new ParameterizedTypeReference<TestPage<PostVM>>() {});
		PostVM storedPost = response.getBody().getContent().get(0);
		assertThat(storedPost.getUser().getUsername()).isEqualTo("user1");
	}
	
	@Test
	public void getPostsOfUser_whenUserExists_receiveOk() {
		userService.save(TestUtil.createValidUser("user1"));
		ResponseEntity<Object> response = getPostsOfUser("user1", new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	@Test
	public void getPostsOfUser_whenUserDoesNotExist_receiveNotFound() {
		ResponseEntity<Object> response = getPostsOfUser("unknown-user", new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
	@Test
	public void getPostsOfUser_whenUserExists_receivePageWithZeroPosts() {
		userService.save(TestUtil.createValidUser("user1"));
		ResponseEntity<TestPage<Object>> response = getPostsOfUser("user1", new ParameterizedTypeReference<TestPage<Object>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(0);
	}

	@Test
	public void getPostsOfUser_whenUserExistWithPost_receivePageWithPostVM() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<PostVM>> response = getPostsOfUser("user1", new ParameterizedTypeReference<TestPage<PostVM>>() {});
		PostVM storedPost = response.getBody().getContent().get(0);
		assertThat(storedPost.getUser().getUsername()).isEqualTo("user1");
	}
	
	@Test
	public void getPostsOfUser_whenUserExistWithMultiplePosts_receivePageWithMatchingPostsCount() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<PostVM>> response = getPostsOfUser("user1", new ParameterizedTypeReference<TestPage<PostVM>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(3);
	}
	
	@Test
	public void getPostsOfUser_whenMultipleUserExistWithMultiplePosts_receivePageWithMatchingPostsCount() {
		User userWithThreePosts = userService.save(TestUtil.createValidUser("user1"));
		IntStream.rangeClosed(1, 3).forEach(i -> {
			postService.save(userWithThreePosts, TestUtil.createValidPost());	
		});
		
		User userWithFivePosts = userService.save(TestUtil.createValidUser("user2"));
		IntStream.rangeClosed(1, 5).forEach(i -> {
			postService.save(userWithFivePosts, TestUtil.createValidPost());	
		});
		
		
		ResponseEntity<TestPage<PostVM>> response = getPostsOfUser(userWithFivePosts.getUsername(), new ParameterizedTypeReference<TestPage<PostVM>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(5);
	}
	
	@Test
	public void getOldPosts_whenThereAreNoPosts_receiveOk() {
		ResponseEntity<Object> response = getOldPosts(5, new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	@Test
	public void getOldPosts_whenThereArePosts_receivePageWithItemsBeforeProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<Object>> response = getOldPosts(fourth.getId(), new ParameterizedTypeReference<TestPage<Object>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(3);
	}
	
	@Test
	public void getOldPosts_whenThereArePosts_receivePageWithPostVMBeforeProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<PostVM>> response = getOldPosts(fourth.getId(), new ParameterizedTypeReference<TestPage<PostVM>>() {});
		assertThat(response.getBody().getContent().get(0).getDate()).isGreaterThan(0);
	}

	@Test
	public void getOldPostsOfUser_whenUserExistThereAreNoPosts_receiveOk() {
		userService.save(TestUtil.createValidUser("user1"));
		ResponseEntity<Object> response = getOldPostsOfUser(5, "user1", new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	@Test
	public void getOldPostsOfUser_whenUserExistAndThereArePosts_receivePageWithItemsBeforeProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<Object>> response = getOldPostsOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<TestPage<Object>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(3);
	}
	
	@Test
	public void getOldPostsOfUser_whenUserExistAndThereArePosts_receivePageWithPostVMBeforeProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<TestPage<PostVM>> response = getOldPostsOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<TestPage<PostVM>>() {});
		assertThat(response.getBody().getContent().get(0).getDate()).isGreaterThan(0);
	}
	

	@Test
	public void getOldPostsOfUser_whenUserDoesNotExistThereAreNoPosts_receiveNotFound() {
		ResponseEntity<Object> response = getOldPostsOfUser(5, "user1", new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void getOldPostsOfUser_whenUserExistAndThereAreNoPosts_receivePageWithZeroItemsBeforeProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		userService.save(TestUtil.createValidUser("user2"));
		
		ResponseEntity<TestPage<PostVM>> response = getOldPostsOfUser(fourth.getId(), "user2", new ParameterizedTypeReference<TestPage<PostVM>>() {});
		assertThat(response.getBody().getTotalElements()).isEqualTo(0);
	}

	@Test
	public void getNewPosts_whenThereArePosts_receiveListOfItemsAfterProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<List<Object>> response = getNewPosts(fourth.getId(), new ParameterizedTypeReference<List<Object>>() {});
		assertThat(response.getBody().size()).isEqualTo(1);
	}

	@Test
	public void getNewPosts_whenThereArePosts_receiveListOfPostVMAfterProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<List<PostVM>> response = getNewPosts(fourth.getId(), new ParameterizedTypeReference<List<PostVM>>() {});
		assertThat(response.getBody().get(0).getDate()).isGreaterThan(0);
	}
	

	@Test
	public void getNewPostsOfUser_whenUserExistThereAreNoPosts_receiveOk() {
		userService.save(TestUtil.createValidUser("user1"));
		ResponseEntity<Object> response = getNewPostsOfUser(5, "user1", new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	@Test
	public void getNewPostsOfUser_whenUserExistAndThereArePosts_receiveListWithItemsAfterProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<List<Object>> response = getNewPostsOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<List<Object>>() {});
		assertThat(response.getBody().size()).isEqualTo(1);
	}
	
	@Test
	public void getNewPostsOfUser_whenUserExistAndThereArePosts_receiveListWithPostVMAfterProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<List<PostVM>> response = getNewPostsOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<List<PostVM>>() {});
		assertThat(response.getBody().get(0).getDate()).isGreaterThan(0);
	}
	

	@Test
	public void getNewPostsOfUser_whenUserDoesNotExistThereAreNoPosts_receiveNotFound() {
		ResponseEntity<Object> response = getNewPostsOfUser(5, "user1", new ParameterizedTypeReference<Object>() {});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void getNewPostsOfUser_whenUserExistAndThereAreNoPosts_receiveListWithZeroItemsAfterProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		userService.save(TestUtil.createValidUser("user2"));
		
		ResponseEntity<List<PostVM>> response = getNewPostsOfUser(fourth.getId(), "user2", new ParameterizedTypeReference<List<PostVM>>() {});
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	public void getNewPostCount_whenThereArePosts_receiveCountAfterProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<Map<String, Long>> response = getNewPostCount(fourth.getId(), new ParameterizedTypeReference<Map<String, Long>>() {});
		assertThat(response.getBody().get("count")).isEqualTo(1);
	}


	@Test
	public void getNewPostCountOfUser_whenThereArePosts_receiveCountAfterProvidedId() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		Post fourth = postService.save(user, TestUtil.createValidPost());
		postService.save(user, TestUtil.createValidPost());
		
		ResponseEntity<Map<String, Long>> response = getNewPostCountOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<Map<String, Long>>() {});
		assertThat(response.getBody().get("count")).isEqualTo(1);
	}
	
	@Test
	public void deletePost_whenUserIsUnAuthorized_receiveUnauthorized() {
		ResponseEntity<Object> response = deletePost(555, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}
	
	@Test
	public void deletePost_whenUserIsAuthorized_receiveOk() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = postService.save(user, TestUtil.createValidPost());

		ResponseEntity<Object> response = deletePost(post.getId(), Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		
	}
	
	@Test
	public void deletePost_whenUserIsAuthorized_receiveGenericResponse() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = postService.save(user, TestUtil.createValidPost());

		ResponseEntity<GenericResponse> response = deletePost(post.getId(), GenericResponse.class);
		assertThat(response.getBody().getMessage()).isNotNull();
		
	}

	@Test
	public void deletePost_whenUserIsAuthorized_postRemovedFromDatabase() {
		User user = userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		Post post = postService.save(user, TestUtil.createValidPost());

		deletePost(post.getId(), Object.class);
		Optional<Post> inDB = postRepository.findById(post.getId());
		assertThat(inDB.isPresent()).isFalse();
		
	}

	@Test
	public void deletePost_whenPostIsOwnedByAnotherUser_receiveForbidden() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		User postOwner = userService.save(TestUtil.createValidUser("post-owner"));
		Post post = postService.save(postOwner, TestUtil.createValidPost());

		ResponseEntity<Object> response = deletePost(post.getId(), Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		
	}

	@Test
	public void deletePost_whenPostNotExist_receiveForbidden() {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		ResponseEntity<Object> response = deletePost(5555, Object.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		
	}

	@Test
	public void deletePost_whenPostHasAttachment_attachmentRemovedFromDatabase() throws IOException {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		
		MultipartFile file = createFile();
		
		FileAttachment savedFile = fileService.saveAttachment(file);
		
		Post post = TestUtil.createValidPost();
		post.setAttachment(savedFile);
		ResponseEntity<PostVM> response = postPost(post, PostVM.class);
		
		long postId = response.getBody().getId();
		
		deletePost(postId, Object.class);
		
		Optional<FileAttachment> optionalAttachment = fileAttachmentRepository.findById(savedFile.getId());
		
		assertThat(optionalAttachment.isPresent()).isFalse();
	}

	@Test
	public void deletePost_whenPostHasAttachment_attachmentRemovedFromStorage() throws IOException {
		userService.save(TestUtil.createValidUser("user1"));
		authenticate("user1");
		
		MultipartFile file = createFile();
		
		FileAttachment savedFile = fileService.saveAttachment(file);
		
		Post post = TestUtil.createValidPost();
		post.setAttachment(savedFile);
		ResponseEntity<PostVM> response = postPost(post, PostVM.class);
		
		long postId = response.getBody().getId();
		
		deletePost(postId, Object.class);
		String attachmentFolderPath = appConfiguration.getFullAttachmentsPath() + "/" + savedFile.getName();
		File storedImage = new File(attachmentFolderPath);
		assertThat(storedImage.exists()).isFalse();
	}
	public <T> ResponseEntity<T> deletePost(long postId, Class<T> responseType){
		return testRestTemplate.exchange(API_1_0_POSTS + "/" + postId, HttpMethod.DELETE, null, responseType);
	}
	
	public <T> ResponseEntity<T> getNewPostCount(long postId, ParameterizedTypeReference<T> responseType){
		String path = API_1_0_POSTS + "/" + postId +"?direction=after&count=true";
		return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
	}

	public <T> ResponseEntity<T> getNewPostCountOfUser(long postId, String username, ParameterizedTypeReference<T> responseType){
		String path = "/api/1.0/users/" + username + "/posts/" + postId +"?direction=after&count=true";
		return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
	}

	
	public <T> ResponseEntity<T> getNewPosts(long postId, ParameterizedTypeReference<T> responseType){
		String path = API_1_0_POSTS + "/" + postId +"?direction=after&sort=id,desc";
		return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
	}

	public <T> ResponseEntity<T> getNewPostsOfUser(long postId, String username, ParameterizedTypeReference<T> responseType){
		String path = "/api/1.0/users/" + username + "/posts/" + postId +"?direction=after&sort=id,desc";
		return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
	}
	
	public <T> ResponseEntity<T> getOldPosts(long postId, ParameterizedTypeReference<T> responseType){
		String path = API_1_0_POSTS + "/" + postId +"?direction=before&page=0&size=5&sort=id,desc";
		return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
	}

	public <T> ResponseEntity<T> getOldPostsOfUser(long postId, String username, ParameterizedTypeReference<T> responseType){
		String path = "/api/1.0/users/" + username + "/posts/" + postId +"?direction=before&page=0&size=5&sort=id,desc";
		return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
	}
	
	public <T> ResponseEntity<T> getPostsOfUser(String username, ParameterizedTypeReference<T> responseType){
		String path = "/api/1.0/users/" + username + "/posts";
		return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
	}

	public <T> ResponseEntity<T> getPosts(ParameterizedTypeReference<T> responseType){
		return testRestTemplate.exchange(API_1_0_POSTS, HttpMethod.GET, null, responseType);
	}
	
	private <T> ResponseEntity<T> postPost(Post post, Class<T> responseType) {
		return testRestTemplate.postForEntity(API_1_0_POSTS, post, responseType);
	}
	

	private void authenticate(String username) {
		testRestTemplate.getRestTemplate()
			.getInterceptors().add(new BasicAuthenticationInterceptor(username, "P4ssword"));
	}
	
	@AfterEach
	public void cleanupAfter() {
		fileAttachmentRepository.deleteAll();
		postRepository.deleteAll();
	}
}
