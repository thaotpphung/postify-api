package com.postify.postify.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import com.postify.postify.TestUtil;
import com.postify.postify.entity.FileAttachment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.postify.postify.entity.Post;

@DataJpaTest
@ActiveProfiles("test")
public class FileAttachmentRepositoryTest {

	@Autowired
	TestEntityManager testEntityManager;
	
	@Autowired
	FileAttachmentRepository fileAttachmentRepository;
	
	@Test
	public void findByDateBeforeAndPostIsNull_whenAttachmentsDateOlderThanOneHour_returnsAll() {
		testEntityManager.persist(getOneHourOldFileAttachment());
		testEntityManager.persist(getOneHourOldFileAttachment());
		testEntityManager.persist(getOneHourOldFileAttachment());
		Date oneHourAgo = new Date(System.currentTimeMillis() - (60*60*1000));
		List<FileAttachment> attachments = fileAttachmentRepository.findByDateBeforeAndPostIsNull(oneHourAgo);
		assertThat(attachments.size()).isEqualTo(3);
	}
	
	@Test
	public void findByDateBeforeAndPostIsNull_whenAttachmentsDateOlderThanOneHorButHavePost_returnsNone() {
		Post post1 = testEntityManager.persist(TestUtil.createValidPost());
		Post post2 = testEntityManager.persist(TestUtil.createValidPost());
		Post post3 = testEntityManager.persist(TestUtil.createValidPost());
		
		testEntityManager.persist(getOldFileAttachmentWithPost(post1));
		testEntityManager.persist(getOldFileAttachmentWithPost(post2));
		testEntityManager.persist(getOldFileAttachmentWithPost(post3));
		Date oneHourAgo = new Date(System.currentTimeMillis() - (60*60*1000));
		List<FileAttachment> attachments = fileAttachmentRepository.findByDateBeforeAndPostIsNull(oneHourAgo);
		assertThat(attachments.size()).isEqualTo(0);
	}

	@Test
	public void findByDateBeforeAndPostIsNull_whenAttachmentsDateWithinOneHour_returnsNone() {
		testEntityManager.persist(getFileAttachmentWithinOneHour());
		testEntityManager.persist(getFileAttachmentWithinOneHour());
		testEntityManager.persist(getFileAttachmentWithinOneHour());
		Date oneHourAgo = new Date(System.currentTimeMillis() - (60*60*1000));
		List<FileAttachment> attachments = fileAttachmentRepository.findByDateBeforeAndPostIsNull(oneHourAgo);
		assertThat(attachments.size()).isEqualTo(0);
	}

	@Test
	public void findByDateBeforeAndPostIsNull_whenSomeAttachmentsOldSomeNewAndSomeWithPost_returnsAttachmentsWithOlderAndNoPostAssigned() {
		Post post1 = testEntityManager.persist(TestUtil.createValidPost());
		testEntityManager.persist(getOldFileAttachmentWithPost(post1));
		testEntityManager.persist(getOneHourOldFileAttachment());
		testEntityManager.persist(getFileAttachmentWithinOneHour());
		Date oneHourAgo = new Date(System.currentTimeMillis() - (60*60*1000));
		List<FileAttachment> attachments = fileAttachmentRepository.findByDateBeforeAndPostIsNull(oneHourAgo);
		assertThat(attachments.size()).isEqualTo(1);
	}
	private FileAttachment getOneHourOldFileAttachment() {
		Date date = new Date(System.currentTimeMillis() - (60*60*1000) - 1);
		FileAttachment fileAttachment = new FileAttachment();
		fileAttachment.setDate(date);
		return fileAttachment;
	}
	private FileAttachment getFileAttachmentWithinOneHour() {
		Date date = new Date(System.currentTimeMillis() - (60*1000));
		FileAttachment fileAttachment = new FileAttachment();
		fileAttachment.setDate(date);
		return fileAttachment;
	}
	
	private FileAttachment getOldFileAttachmentWithPost(Post post) {
		FileAttachment fileAttachment = getOneHourOldFileAttachment();
		fileAttachment.setPost(post);
		return fileAttachment;
	}
}
