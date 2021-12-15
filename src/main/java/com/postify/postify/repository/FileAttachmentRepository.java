package com.postify.postify.repository;

import java.util.Date;
import java.util.List;

import com.postify.postify.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long>{
	
	List<FileAttachment> findByDateBeforeAndPostIsNull(Date date);

}
