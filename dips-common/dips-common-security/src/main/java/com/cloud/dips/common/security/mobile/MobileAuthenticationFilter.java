/*
 *    Copyright (c) 2018-2025, BigPan All rights reserved.
 *
 */

package com.cloud.dips.common.security.mobile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.cloud.dips.common.core.constant.SecurityConstants;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author BigPan
 * @date 2018/1/9
 * 手机号登录验证filter
 * 拦截openid 的过滤器   第一步
 */
public class MobileAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
	private static final String SPRING_SECURITY_FORM_MOBILE_KEY = "mobile";
	private AuthenticationEntryPoint authenticationEntryPoint = new MobileAuthenticationEntryPoint();
	@Getter
	@Setter
	private String mobileParameter = SPRING_SECURITY_FORM_MOBILE_KEY;
	@Getter
	@Setter
	private boolean postOnly = true;
	@Getter
	@Setter
	private AuthenticationEventPublisher eventPublisher;

	public MobileAuthenticationFilter() {
		super(new AntPathRequestMatcher(SecurityConstants.MOBILE_TOKEN_URL, "POST"));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
												HttpServletResponse response) throws AuthenticationException {
		if (postOnly && !request.getMethod().equals(HttpMethod.POST.name())) {
			throw new AuthenticationServiceException(
				"Authentication method not supported: " + request.getMethod());
		}

		String mobile = obtainMobile(request);

		if (mobile == null) {
			mobile = "";
		}

		mobile = mobile.trim();

		MobileAuthenticationToken mobileAuthenticationToken = new MobileAuthenticationToken(mobile);

		setDetails(request, mobileAuthenticationToken);

		Authentication authResult = null;
		try {
			authResult = this.getAuthenticationManager().authenticate(mobileAuthenticationToken);

			logger.debug("Authentication success: " + authResult);
			eventPublisher.publishAuthenticationSuccess(authResult);
			SecurityContextHolder.getContext().setAuthentication(authResult);

		} catch (Exception failed) {
			SecurityContextHolder.clearContext();
			logger.debug("Authentication request failed: " + failed);

			eventPublisher.publishAuthenticationFailure(new BadCredentialsException(failed.getMessage(), failed),
				new PreAuthenticatedAuthenticationToken("access-token", "N/A"));

			try {
				authenticationEntryPoint.commence(request, response,
					new UsernameNotFoundException(failed.getMessage(), failed));
			} catch (Exception e) {
				logger.error("authenticationEntryPoint handle error:{}", failed);
			}
		}
		    //返回token去校验
     		return authResult;
	}

	private String obtainMobile(HttpServletRequest request) {
		Map<String, String[]> parameterMap = request.getParameterMap();
		System.out.println(parameterMap.get(mobileParameter));

		return request.getParameter(mobileParameter);
	}

	private void setDetails(HttpServletRequest request,
							MobileAuthenticationToken authRequest) {
		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
	}
}

