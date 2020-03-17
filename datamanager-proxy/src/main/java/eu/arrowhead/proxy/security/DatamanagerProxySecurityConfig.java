package eu.arrowhead.proxy.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import eu.arrowhead.legacy.common.security.DefaultSecurityConfig;

@Configuration
@EnableWebSecurity
public class DatamanagerProxySecurityConfig extends DefaultSecurityConfig {}