package net.backlogic.persistence.jdacspringboot;

import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JDAC Spring Boot Starter Configuration Properties.
 */
public class DataAccessProperties {
	/**
	 * baseUrl of backend data access application.
	 */
	private String baseUrl;
	/**
	 * base package to start data access interface scanning.
	 */
	private String basePackage;
	/**
	 * Whether to enable request logging.
	 */
	private boolean logRequest;
	/**
	 * Specify a JwtProvider for JDAC client to use
	 */
	private Properties jwtProvider = new Properties();
	
	public String getBaseUrl() {
		return baseUrl;
	}
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	public String getBasePackage() {
		return basePackage;
	}
	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}
	public boolean isLogRequest() {
		return logRequest;
	}
	public void setLogRequest(boolean logRequest) {
		this.logRequest = logRequest;
	}

	public Properties getJwtProvider() {
		return jwtProvider;
	}

	public void setJwtProvider(Properties jwtProvider) {
		this.jwtProvider = jwtProvider;
	}
}
