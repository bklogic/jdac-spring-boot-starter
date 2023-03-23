package net.backlogic.persistence.springboot;

public class DevTimeProperties {
	private boolean enabled = false;
	private String jwt;
    private String authEndpoint;
    private String serviceKey;
    private String serviceSecret;
    
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getJwt() {
		return jwt;
	}
	public void setJwt(String jwt) {
		this.jwt = jwt;
	}
	public String getAuthEndpoint() {
		return authEndpoint;
	}
	public void setAuthEndpoint(String authEndpoint) {
		this.authEndpoint = authEndpoint;
	}
	public String getServiceKey() {
		return serviceKey;
	}
	public void setServiceKey(String serviceKey) {
		this.serviceKey = serviceKey;
	}
	public String getServiceSecret() {
		return serviceSecret;
	}
	public void setServiceSecret(String serviceSecret) {
		this.serviceSecret = serviceSecret;
	}
}
