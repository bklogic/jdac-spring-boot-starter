package net.backlogic.persistence.springboot;

public class DataAccessProperties {
	private String baseUrl;
	private String basePackage;
	private boolean logRequest;
	private DevTimeProperties devtime = new DevTimeProperties();
	
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
	public DevTimeProperties getDevtime() {
		return devtime;
	}
	public void setDevtime(DevTimeProperties devtime) {
		this.devtime = devtime;
	}		
}
