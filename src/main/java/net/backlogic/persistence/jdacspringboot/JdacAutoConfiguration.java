package net.backlogic.persistence.jdacspringboot;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import net.backlogic.persistence.client.DataAccessClient;

/**
 * AutoConfiguration class for JDAC Spring Boot Starter.
 */
@AutoConfiguration
@ConditionalOnClass(DataAccessClient.class)
@Import(DataAccessBeanRegistrar.class)
public class JdacAutoConfiguration {}
