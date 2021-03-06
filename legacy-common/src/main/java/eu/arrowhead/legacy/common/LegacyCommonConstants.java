package eu.arrowhead.legacy.common;

public class LegacyCommonConstants {

	//=================================================================================================
	// members
	
	public static final String ORCHESTRATION_INPUT_JSON_KEY_SERVICE_DEFINITION_REQUIREMENT = "serviceDefinitionRequirement";
	
	public static final String SERVER_ADDRESS = "server.address";
	public static final String $SERVER_ADDRESS = "${" + SERVER_ADDRESS + "}";
	public static final String SERVER_PORT = "server.port";
	public static final String $SERVER_PORT = "${" + SERVER_PORT + "}";
	public static final String DOMAIN_NAME = "domain.name";
	public static final String $DOMAIN_NAME = "${" + DOMAIN_NAME + ":}";
	public static final String DOMAIN_PORT = "domain.port";
	public static final String $DOMAIN_PORT = "${" + DOMAIN_PORT + ":0}";
	
	public static final String ORCHESTRATOR_ORCHESTRATION_URI = "orchestrator.orchestration.uri";
	public static final String OP_SERVICE_REGISTRY_QUERY_URI = "/query";
	public static final String OP_SERVICE_REGISTRY_REGISTER_URI = "/register";
	public static final String OP_SERVICE_REGISTRY_UNREGISTER_URI = "/unregister";
	
	public static final long CONVERSION_MILLISECOND_TO_SECOND = 1000;
	
	public static final String KEY_SECURITY = "security";
	public static final String KEY_MIN_VERSION = "minVersion";
	public static final String KEY_MAX_VERSION = "maxVersion";
	public static final String KEY_ARROWHEAD_VERSION = "arrowhead.version";
	public static final String KEY_LEGACY_INTERFACE = "legacy.intf";
	public static final String KEY_LEGACY_TOKEN = "legacy.token";
	public static final String KEY_LEGACY_SIGNATURE = "legacy.signature";
	public static final String SUFFIX_412_SIGNATURE = "-SIGNATURE";
	public static final String SECURITY_VALUE_TOKEN = "token";
	public static final String ARROWHEAD_VERSION_VALUE_412 = "412";
	public static final String ARROWHEAD_VERSION_VALUE_413 = "413";
	
	public static final String AUTHORIZATION_KEYSTORE_TYPE = "authorization.server.ssl.key-store-type";
	public static final String $AUTHORIZATION_KEYSTORE_TYPE = "${" + AUTHORIZATION_KEYSTORE_TYPE + "}";
	public static final String AUTHORIZATION_KEYSTORE_PATH = "authorization.server.ssl.key-store";
	public static final String $AUTHORIZATION_KEYSTORE_PATH = "${" + AUTHORIZATION_KEYSTORE_PATH + "}";
	public static final String AUTHORIZATION_KEYSTORE_PASSWORD = "authorization.server.ssl.key-store-password";
	public static final String $AUTHORIZATION_KEYSTORE_PASSWORD = "${" + AUTHORIZATION_KEYSTORE_PASSWORD + "}";
	public static final String AUTHORIZATION_KEY_PASSWORD = "authorization.server.ssl.key-password";
	public static final String $AUTHORIZATION_KEY_PASSWORD = "${" + AUTHORIZATION_KEY_PASSWORD + "}";
	
	public static final String AUTHORIZATION_PRIVATE_KEY = "authorization.private.key";
	public static final String OWN_CLOUD_NAME = "own.cloud.name";
	public static final String OWN_CLOUD_OPERATOR = "own.cloud.operator";
	
	public static final String DEFAULT_INTERFACE = "HTTP-SECURE-JSON";
	
	public static final String FIELD_ERROR_MESSAGE = "errorMessage";
	public static final String FIELD_ERROR_CODE = "errorCode";
	public static final String FIELD_EXCEPTION_TYPE = "exceptionType";
	public static final String FIELD_ORIGIN = "origin";
	public static final String VALUE_DUPLICATE_ENTRY = "DUPLICATE_ENTRY";

	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private LegacyCommonConstants() {
		throw new UnsupportedOperationException();
	}
}