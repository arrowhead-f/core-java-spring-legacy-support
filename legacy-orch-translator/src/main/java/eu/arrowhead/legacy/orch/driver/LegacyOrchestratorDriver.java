package eu.arrowhead.legacy.orch.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.OrchestrationFlags.Flag;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResultDTO;
import eu.arrowhead.common.dto.shared.ServiceInterfaceResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.legacy.common.LegacyCommonConstants;
import eu.arrowhead.legacy.common.model.LegacyModelConverter;
import eu.arrowhead.legacy.common.model.LegacyOrchestrationResponse;
import eu.arrowhead.legacy.common.model.LegacyServiceRequestForm;

@Service
public class LegacyOrchestratorDriver {
	
	//=================================================================================================
	// members
	
	@Autowired
	private HttpService httpService;
	
	@Autowired
	private LegacyTokenGenerator legacyTokenGenerator;
	
	@Resource(name = CommonConstants.ARROWHEAD_CONTEXT)
	private Map<String,Object> arrowheadContext;
	
	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;
	
	private final Logger logger = LogManager.getLogger(LegacyOrchestratorDriver.class);
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String echo() {
		final String scheme = sslEnabled ? CommonConstants.HTTPS : CommonConstants.HTTP;
		final UriComponents uri = (UriComponents) arrowheadContext.get(LegacyCommonConstants.ORCHESTRATOR_ORCHESTRATION_URI);
		final String address = uri.getHost();
		final int port = uri.getPort();
		final UriComponents uriEcho = Utilities.createURI(scheme, address, port, CommonConstants.ORCHESTRATOR_URI + CommonConstants.ECHO_URI);
		final ResponseEntity<String> response = httpService.sendRequest(uriEcho, HttpMethod.GET, String.class);
		
		return response.getBody();
	}
	
	//-------------------------------------------------------------------------------------------------
	public ResponseEntity<OrchestrationResponseDTO> proceedOrchestration413(final OrchestrationFormRequestDTO request) {
		final String origin = CommonConstants.ORCHESTRATOR_URI + CommonConstants.OP_ORCH_PROCESS;
//		if (request.getOrchestrationFlags().getOrDefault(Flag.TRIGGER_INTER_CLOUD, false)) {
//			throw new BadPayloadException("Translator does not support orchestration with flag 'TRIGGER_INTER_CLOUD=true'", HttpStatus.SC_BAD_REQUEST, origin);
//		}	
//		if (request.getOrchestrationFlags().getOrDefault(Flag.ENABLE_INTER_CLOUD, false)) {
//			request.getOrchestrationFlags().put(Flag.ENABLE_INTER_CLOUD, false);
//			logger.debug("Orchestration flag 'ENABLE_INTER_CLOUD=true' is not supported and was changed to false");
//		}
		
		final List<String> requestedInterfaces = request.getRequestedService().getInterfaceRequirements();
		request.getRequestedService().setInterfaceRequirements(new ArrayList<>());
		
		final boolean originalMatchmakingFlag = request.getOrchestrationFlags().getOrDefault(Flag.MATCHMAKING, false);
		request.getOrchestrationFlags().put(Flag.MATCHMAKING, false);
		
		final UriComponents uri = (UriComponents) arrowheadContext.get(LegacyCommonConstants.ORCHESTRATOR_ORCHESTRATION_URI);		
		final ResponseEntity<OrchestrationResponseDTO> response = httpService.sendRequest(uri, HttpMethod.POST, OrchestrationResponseDTO.class, request);
		final OrchestrationResponseDTO dto = filterOnRequestedInterfaces(requestedInterfaces, response.getBody());		

		if (originalMatchmakingFlag && dto.getResponse().size() > 1) {
			dto.setResponse(List.of(dto.getResponse().iterator().next()));
		}
		
		//Generate token if required
		final List<OrchestrationResultDTO> orchResultsWithLegacyTokenWorkaround = new ArrayList<>();		
		for (OrchestrationResultDTO result : dto.getResponse()) {
			if (result.getMetadata() != null
					&& result.getMetadata().containsKey(LegacyCommonConstants.KEY_ARROWHEAD_VERSION)
					&& result.getMetadata().get(LegacyCommonConstants.KEY_ARROWHEAD_VERSION).equalsIgnoreCase(LegacyCommonConstants.ARROWHEAD_VERSION_VALUE_412)) {
				
				//Arrowhead v4.1.2 compliant provider
				if (result.getSecure() == ServiceSecurityType.TOKEN) {					
					result = generateLegacyTokenForConsumer413(request.getRequesterSystem().getSystemName(), result);
				}
				if (result != null) { //Can be null when token generation failed -> skip provider
					orchResultsWithLegacyTokenWorkaround.add(result);
				}				
			} else {
				//Arrowhead v4.1.3 compliant provider
				if (result.getAuthorizationTokens() != null && !result.getAuthorizationTokens().isEmpty()) {
					result.getAuthorizationTokens().put(result.getInterfaces().iterator().next().getInterfaceName(), result.getAuthorizationTokens().get(LegacyCommonConstants.DEFAULT_INTERFACE));					
				}
				orchResultsWithLegacyTokenWorkaround.add(result);
			}
		}
		dto.setResponse(orchResultsWithLegacyTokenWorkaround);
		
		return new ResponseEntity<>(dto, org.springframework.http.HttpStatus.OK);
	}
	
	//-------------------------------------------------------------------------------------------------
	public ResponseEntity<LegacyOrchestrationResponse> proceedOrchestration412(final LegacyServiceRequestForm request) {
		final String origin = CommonConstants.ORCHESTRATOR_URI + CommonConstants.OP_ORCH_PROCESS;
		if (request.getRequesterSystem() == null) {
			throw new BadPayloadException("requesterSystem cannot be null", HttpStatus.SC_BAD_REQUEST, origin);
		}
		if (request.getOrchestrationFlags().getOrDefault(CommonConstants.ORCHESTRATON_FLAG_TRIGGER_INTER_CLOUD, false)) {
			throw new BadPayloadException("Translator does not support orchestration with flag 'TRIGGER_INTER_CLOUD=true'", HttpStatus.SC_BAD_REQUEST, origin);
		}
		
		final Set<String> requestedInterfaces = request.getRequestedService().getInterfaces();		
		final OrchestrationFormRequestDTO orchestrationRequest = LegacyModelConverter.convertLegacyServiceRequestFormToOrchestrationFormRequestDTO(request);
		if (orchestrationRequest.getOrchestrationFlags().getOrDefault(Flag.ENABLE_INTER_CLOUD, false)) {
			orchestrationRequest.getOrchestrationFlags().put(Flag.ENABLE_INTER_CLOUD, false);
			logger.debug("Orchestration flag 'ENABLE_INTER_CLOUD=true' is not supported and was changed to false");
		}		
		final boolean originalMatchmakingFlag = orchestrationRequest.getOrchestrationFlags().getOrDefault(Flag.MATCHMAKING, false);
		orchestrationRequest.getOrchestrationFlags().put(Flag.MATCHMAKING, false);
		
		final UriComponents uri = (UriComponents) arrowheadContext.get(LegacyCommonConstants.ORCHESTRATOR_ORCHESTRATION_URI);		
		final ResponseEntity<OrchestrationResponseDTO> response = httpService.sendRequest(uri, HttpMethod.POST, OrchestrationResponseDTO.class, orchestrationRequest);
		final OrchestrationResponseDTO dto = filterOnRequestedInterfaces(requestedInterfaces, response.getBody());	
		
		if (originalMatchmakingFlag && dto.getResponse().size() > 1) {
			dto.setResponse(List.of(dto.getResponse().iterator().next()));
		}
		
		//Generate token if required		
		final List<OrchestrationResultDTO> orchResultsWithLegacyTokenWorkaround = new ArrayList<>();
		for (OrchestrationResultDTO result : dto.getResponse()) {
			if (result.getMetadata() != null
					&& result.getMetadata().containsKey(LegacyCommonConstants.KEY_ARROWHEAD_VERSION)
					&& result.getMetadata().get(LegacyCommonConstants.KEY_ARROWHEAD_VERSION).equalsIgnoreCase(LegacyCommonConstants.ARROWHEAD_VERSION_VALUE_412)) {
				
				//Arrowhead v4.1.2 compliant provider
				if (result.getSecure() == ServiceSecurityType.TOKEN) {
					result = generateLegacyTokenForConsumer412(true, request.getRequesterSystem().getSystemName(), result);							
				}
				if (result != null) { //Can be null when token generation failed -> skip provider							
					orchResultsWithLegacyTokenWorkaround.add(result);
				}
								
			} else {
				
				//Arrowhead v4.1.3 compliant provider 
				if (result.getSecure() == ServiceSecurityType.TOKEN) {
					result = generateLegacyTokenForConsumer412(false, request.getRequesterSystem().getSystemName(), result);								
				}
				orchResultsWithLegacyTokenWorkaround.add(result);					
			}
		}
		dto.setResponse(orchResultsWithLegacyTokenWorkaround);			
		
		final LegacyOrchestrationResponse legacyResponse = LegacyModelConverter.convertOrchestrationResponseDTOtoLegacyOrchestrationResponse(dto);
		if (legacyResponse.getResponse().isEmpty()) {
			new ResponseEntity<>(org.springframework.http.HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(legacyResponse, org.springframework.http.HttpStatus.OK);
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResultDTO generateLegacyTokenForConsumer412(final boolean isProvider412, final String consumerSystemName, final OrchestrationResultDTO result) {
		if (isProvider412) {			
			final Entry<String,String> tokenData = legacyTokenGenerator.generateLegacyToken((String) arrowheadContext.get(LegacyCommonConstants.OWN_CLOUD_OPERATOR),
																							(String) arrowheadContext.get(LegacyCommonConstants.OWN_CLOUD_NAME),
																							consumerSystemName,
																							result.getProvider().getAuthenticationInfo(),
																							result.getService().getServiceDefinition(),
																							result.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE));			
			if (tokenData == null || tokenData.getKey() == null || tokenData.getValue() == null) {
				logger.debug("Token generation failed for the provider ArrowheadSystem");
				return null;
			}
			result.getMetadata().put(LegacyCommonConstants.KEY_LEGACY_SIGNATURE, tokenData.getKey());
			result.getMetadata().put(LegacyCommonConstants.KEY_LEGACY_TOKEN, tokenData.getValue());
			return result;
			
		} else {			
			result.getMetadata().put(LegacyCommonConstants.KEY_LEGACY_SIGNATURE, randomString());
			result.getMetadata().put(LegacyCommonConstants.KEY_LEGACY_TOKEN, result.getAuthorizationTokens().entrySet().iterator().next().getValue());	
			return result;
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private OrchestrationResultDTO generateLegacyTokenForConsumer413(final String consumerSystemName, final OrchestrationResultDTO result) {
		final Entry<String,String> tokenDataLegacyInterface = legacyTokenGenerator.generateLegacyToken((String) arrowheadContext.get(LegacyCommonConstants.OWN_CLOUD_OPERATOR),
				 																					   (String) arrowheadContext.get(LegacyCommonConstants.OWN_CLOUD_NAME),
				 																					   consumerSystemName,
				 																					   result.getProvider().getAuthenticationInfo(),
				 																					   result.getService().getServiceDefinition(),
				 																					   result.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE));
		if (tokenDataLegacyInterface == null || tokenDataLegacyInterface.getKey() == null || tokenDataLegacyInterface.getValue() == null) {
				logger.debug("Token generation with legacy interface failed for the provider ArrowheadSystem");
				return null;
			}
			
			final Entry<String,String> tokenDataDefaultInterface = legacyTokenGenerator.generateLegacyToken((String) arrowheadContext.get(LegacyCommonConstants.OWN_CLOUD_OPERATOR),
																											(String) arrowheadContext.get(LegacyCommonConstants.OWN_CLOUD_NAME),
																											consumerSystemName,
																											result.getProvider().getAuthenticationInfo(),
																											result.getService().getServiceDefinition(),
																											LegacyCommonConstants.DEFAULT_INTERFACE);
			if (tokenDataDefaultInterface == null || tokenDataDefaultInterface.getKey() == null || tokenDataDefaultInterface.getValue() == null) {
				logger.debug("Token generation with default interface failed for the provider ArrowheadSystem");
				return null;
			}
			
			result.getAuthorizationTokens().put(result.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE), tokenDataLegacyInterface.getValue());
			result.getAuthorizationTokens().put(result.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE) + LegacyCommonConstants.SUFFIX_412_SIGNATURE, tokenDataLegacyInterface.getKey());
			result.getAuthorizationTokens().put(LegacyCommonConstants.DEFAULT_INTERFACE, tokenDataDefaultInterface.getValue());
			result.getAuthorizationTokens().put(LegacyCommonConstants.DEFAULT_INTERFACE + LegacyCommonConstants.SUFFIX_412_SIGNATURE, tokenDataDefaultInterface.getKey());
			return result;
	}
	
	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO filterOnRequestedInterfaces(final Iterable<String> requestedInterfaces, final OrchestrationResponseDTO dto) {
		if (requestedInterfaces != null) {
			final List<OrchestrationResultDTO> resultsWithRequestedInterfaces = new ArrayList<>();
			for (final OrchestrationResultDTO result : dto.getResponse()) {
				for (final String interf : requestedInterfaces) {
					if (interf != null && interf.equalsIgnoreCase(result.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE))) {
						final ServiceInterfaceResponseDTO interfaceResponseDTO = result.getInterfaces().iterator().next();
						interfaceResponseDTO.setInterfaceName(result.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE).toUpperCase());
						result.setInterfaces(List.of(interfaceResponseDTO));
						resultsWithRequestedInterfaces.add(result);
					}
				}
			}
			dto.setResponse(resultsWithRequestedInterfaces);
		}
		return dto;
	}
	
	//-------------------------------------------------------------------------------------------------
	private String randomString() {
		return RandomStringUtils.random(150, true, true);
	}
}