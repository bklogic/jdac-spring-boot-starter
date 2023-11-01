package net.backlogic.persistence.springboot;

import net.backlogic.persistence.client.DataAccessClient;
import net.backlogic.persistence.client.annotation.BatchService;
import net.backlogic.persistence.client.annotation.CommandService;
import net.backlogic.persistence.client.annotation.QueryService;
import net.backlogic.persistence.client.annotation.RepositoryService;
import net.backlogic.persistence.client.auth.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

@Configuration
@Import(DataAccessBeanRegistrar.class)
public class DataAccessBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
	Logger LOGGER = LoggerFactory.getLogger(DataAccessBeanRegistrar.class);

	private DataAccessClient client;
	
	private ClassPathScanningCandidateComponentProvider scanner;

	private DataAccessProperties dataAccessProperties;

	@Override
	public void setEnvironment(Environment environment) {
		//basic data access properties
		DataAccessProperties dataAccessProperties = new DataAccessProperties();
		dataAccessProperties.setBaseUrl(environment.getProperty("rdas.baseUrl"));
		dataAccessProperties.setBasePackage(environment.getProperty("rdas.basePackage"));
		dataAccessProperties.setLogRequest(environment.getProperty("rdas.logRequest").equalsIgnoreCase("true"));
		// jwt provider properties
		Properties properties = new Properties();
		if (environment instanceof ConfigurableEnvironment) {
			for (PropertySource<?> propertySource : ((ConfigurableEnvironment) environment).getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource) {
					for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
						// note: there may be multiple entries for same key sorted by importance desc
						if (key.startsWith("rdas.jwtProvider") && !properties.containsKey(key)) {
							properties.put(key, propertySource.getProperty(key));
						}
					}
				}
			}
		}
		dataAccessProperties.setJwtProvider(properties);
		this.dataAccessProperties = dataAccessProperties;

		// client
		JwtProvider jwtProvider = this.getJwtProvider(dataAccessProperties.getJwtProvider());
		this.client = DataAccessClient.builder()
				.baseUrl(dataAccessProperties.getBaseUrl())
				.logRequest(dataAccessProperties.isLogRequest())
				.jwtProvider(jwtProvider)
				.build();

		// create interface scanner
		this.scanner = new ClassPathScanningCandidateComponentProvider(false){
	        // Override isCandidateComponent to only scan for interface
	        @Override
	        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
	          AnnotationMetadata metadata = beanDefinition.getMetadata();
	          return beanDefinition.getMetadata().isIndependent() && metadata.isInterface();
	        }
		};
	}	
	
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// register client bean
		registerClientBean(this.client, registry);
		
		//scan and register beans
		Set<BeanDefinition> definitions;
		String basePackage = dataAccessProperties.getBasePackage();
		
		// queries
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(QueryService.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registerBeanDefinitions(definitions, "query", registry);
		
		// commands
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CommandService.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registerBeanDefinitions(definitions, "command", registry);
		
		// repositories
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RepositoryService.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registerBeanDefinitions(definitions, "repository", registry);
		
		// batches
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(BatchService.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registerBeanDefinitions(definitions, "batch", registry);
	}
	
	
	private void registerBeanDefinitions(Set<BeanDefinition> definitions, String beanType, BeanDefinitionRegistry registry) {
		for (BeanDefinition definition : definitions) {
			Class<?> beanClass = getBeanClass(definition.getBeanClassName());
			
			Supplier<?> instanceSupplier = null;
			switch(beanType) {
			case "query":
				instanceSupplier = ()-> client.getQuery(beanClass);
				break;
			case "command":
				instanceSupplier = ()-> client.getCommand(beanClass);
				break;
			case "repository":
				instanceSupplier = ()-> client.getRepository(beanClass);
				break;
			case "batch":
				instanceSupplier = ()-> client.getBatch(beanClass);
				break;
			}
			
			GenericBeanDefinition targetBeanDefinition = new GenericBeanDefinition();
			targetBeanDefinition.setBeanClass(beanClass);
			targetBeanDefinition.setInstanceSupplier(instanceSupplier);
			registry.registerBeanDefinition(beanClass.getName(), targetBeanDefinition);
		}
	}
	
	
	private void registerClientBean(DataAccessClient client, BeanDefinitionRegistry registry) {
		Supplier<?> instanceSupplier = ()-> { return client; };
		GenericBeanDefinition targetBeanDefinition = new GenericBeanDefinition();
		targetBeanDefinition.setBeanClass(DataAccessClient.class);
		targetBeanDefinition.setInstanceSupplier(instanceSupplier);
		registry.registerBeanDefinition(DataAccessClient.class.getName(), targetBeanDefinition);
	}
	
	
	private Class<?> getBeanClass(String beanClassName){
		Class<?> beanClass = null;
		try {
			beanClass = Class.forName(beanClassName);
		} catch(Exception e) {
			LOGGER.error("Error in getting bean class.", e);
		}
		return beanClass;
	}

	private JwtProvider getJwtProvider(Properties jwtProviderProperties) {
		if (jwtProviderProperties == null) {
			return null;
		}

		String className = jwtProviderProperties.getProperty("class");
		if (className == null) {
			return null;
		}

		switch (className) {
			case "simple":
				className = "net.backlogic.persistence.client.auth.SimpleJwtProvider";
			case "basic":
				className = "net.backlogic.persistence.client.auth.BasicJwtProvider";
		}

		JwtProvider jwtProvider = null;
		try {
			Object provider = Class.forName(className);
			jwtProvider = (JwtProvider) provider;
		} catch (ClassNotFoundException e) {
			throw new RDAS_EXCEPTION("JwtProvider class name not found: " + className);
		} catch (ClassCastException e) {
			throw new RDAS_EXCEPTION("class name is not JwtProvider: " + className);
		}
		return jwtProvider;
	}

}
