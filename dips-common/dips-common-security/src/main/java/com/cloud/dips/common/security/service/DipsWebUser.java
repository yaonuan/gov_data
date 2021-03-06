///*
// *
// *      Copyright (c) 2018-2025, BigPan All rights reserved.
// *
// */
//
//package com.cloud.dips.common.security.service;
//
//import com.cloud.dips.user.api.entity.WebUsers;
//import lombok.Getter;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.userdetails.User;
//
//import java.util.Collection;
//
///**
// * @author BigPan
// * @date 2018/8/20
// * 扩展用户信息
// */
//public class DipsWebUser extends User {
//	/**
//	 *
//	 */
//	private static final long serialVersionUID = 1L;
//	/**
//	 * 用户ID
//	 */
//	@Getter
//	private Integer id;
//
//
//	/**
//	 * Construct the <code>User</code> with the details required by
//	 * {@link DaoAuthenticationProvider}.
//	 *
//	 * @param id                    用户ID
//	 * @param username              the username presented to the
//	 *                              <code>DaoAuthenticationProvider</code>
//	 * @param password              the password that should be presented to the
//	 *                              <code>DaoAuthenticationProvider</code>
//	 * @param enabled               set to <code>true</code> if the user is enabled
//	 * @param accountNonExpired     set to <code>true</code> if the account has not expired
//	 * @param credentialsNonExpired set to <code>true</code> if the credentials have not
//	 *                              expired
//	 * @param accountNonLocked      set to <code>true</code> if the account is not locked
//	 * @param authorities           the authorities that should be granted to the caller if they
//	 *                              presented the correct username and password and the user is enabled. Not null.
//	 * @throws IllegalArgumentException if a <code>null</code> value was passed either as
//	 *                                  a parameter or as an element in the <code>GrantedAuthority</code> collection
//	 */
//	public DipsWebUser(Integer id, String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
//		super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
//		this.id = id;
//	}
//}
