package eu.arrowhead.legacy.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class LegacyServiceRegistryEntry implements Serializable {
	
	//=================================================================================================
	// members

	private static final long serialVersionUID = -8918990859807536374L;
	
	private Long id;
	private LegacyArrowheadService providedService;
	private LegacyArrowheadSystem provider;
	private String serviceURI;
	private Boolean udp;
	private LocalDateTime endOfValidity;
	private Integer version = 1;
	private Integer ttl = 0;
	private String metadata;

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Long getId() { return id; }
	public LegacyArrowheadService getProvidedService() { return providedService; }
	public LegacyArrowheadSystem getProvider() { return provider; }
	public String getServiceURI() { return serviceURI; }
	public String getServiceUri() { return serviceURI; } //keep lower case version of version to avoid problems with skeletons
	public Boolean getUdp() { return udp; }
	public LocalDateTime getEndOfValidity() { return endOfValidity; }
	public Integer getVersion() { return version; }
	public Integer getTtl() { return ttl; }
	public String getMetadata() { return metadata; }
	
	//-------------------------------------------------------------------------------------------------
	public void setId(final Long id) { this.id = id; }
	public void setProvidedService(final LegacyArrowheadService providedService) { this.providedService = providedService; }
	public void setProvider(final LegacyArrowheadSystem provider) { this.provider = provider; }
	public void setServiceURI(final String serviceURI) { this.serviceURI = serviceURI; }
	public void setServiceUri(final String serviceURI) { this.serviceURI = serviceURI; } //keep lower case version of version to avoid problems with skeletons
	public void setUdp(final Boolean udp) { this.udp = udp; }
	public void setEndOfValidity(final LocalDateTime endOfValidity) { this.endOfValidity = endOfValidity; }
	public void setVersion(final Integer version) { this.version = version; }
	public void setTtl(final Integer ttl) { this.ttl = ttl; }
	public void setMetadata(final String metadata) { this.metadata = metadata; }
}