package eu.arrowhead.proxy;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryResultDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.legacy.common.LegacyAppInitListener;
import eu.arrowhead.legacy.common.LegacyCommonConstants;

@Component
public class DatamanagerProxyAppInitListener extends LegacyAppInitListener {
	
	//=================================================================================================
	// members
	
	private static final String DATAMANAGER_HISTORIAN = "historian";
	private static final String DATAMANAGER_HISTORIAN_URI = "/datamanager/historian";
	private static final String DATAMANAGER_HISTORIAN_SERVICE_URI = "/datamanager/historian/{system}";
	private static final String DATAMANAGER_HISTORIAN_DATA_URI = "/datamanager/historian/{system}/{service}";

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	protected String getSystemName() {
		return "Datamanager Proxy";
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		logger.debug("customInit started");
		
		@SuppressWarnings("unchecked")
		final Map<String,Object> context = event.getApplicationContext().getBean(CommonConstants.ARROWHEAD_CONTEXT, Map.class);

		final String scheme = sslProperties.isSslEnabled() ? CommonConstants.HTTPS : CommonConstants.HTTP;
		final UriComponents srQueryUri = createSRQueryUri(scheme);
		final SystemResponseDTO datamanager = getDatamanagerSystemDTO(srQueryUri, scheme);
		
		context.put(DataManagerProxyController.CORE_SERVICE_DATAMANAGER_HISTORIAN_KEY, Utilities.createURI(scheme, datamanager.getAddress(), datamanager.getPort(), DATAMANAGER_HISTORIAN_URI));
		context.put(DataManagerProxyController.CORE_SERVICE_DATAMANAGER_HISTORIAN_SERVICE_KEY, Utilities.createURI(scheme, datamanager.getAddress(), datamanager.getPort(), DATAMANAGER_HISTORIAN_SERVICE_URI));
		context.put(DataManagerProxyController.CORE_SERVICE_DATAMANAGER_HISTORIAN_DATA_KEY, Utilities.createURI(scheme, datamanager.getAddress(), datamanager.getPort(), DATAMANAGER_HISTORIAN_DATA_URI));
		try {
			checkDatamanagerConnection(datamanager.getAddress(), datamanager.getPort());
		} catch (final InterruptedException ex) {
			throw new ServiceConfigurationError("Data Manager checking is failed.");
		}

		registerDatamanagerProxy(scheme);
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents createSRQueryUri(final String scheme) {
		logger.debug("createSRQueryUri started...");
				
		final String srQueryUriString = CommonConstants.SERVICE_REGISTRY_URI + LegacyCommonConstants.OP_SERVICE_REGISTRY_QUERY_URI;		
		return Utilities.createURI(scheme, systemRegistrationProperties.getServiceRegistryAddress(), systemRegistrationProperties.getServiceRegistryPort(),	srQueryUriString);
	}
	
	//-------------------------------------------------------------------------------------------------
	private SystemResponseDTO getDatamanagerSystemDTO(final UriComponents srQueryUri, final String scheme) {
		logger.debug("getDatamanagerSystemDTO started...");
		
		final ServiceQueryFormDTO serviceQueryFormDTO = new ServiceQueryFormDTO.Builder(DATAMANAGER_HISTORIAN).build();
		final ResponseEntity<ServiceQueryResultDTO> serviceQueryResult = httpService.sendRequest(srQueryUri, HttpMethod.POST, ServiceQueryResultDTO.class, serviceQueryFormDTO);
		final List<ServiceRegistryResponseDTO> serviceQueryData = serviceQueryResult.getBody().getServiceQueryData();
		if (serviceQueryData == null || serviceQueryData.isEmpty()) {
			throw new ServiceConfigurationError("Datamanager not found");
		}
		return serviceQueryData.iterator().next().getProvider();
	}
	
	//-------------------------------------------------------------------------------------------------
	private void checkDatamanagerConnection(final String address, final int port) throws InterruptedException {
		logger.debug("checkDatamanagerConnection started...");
	
		final String scheme = sslProperties.isSslEnabled() ? CommonConstants.HTTPS : CommonConstants.HTTP;
		final int retries = 3;
		final int period = 10;
		
		final UriComponents echoUri = createEchoUri(scheme, address, port);
		for (int i = 0; i <= retries; ++i) {
			try {
				httpService.sendRequest(echoUri, HttpMethod.GET, String.class);
				logger.info("Data Manager is accessible...");
				break;
			} catch (final AuthException ex) {
				throw ex;
			} catch (final ArrowheadException ex) {
				if (i == 3) {
					throw ex;
				} else {
					logger.info("Data Manager is unavailable at the moment, retrying in {} seconds...", period);
					Thread.sleep(period * LegacyCommonConstants.CONVERSION_MILLISECOND_TO_SECOND);
				}
			}
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents createEchoUri(final String scheme, final String address, final int port) {
		logger.debug("createEchoUri started...");
				
		final String echoUriStr = "/datamanager" + CommonConstants.ECHO_URI;
		return Utilities.createURI(scheme, address, port, echoUriStr);
	}
	
	//-------------------------------------------------------------------------------------------------
	private void registerDatamanagerProxy(final String scheme) {
		logger.debug("registerDatamanagerProxy started...");
		
		final String srRegisterUriSrting = CommonConstants.SERVICE_REGISTRY_URI + LegacyCommonConstants.OP_SERVICE_REGISTRY_REGISTER_URI;
		final UriComponents srRegisterUri = Utilities.createURI(scheme, systemRegistrationProperties.getServiceRegistryAddress(), systemRegistrationProperties.getServiceRegistryPort(),
															    srRegisterUriSrting);
		
		final SystemRequestDTO systemRequestDTO = new SystemRequestDTO();
		systemRequestDTO.setSystemName("datamanager_proxy");
		systemRequestDTO.setAddress(systemRegistrationProperties.getSystemDomainName());
		systemRequestDTO.setPort(systemRegistrationProperties.getSystemDomainPort());
		if (sslProperties.isSslEnabled()) {
			systemRequestDTO.setAuthenticationInfo(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
		}
		
		final ServiceRegistryRequestDTO serviceRegistryRequestDTO = new ServiceRegistryRequestDTO();
		serviceRegistryRequestDTO.setProviderSystem(systemRequestDTO);
		serviceRegistryRequestDTO.setServiceDefinition("fetch-from-historian-proxy");
		serviceRegistryRequestDTO.setServiceUri("/datamanager_proxy/historian");
		serviceRegistryRequestDTO.setInterfaces(sslProperties.isSslEnabled() ? List.of(CommonConstants.HTTP_SECURE_JSON) : List.of(CommonConstants.HTTP_INSECURE_JSON));
		serviceRegistryRequestDTO.setSecure(sslProperties.isSslEnabled() ? ServiceSecurityType.CERTIFICATE.name() : ServiceSecurityType.NOT_SECURE.name());
		
		try {
			httpService.sendRequest(srRegisterUri, HttpMethod.POST, ServiceRegistryResponseDTO.class, serviceRegistryRequestDTO);
		} catch (final InvalidParameterException ex) {
			if (ex.getMessage().contains("already exists")) {
				return;
			}
			
			throw ex;
		}
	}
}