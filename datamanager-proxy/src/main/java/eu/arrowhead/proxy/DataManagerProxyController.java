package eu.arrowhead.proxy;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.proxy.dto.DataManagerDataResponseDTO;
import eu.arrowhead.proxy.dto.DataManagerServicesResponseDTO;
import eu.arrowhead.proxy.dto.DataManagerSystemsResponseDTO;
import eu.arrowhead.proxy.dto.SenML;


@RestController
@RequestMapping("/datamanager_proxy")
public class DataManagerProxyController {
	
	//=================================================================================================
	// members
	
	public static final String CORE_SERVICE_DATAMANAGER_HISTORIAN_KEY = "historian-uri";
	public static final String CORE_SERVICE_DATAMANAGER_HISTORIAN_SERVICE_KEY = "historian-service-uri";
	public static final String CORE_SERVICE_DATAMANAGER_HISTORIAN_DATA_KEY = "historian-data-uri";
	
	private final Logger logger = LogManager.getLogger(DataManagerProxyController.class);
	
	@Resource(name = CommonConstants.ARROWHEAD_CONTEXT)
	private Map<String,Object> arrowheadContext;
	
	@Autowired
	private HttpService httpService;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = CommonConstants.ECHO_URI)
	@ResponseBody public String echoService() {
		return "Got it!";
	}
	
	//-------------------------------------------------------------------------------------------------
	@GetMapping(value= "/historian")
	@ResponseBody public DataManagerSystemsResponseDTO historianSystems() {
		logger.debug("DataManager:GET:Historian");
		
		final UriComponents uri = getSystemsUri();
		final ResponseEntity<DataManagerSystemsResponseDTO> response = httpService.sendRequest(uri, HttpMethod.GET, DataManagerSystemsResponseDTO.class);
		
		return response.getBody();
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(value= "/historian/{systemName}")
	@ResponseBody public DataManagerServicesResponseDTO historianSystemGet(@PathVariable(value="systemName", required = true) final String systemName) {
		logger.debug("DataManager:GET:Historian/" + systemName);

		final UriComponents uri = getServicesUri(systemName);
		final ResponseEntity<DataManagerServicesResponseDTO> response = httpService.sendRequest(uri, HttpMethod.GET, DataManagerServicesResponseDTO.class);
		
		return response.getBody();
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(value= "/historian/{system}/{service}")
	@ResponseBody public DataManagerDataResponseDTO historianServiceGet(@PathVariable(value="system", required = true) final String systemName, @PathVariable(value="service", required = true) final String serviceName,
														 				@RequestParam final MultiValueMap<String, String> params) {
		logger.debug("DataManager:GET:Historian/" + systemName + "/" + serviceName);
		
		final UriComponents uri = getDataUri(systemName, serviceName, params);
		final DataManagerDataResponseDTO result = new DataManagerDataResponseDTO();
		try {
			final ResponseEntity<List<SenML>> response = httpService.sendRequest(uri, HttpMethod.GET, new ParameterizedTypeReference<List<SenML>>() {});
			result.setData(response.getBody());
		} catch (final ArrowheadException ex) {
			if (ex.getMessage().contains("Unknown error occurred at ")) {
				// means the Data Manager throws a non-Arrowhead DATA_NOT_FOUND
				// do nothing
			}
		}
		
		return result;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private UriComponents getSystemsUri() {
		try {
			return (UriComponents) arrowheadContext.get(CORE_SERVICE_DATAMANAGER_HISTORIAN_KEY);
		} catch (final Exception ex) {
			throw new ArrowheadException(ex.getMessage());
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents getServicesUri(final String system) {
		try {
			final UriComponents uri = (UriComponents) arrowheadContext.get(CORE_SERVICE_DATAMANAGER_HISTORIAN_SERVICE_KEY);
			return uri.expand(system);
		} catch (final Exception ex) {
			throw new ArrowheadException(ex.getMessage());
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private UriComponents getDataUri(final String system, final String service, final MultiValueMap<String, String> params) {
		try {
			final UriComponents uriTemplate = (UriComponents) arrowheadContext.get(CORE_SERVICE_DATAMANAGER_HISTORIAN_DATA_KEY);
			final UriComponents uri = Utilities.createURI(uriTemplate.getScheme(), uriTemplate.getHost(), uriTemplate.getPort(), params, uriTemplate.getPath(), (String) null);
			
			return uri.expand(system, service);
		} catch (final Exception ex) {
			throw new ArrowheadException(ex.getMessage());
		}
	}
}