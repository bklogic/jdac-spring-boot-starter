package net.backlogic.persistence.jdacspringboot;

import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="jdac")
public class DataAccessProperties {
	private String baseUrl;
	private String basePackage;
	private boolean logRequest;
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