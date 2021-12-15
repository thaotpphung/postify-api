package com.postify.postify.model;

import com.postify.postify.entity.Post;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PostVM {
	
	private long id;
	
	private String content;
	
	private long date;
	
	private UserVM user;
	
	private FileAttachmentVM attachment;
	
	public PostVM(Post post) {
		this.setId(post.getId());
		this.setContent(post.getContent());
		this.setDate(post.getTimestamp().getTime());
		this.setUser(new UserVM(post.getUser()));
		if(post.getAttachment() != null) {
			this.setAttachment(new FileAttachmentVM(post.getAttachment()));
		}
	}

}
