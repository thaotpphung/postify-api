package com.postify.postify.entity;

import java.beans.Transient;
import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.postify.postify.validation.constraint.UniqueUsername;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Data
@Entity
public class User implements UserDetails{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4074374728582967483L;
	
	@Id
	@GeneratedValue
	private long id;
	
	@NotNull(message = "{postify.constraints.username.NotNull.message}")
	@Size(min = 4, max=255)
	@UniqueUsername
	private String username;
	
	@NotNull
	@Size(min = 4, max=255)
	private String displayName;
	
	@NotNull
	@Size(min = 8, max=255)
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message="{postify.constraints.password.Pattern.message}")
	private String password;
	
	private String image;
	
	@OneToMany(mappedBy = "user")
	private List<Post> posts;

	@Override
	@Transient
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return AuthorityUtils.createAuthorityList("Role_USER");
	}

	@Override
	@Transient
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	@Transient
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	@Transient
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	@Transient
	public boolean isEnabled() {
		return true;
	}

}
