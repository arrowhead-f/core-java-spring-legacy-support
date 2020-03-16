package eu.arrowhead.legacy.common.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CloudRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationFlags;
import eu.arrowhead.common.dto.shared.OrchestrationFormRequestDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResponseDTO;
import eu.arrowhead.common.dto.shared.OrchestrationResultDTO;
import eu.arrowhead.common.dto.shared.PreferredProviderDataDTO;
import eu.arrowhead.common.dto.shared.ServiceInterfaceResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryFormDTO;
import eu.arrowhead.common.dto.shared.ServiceQueryResultDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.shared.ServiceRegistryResponseDTO;
import eu.arrowhead.common.dto.shared.ServiceSecurityType;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.dto.shared.SystemResponseDTO;
import eu.arrowhead.legacy.common.LegacyCommonConstants;


public class LegacyModelConverter {
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public static SystemRequestDTO convertLegacyArrowheadSystemToSystemRequestDTO(final LegacyArrowheadSystem legacySystem) {
		final SystemRequestDTO systemRequestDTO = new SystemRequestDTO();
		systemRequestDTO.setSystemName(legacySystem.getSystemName());
		systemRequestDTO.setAddress(legacySystem.getAddress());
		systemRequestDTO.setPort(legacySystem.getPort());
		systemRequestDTO.setAuthenticationInfo(legacySystem.getAuthenticationInfo());
		return systemRequestDTO;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static LegacyArrowheadSystem convertSystemResponseDTOToLegacyArrowheadSystem(final SystemResponseDTO dto) {
		final LegacyArrowheadSystem legacySystem = new LegacyArrowheadSystem();
		legacySystem.setId(dto.getId());
		legacySystem.setSystemName(dto.getSystemName());
		legacySystem.setAddress(dto.getAddress());
		legacySystem.setPort(dto.getPort());
		legacySystem.setAuthenticationInfo(dto.getAuthenticationInfo());
		return legacySystem;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static CloudRequestDTO convertLegacyArrowheadCloudToCloudRequestDTO(final LegacyArrowheadCloud legacyCloud) {
		final CloudRequestDTO cloudRequestDTO = new CloudRequestDTO();
		cloudRequestDTO.setOperator(legacyCloud.getOperator());
		cloudRequestDTO.setName(legacyCloud.getCloudName());
		cloudRequestDTO.setSecure(legacyCloud.getSecure());
		cloudRequestDTO.setAuthenticationInfo(legacyCloud.getAuthenticationInfo());
		return cloudRequestDTO;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static LegacyServiceRegistryEntry convertServiceRegistryResponseDTOToLegacyServiceRegistryEntry(final ServiceRegistryResponseDTO dto) {
		final LegacyArrowheadService providedService = new LegacyArrowheadService();
		providedService.setId(dto.getServiceDefinition().getId());
		providedService.setServiceDefinition(dto.getServiceDefinition().getServiceDefinition());
		if (dto.getMetadata().containsKey(LegacyCommonConstants.KEY_LEGACY_INTERFACE)) {
			providedService.setInterfaces(Set.of(dto.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE)));
		} else {
			providedService.setInterfaces(dto.getInterfaces().stream().map(e -> e.getInterfaceName()).collect(Collectors.toSet()));
		}
		providedService.setServiceMetadata(dto.getMetadata());
		providedService.getServiceMetadata().put(LegacyCommonConstants.KEY_SECURITY, dto.getSecure().name());
		providedService.getServiceMetadata().remove(LegacyCommonConstants.KEY_LEGACY_INTERFACE);
		providedService.getServiceMetadata().remove(LegacyCommonConstants.KEY_ARROWHEAD_VERSION);
		
		final LegacyArrowheadSystem provider = new LegacyArrowheadSystem();
		provider.setId(dto.getProvider().getId());
		provider.setSystemName(dto.getProvider().getSystemName());
		provider.setAddress(dto.getProvider().getAddress());
		provider.setPort(dto.getProvider().getPort());
		provider.setAuthenticationInfo(dto.getProvider().getAuthenticationInfo());
		
		final LegacyServiceRegistryEntry entry = new LegacyServiceRegistryEntry();
		entry.setId(dto.getId());
		entry.setProvidedService(providedService);
		entry.setProvider(provider);
		entry.setServiceURI(dto.getServiceUri());
		entry.setUdp(false);
		entry.setVersion(dto.getVersion());
		entry.setEndOfValidity(Utilities.isEmpty(dto.getEndOfValidity()) ? null : convertUTCSTringToLocalDateTime(dto.getEndOfValidity()));
		
		return entry;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static ServiceRegistryRequestDTO convertLegacyServiceRegistryEntryToServiceRegistryRequestDTO(final LegacyServiceRegistryEntry entry) {
		final SystemRequestDTO provider = convertLegacyArrowheadSystemToSystemRequestDTO(entry.getProvider());
		
		if (entry.getProvidedService().getServiceMetadata() == null) {
			entry.getProvidedService().setServiceMetadata(new HashMap<>());
		} 
		entry.getProvidedService().getServiceMetadata().put(LegacyCommonConstants.KEY_ARROWHEAD_VERSION, LegacyCommonConstants.ARROWHEAD_VERSION_VALUE_412);
		
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition(entry.getProvidedService().getServiceDefinition());
		dto.setProviderSystem(provider);
		dto.setServiceUri(entry.getServiceURI());
		dto.setVersion(entry.getVersion());
		dto.setSecure(calculateSecurityTypeStr(entry.getProvidedService().getServiceMetadata()));
		entry.getProvidedService().getServiceMetadata().remove(LegacyCommonConstants.KEY_SECURITY);
		entry.getProvidedService().getServiceMetadata().put(LegacyCommonConstants.KEY_LEGACY_INTERFACE, entry.getProvidedService().getInterfaces().iterator().next());
		dto.setInterfaces(List.of(LegacyCommonConstants.DEFAULT_INTERFACE));
		dto.setMetadata(entry.getProvidedService().getServiceMetadata());
		dto.setEndOfValidity(entry.getEndOfValidity() == null ? null : convertEndOfValidityToUTCString(entry.getEndOfValidity()));
		
		return dto;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static ServiceQueryFormDTO convertLegacyServiceQueryFormToServiceQueryFormDTO(final LegacyServiceQueryFrom form) {
		final Map<String,String> metadata = form.getService().getServiceMetadata();
		final ServiceQueryFormDTO.Builder builder = new ServiceQueryFormDTO.Builder(form.getService().getServiceDefinition())
															   	   		   .version(form.getVersion())
															   	   		   .pingProviders(form.isPingProviders());

		if (metadata.containsKey(LegacyCommonConstants.KEY_SECURITY)) {
			builder.security(metadata.get(LegacyCommonConstants.KEY_SECURITY).equalsIgnoreCase(LegacyCommonConstants.SECURITY_VALUE_TOKEN) ? 
							 ServiceSecurityType.TOKEN : ServiceSecurityType.CERTIFICATE);
			metadata.remove(LegacyCommonConstants.KEY_SECURITY);
		}
		
		if (metadata.containsKey(LegacyCommonConstants.KEY_MIN_VERSION) || metadata.containsKey(LegacyCommonConstants.KEY_MAX_VERSION)) {
			final String minVersionStr = metadata.get(LegacyCommonConstants.KEY_MIN_VERSION);
			final String maxVersionStr = metadata.get(LegacyCommonConstants.KEY_MAX_VERSION);
			
			Integer minVersion = null, maxVersion = null;
			try {
				minVersion = Utilities.isEmpty(minVersionStr) ? null : Integer.parseInt(minVersionStr);
			} catch (final NumberFormatException ex) {
				// intentionally ignored
			}
			
			try {
				maxVersion = Utilities.isEmpty(maxVersionStr) ? null : Integer.parseInt(maxVersionStr);
			} catch (final NumberFormatException ex) {
				// intentionally ignored
			}

			builder.version(minVersion, maxVersion);
			metadata.remove(LegacyCommonConstants.KEY_MIN_VERSION);
			metadata.remove(LegacyCommonConstants.KEY_MAX_VERSION);
			
			if (metadata.size() > 0) {
				builder.metadata(metadata);
			}
		}
		
		return builder.build();
	}
	
	//-------------------------------------------------------------------------------------------------
	public static LegacyServiceQueryResult convertServiceQueryResultDTOToLegacyServiceQueryResult(final ServiceQueryResultDTO dto) {
		final LegacyServiceQueryResult result = new LegacyServiceQueryResult();
		final List<LegacyServiceRegistryEntry> resultList = new ArrayList<LegacyServiceRegistryEntry>(dto.getServiceQueryData().size());
		
		for (final ServiceRegistryResponseDTO srDTO : dto.getServiceQueryData()) {
			resultList.add(convertServiceRegistryResponseDTOToLegacyServiceRegistryEntry(srDTO));
		}
		
		result.setServiceQueryData(resultList);
		
		return result;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static OrchestrationFormRequestDTO convertLegacyServiceRequestFormToOrchestrationFormRequestDTO(final LegacyServiceRequestForm form) {
		final SystemRequestDTO requesterSystem = convertLegacyArrowheadSystemToSystemRequestDTO(form.getRequesterSystem());
		final CloudRequestDTO requesterCloud = form.getRequesterCloud() != null ? convertLegacyArrowheadCloudToCloudRequestDTO(form.getRequesterCloud()) : null;
		
		final ServiceQueryFormDTO requestedService = new ServiceQueryFormDTO.Builder(form.getRequestedService().getServiceDefinition())
																			.interfaces() //intentionally ignored
																			.security(calculateSecurityType(form.getRequestedService().getServiceMetadata()))
																			.metadata(form.getRequestedService().getServiceMetadata())
																			.pingProviders(form.getOrchestrationFlags().getOrDefault(CommonConstants.ORCHESTRATON_FLAG_PING_PROVIDERS, false))
																			.build();
		
		requestedService.getMetadataRequirements().remove(LegacyCommonConstants.KEY_SECURITY);
		
		if (form.getRequestedService().getServiceMetadata().containsKey(LegacyCommonConstants.KEY_MIN_VERSION)
				|| form.getRequestedService().getServiceMetadata().containsKey(LegacyCommonConstants.KEY_MAX_VERSION)) {
			
			final String minVersionStr = form.getRequestedService().getServiceMetadata().get(LegacyCommonConstants.KEY_MIN_VERSION);
			final String maxVersionStr = form.getRequestedService().getServiceMetadata().get(LegacyCommonConstants.KEY_MAX_VERSION);			
			Integer minVersion = null, maxVersion = null;
			try {
				minVersion = Utilities.isEmpty(minVersionStr) ? null : Integer.parseInt(minVersionStr);
			} catch (final NumberFormatException ex) {} // intentionally ignored			
			try {
				maxVersion = Utilities.isEmpty(maxVersionStr) ? null : Integer.parseInt(maxVersionStr);
			} catch (final NumberFormatException ex) {} // intentionally ignored			
			requestedService.setMinVersionRequirement(minVersion);
			requestedService.setMaxVersionRequirement(maxVersion);
			requestedService.getMetadataRequirements().remove(LegacyCommonConstants.KEY_MIN_VERSION);
			requestedService.getMetadataRequirements().remove(LegacyCommonConstants.KEY_MAX_VERSION);
		}
		
		return new OrchestrationFormRequestDTO.Builder(requesterSystem)
											  .requesterCloud(requesterCloud)
											  .requestedService(requestedService)
											  .flags(new OrchestrationFlags(form.getOrchestrationFlags()))
											  .preferredProviders(convertLegacyPreferredProviderListToPreferredProviderListDTO(form.getPreferredProviders()))
											  .commands(form.getCommands())
											  .build();
	}
	
	//-------------------------------------------------------------------------------------------------
	public static LegacyOrchestrationResponse convertOrchestrationResponseDTOtoLegacyOrchestrationResponse(final OrchestrationResponseDTO dto) {
		final List<LegacyOrchestrationForm> legacyForms = new ArrayList<>();
		
		for (final OrchestrationResultDTO orchResult : dto.getResponse()) {
			LegacyOrchestrationForm legacyForm = new LegacyOrchestrationForm();
			legacyForm.setProvider(convertSystemResponseDTOToLegacyArrowheadSystem(orchResult.getProvider()));
			final Set<String> interfaces = new HashSet<>();
			for (final ServiceInterfaceResponseDTO interf : orchResult.getInterfaces()) {
				interfaces.add(interf.getInterfaceName());
			}
			interfaces.add(orchResult.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_INTERFACE));
			legacyForm.setService(new LegacyArrowheadService(orchResult.getService().getId(), orchResult.getService().getServiceDefinition(), interfaces, orchResult.getMetadata()));
			legacyForm.setServiceURI(orchResult.getServiceUri());
			legacyForm.setWarnings(orchResult.getWarnings());
			legacyForm.setAuthorizationToken(orchResult.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_TOKEN));
			legacyForm.setSignature(orchResult.getMetadata().get(LegacyCommonConstants.KEY_LEGACY_SIGNATURE));			
			legacyForm.getService().getServiceMetadata().remove(LegacyCommonConstants.KEY_LEGACY_TOKEN);
			legacyForm.getService().getServiceMetadata().remove(LegacyCommonConstants.KEY_LEGACY_SIGNATURE);
			if (orchResult.getSecure() == ServiceSecurityType.TOKEN) {
				legacyForm.getService().getServiceMetadata().put(LegacyCommonConstants.KEY_SECURITY, LegacyCommonConstants.SECURITY_VALUE_TOKEN);
			}
			if (orchResult.getSecure() == ServiceSecurityType.CERTIFICATE) {
				legacyForm.getService().getServiceMetadata().put(LegacyCommonConstants.KEY_SECURITY, ServiceSecurityType.CERTIFICATE.name().toLowerCase());
			}
			legacyForms.add(legacyForm);
		}
		
		final LegacyOrchestrationResponse legacyResponse = new LegacyOrchestrationResponse();
		legacyResponse.setResponse(legacyForms);
		return legacyResponse;
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private static LocalDateTime convertUTCSTringToLocalDateTime(final String utcTime) {
		final ZonedDateTime zonedDateTime = Utilities.parseUTCStringToLocalZonedDateTime(utcTime);
		return zonedDateTime.toLocalDateTime(); 
	}
	
	//-------------------------------------------------------------------------------------------------
	private static String convertEndOfValidityToUTCString(final LocalDateTime localDateTime) {
		final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
		return Utilities.convertZonedDateTimeToUTCString(zonedDateTime);
	}
	
	//-------------------------------------------------------------------------------------------------
	private static ServiceSecurityType calculateSecurityType(final Map<String,String> metadata) {
		if (metadata == null) {
			return ServiceSecurityType.NOT_SECURE;
		}
		
		if (metadata.containsKey(LegacyCommonConstants.KEY_SECURITY)) {
			return metadata.get(LegacyCommonConstants.KEY_SECURITY).equalsIgnoreCase(LegacyCommonConstants.SECURITY_VALUE_TOKEN) ? ServiceSecurityType.TOKEN : ServiceSecurityType.CERTIFICATE;
		} else {
			return ServiceSecurityType.NOT_SECURE;
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private static String calculateSecurityTypeStr(final Map<String,String> metadata) {
		final ServiceSecurityType type = calculateSecurityType(metadata);
		
		return type == null ? null : type.name();
	}
	
	//-------------------------------------------------------------------------------------------------
	private static PreferredProviderDataDTO[] convertLegacyPreferredProviderListToPreferredProviderListDTO(final List<LegacyPreferredProvider> legacyPrefferedProviderList) {
		final PreferredProviderDataDTO[] preferredProviders = new PreferredProviderDataDTO[legacyPrefferedProviderList.size()];
		int index = 0;
		for (final LegacyPreferredProvider legacyProvider : legacyPrefferedProviderList) {
			final PreferredProviderDataDTO providerDataDTO = new PreferredProviderDataDTO();
			providerDataDTO.setProviderSystem(convertLegacyArrowheadSystemToSystemRequestDTO(legacyProvider.getProviderSystem()));
			providerDataDTO.setProviderCloud(convertLegacyArrowheadCloudToCloudRequestDTO(legacyProvider.getProviderCloud()));
			preferredProviders[index] = providerDataDTO;
			index++;
		}
		
		return preferredProviders;
	}

	//-------------------------------------------------------------------------------------------------
	private LegacyModelConverter() {
		throw new UnsupportedOperationException();
	}
}