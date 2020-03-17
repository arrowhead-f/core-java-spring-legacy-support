package eu.arrowhead.legacy.orch;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.core.CoreSystem;
import eu.arrowhead.common.core.CoreSystemService;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryResultDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.legacy.common.LegacyAppInitListener;
import eu.arrowhead.legacy.common.LegacyCommonConstants;

@Component
public class LegacyOrchestratorAppInitListener extends LegacyAppInitListener {
	
	//=================================================================================================
	// members
	
	@Value(LegacyCommonConstants.$AUTHORIZATION_KEYSTORE_TYPE)
	private String authorizationKeyStoreType;
	
	@Value(LegacyCommonConstants.$AUTHORIZATION_KEYSTORE_PATH)
	private Resource authorizationKeyStore;
	
	@Value(LegacyCommonConstants.$AUTHORIZATION_KEYSTORE_PASSWORD)
	private String authorizationKeyStorePassword;
	
	@Value(LegacyCommonConstants.$AUTHORIZATION_KEY_PASSWORD)
	private String authorizationKeyPassword;

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	protected String getSystemName() {
		return "Legacy Orchestrator Translator";
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		logger.debug("customInit started");
		
		@SuppressWarnings("unchecked")
		final Map<String,Object> context = event.getApplicationContext().getBean(CommonConstants.ARROWHEAD_CONTEXT, Map.class);
		if (sslProperties.isSslEnabled()) {
			obtainAuthorizationPrivateKey(context); // Necessary for legacy token generation 
			obtainOwnCloudInfo(context); // Necessary for legacy token generation
		}		
		final String scheme = sslProperties.isSslEnabled() ? CommonConstants.HTTPS : CommonConstants.HTTP;
		final UriComponents srQueryUri = createSRQueryUri(scheme);
		final SystemResponseDTO orchestrator = getOrchestrationCoreSystemDTO(srQueryUri, scheme);
		if (orchestrator.getPort() == systemRegistrationProperties.getSystemDomainPort()) {
			throw new ServiceConfigurationError("Real Orcestrator Core System not found");
		}
		context.put(LegacyCommonConstants.ORCHESTRATOR_ORCHESTRATION_URI, Utilities.createURI(scheme, orchestrator.getAddress(), orchestrator.getPort(), 
																							  CoreSystemService.ORCHESTRATION_SERVICE.getServiceUri()));
		unregisterOrchestratorFromSR(orchestrator, scheme);
		registerLegacyOrchestratorTranslator(scheme);
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents createSRQueryUri(final String scheme) {
		logger.debug("createSRQueryUri started...");
				
		final String srQueryUriString = CommonConstants.SERVICE_REGISTRY_URI + LegacyCommonConstants.OP_SERVICE_REGISTRY_QUERY_URI;		
		return Utilities.createURI(scheme, systemRegistrationProperties.getServiceRegistryAddress(), systemRegistrationProperties.getServiceRegistryPort(),	srQueryUriString);
	}
	
	//-------------------------------------------------------------------------------------------------
	private SystemResponseDTO getOrchestrationCoreSystemDTO(final UriComponents srQueryUri, final String scheme) {
		logger.debug("getOrchestrationCoreSystemDTO started...");
		
		final ServiceQueryFormDTO serviceQueryFormDTO = new ServiceQueryFormDTO.Builder(CoreSystemService.ORCHESTRATION_SERVICE.getServiceDefinition()).build();
		final ResponseEntity<ServiceQueryResultDTO> serviceQueryResult = httpService.sendRequest(srQueryUri, HttpMethod.POST, ServiceQueryResultDTO.class, serviceQueryFormDTO);
		final List<ServiceRegistryResponseDTO> serviceQueryData = serviceQueryResult.getBody().getServiceQueryData();
		if (serviceQueryData == null || serviceQueryData.isEmpty()) {
			throw new ServiceConfigurationError("Orchestrator Core System not found");
		}
		return serviceQueryData.iterator().next().getProvider();
	}
	
	//-------------------------------------------------------------------------------------------------
	private void unregisterOrchestratorFromSR(final SystemResponseDTO orchestrator, final String scheme) {
		logger.debug("unregisterOrchestratorFromSR started...");
		
		final String srUnregisterUriSrting = CommonConstants.SERVICE_REGISTRY_URI + LegacyCommonConstants.OP_SERVICE_REGISTRY_UNREGISTER_URI;
		final String serviceDefinition = CoreSystemService.ORCHESTRATION_SERVICE.getServiceDefinition();
		final String providerName = orchestrator.getSystemName();
		final String providerAddress = orchestrator.getAddress();
		final int providerPort= orchestrator.getPort();
		
		String[] queryParam = {CommonConstants.OP_SERVICE_REGISTRY_UNREGISTER_REQUEST_PARAM_SERVICE_DEFINITION, serviceDefinition,
							   CommonConstants.OP_SERVICE_REGISTRY_UNREGISTER_REQUEST_PARAM_PROVIDER_SYSTEM_NAME, providerName,
							   CommonConstants.OP_SERVICE_REGISTRY_UNREGISTER_REQUEST_PARAM_PROVIDER_ADDRESS, providerAddress,
							   CommonConstants.OP_SERVICE_REGISTRY_UNREGISTER_REQUEST_PARAM_PROVIDER_PORT, String.valueOf(providerPort)};
		
		final UriComponents srUnregisterUri = Utilities.createURI(scheme, systemRegistrationProperties.getServiceRegistryAddress(), systemRegistrationProperties.getServiceRegistryPort(),
																  srUnregisterUriSrting, queryParam);
		httpService.sendRequest(srUnregisterUri, HttpMethod.DELETE, Void.class);
	}
	
	//-------------------------------------------------------------------------------------------------
	private void registerLegacyOrchestratorTranslator(final String scheme) {
		logger.debug("registerLegacyOrchestratorTranslator started...");
		
		final String srRegisterUriSrting = CommonConstants.SERVICE_REGISTRY_URI + LegacyCommonConstants.OP_SERVICE_REGISTRY_REGISTER_URI;
		final UriComponents srRegisterUri = Utilities.createURI(scheme, systemRegistrationProperties.getServiceRegistryAddress(), systemRegistrationProperties.getServiceRegistryPort(),
															    srRegisterUriSrting);
		
		final SystemRequestDTO systemRequestDTO = new SystemRequestDTO();
		systemRequestDTO.setSystemName(CoreSystem.ORCHESTRATOR.name().toLowerCase());
		systemRequestDTO.setAddress(systemRegistrationProperties.getSystemDomainName());
		systemRequestDTO.setPort(systemRegistrationProperties.getSystemDomainPort());
		if (sslProperties.isSslEnabled()) {
			systemRequestDTO.setAuthenticationInfo(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
		}
		
		final ServiceRegistryRequestDTO serviceRegistryRequestDTO = new ServiceRegistryRequestDTO();
		serviceRegistryRequestDTO.setProviderSystem(systemRequestDTO);
		serviceRegistryRequestDTO.setServiceDefinition(CoreSystemService.ORCHESTRATION_SERVICE.getServiceDefinition());
		serviceRegistryRequestDTO.setServiceUri(CoreSystemService.ORCHESTRATION_SERVICE.getServiceUri());
		serviceRegistryRequestDTO.setInterfaces(sslProperties.isSslEnabled() ? List.of(CommonConstants.HTTP_SECURE_JSON) : List.of(CommonConstants.HTTP_INSECURE_JSON));
		serviceRegistryRequestDTO.setSecure(sslProperties.isSslEnabled() ? ServiceSecurityType.CERTIFICATE.name() : ServiceSecurityType.NOT_SECURE.name());
		
		httpService.sendRequest(srRegisterUri, HttpMethod.POST, ServiceRegistryResponseDTO.class, serviceRegistryRequestDTO);
	}
	
	//-------------------------------------------------------------------------------------------------
	private void obtainAuthorizationPrivateKey(final Map<String,Object> context) {
		logger.debug("obtainAuthorzationPrivateKey started...");
		
		Assert.isTrue(sslProperties.isSslEnabled(), "SSL is not enabled.");
		final String messageNotDefined = " is not defined.";
		Assert.isTrue(!Utilities.isEmpty(authorizationKeyStoreType), LegacyCommonConstants.AUTHORIZATION_KEYSTORE_TYPE + messageNotDefined);
		Assert.notNull(authorizationKeyStore, LegacyCommonConstants.AUTHORIZATION_KEYSTORE_PATH + messageNotDefined);
		Assert.isTrue(authorizationKeyStore.exists(), LegacyCommonConstants.AUTHORIZATION_KEYSTORE_PATH + " file is not found.");
		Assert.notNull(authorizationKeyStorePassword, LegacyCommonConstants.AUTHORIZATION_KEYSTORE_PASSWORD + messageNotDefined);
		
		try {
			final KeyStore keyStore = KeyStore.getInstance(authorizationKeyStoreType);
			keyStore.load(authorizationKeyStore.getInputStream(), authorizationKeyStorePassword.toCharArray());
			
			final X509Certificate serverCertificate = Utilities.getFirstCertFromKeyStore(keyStore);
			final String serverCN = Utilities.getCertCNFromSubject(serverCertificate.getSubjectDN().getName());
			if (!Utilities.isKeyStoreCNArrowheadValid(serverCN)) {
				logger.info("Server CN ({}) is not compliant with the Arrowhead certificate structure, since it does not have 5 parts, or does not end with \"arrowhead.eu\".", serverCN);
				throw new AuthException("Server CN (" + serverCN + ") is not compliant with the Arrowhead certificate structure, since it does not have 5 parts, or does not end with \"arrowhead.eu\".");
			}
			
			final PrivateKey privateKey = Utilities.getPrivateKey(keyStore, sslProperties.getKeyPassword());
			context.put(LegacyCommonConstants.AUTHORIZATION_PRIVATE_KEY, privateKey);
			
		} catch (final NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException ex) {
			throw new AuthException("Obtaining Authorization privet key failed: " + ex.getMessage());
		}
		
	}
	
	//-------------------------------------------------------------------------------------------------
	private void obtainOwnCloudInfo(final Map<String,Object> context) {
		Assert.isTrue(sslProperties.isSslEnabled(), "SSL is not enabled.");
		
		final String serverCN = (String) context.get(CommonConstants.SERVER_COMMON_NAME);
		final String[] serverFields = serverCN.split("\\.");
		String name = serverFields[1];
		String operator = serverFields[2];
		
		context.put(LegacyCommonConstants.OWN_CLOUD_NAME, name);
		context.put(LegacyCommonConstants.OWN_CLOUD_OPERATOR, operator);
	}
}