package com.postify.postify.controller;

import com.postify.postify.entity.FileAttachment;
import com.postify.postify.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/1.0")
public class FileUploadController {
	
	@Autowired
	FileService fileService;
	
	@PostMapping("/posts/upload")
	FileAttachment uploadForPost(MultipartFile file) {
		return fileService.saveAttachment(file);
	}

}
