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

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Long getId() { return id; }
	public LegacyArrowheadService getProvidedService() { return providedService; }
	public LegacyArrowheadSystem getProvider() { return provider; }
	public String getServiceURI() { return serviceURI; }
	public String getServiceUri() { return serviceURI; }
	public Boolean getUdp() { return udp; }
	public LocalDateTime getEndOfValidity() { return endOfValidity; }
	public Integer getVersion() { return version; }
	
	//-------------------------------------------------------------------------------------------------
	public void setId(final Long id) { this.id = id; }
	public void setProvidedService(final LegacyArrowheadService providedService) { this.providedService = providedService; }
	public void setProvider(final LegacyArrowheadSystem provider) { this.provider = provider; }
	public void setServiceURI(final String serviceUri) { this.serviceURI = serviceUri; }
	public void setServiceUri(final String serviceUri) { this.serviceURI = serviceUri; }
	public void setUdp(final Boolean udp) { this.udp = udp; }
	public void setEndOfValidity(final LocalDateTime endOfValidity) { this.endOfValidity = endOfValidity; }
	public void setVersion(final Integer version) { this.version = version; }
}