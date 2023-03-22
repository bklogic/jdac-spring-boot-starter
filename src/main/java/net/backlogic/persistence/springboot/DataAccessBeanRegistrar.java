package net.backlogic.persistence.springboot;

import java.util.Set;
import java.util.function.Supplier;

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
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import net.backlogic.persistence.client.DataAccessClient;
import net.backlogic.persistence.client.annotation.BatchService;
import net.backlogic.persistence.client.annotation.CommandService;
import net.backlogic.persistence.client.annotation.QueryService;
import net.backlogic.persistence.client.annotation.RepositoryService;

@Configuration
@Import(DataAccessBeanRegistrar.class)
public class DataAccessBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
	Logger LOGGER = LoggerFactory.getLogger(DataAccessBeanRegistrar.class);

	private DataAccessClient client;
	
	private ClassPathScanningCandidateComponentProvider scanner;
	
	private DataAccessProperties dataAccessProperties;

	@Override
	public void setEnvironment(Environment environment) {
		//load data access properties
		this.dataAccessProperties = new DataAccessProperties();
		this.dataAccessProperties.setBaseUrl(environment.getProperty("das.baseUrl"));
		this.dataAccessProperties.setBasePackage(environment.getProperty("das.basePackage"));
		
		//create data access client
		this.client = DataAccessClient.builder()
				.baseUrl(dataAccessProperties.getBaseUrl())
				.jwtProvider(() -> "eyJraWQiOiJiYWNrbG9naWMtdHJ5LWU5MmU5NGFiLWM2NTMtNDE3Yi1hYmFhLTUwNjNiZWUyMjhlMiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJhdWQiOiJiYWNrbG9naWMtc2VydmljZXMiLCJ3b3Jrc3BhY2UiOiJ0cnk0IiwiaXNzIjoiaHR0cHM6Ly9yZXF1ZXN0LnRyeXByb2QuYmFja2xvZ2ljLm5ldC9hdXRoIn0.cWxU44aj8tUE0_ogUXkPh4HiCv_qWG6tJw7OLcwf70TUdnWdYFAk8JovFcCqFKFZdBfbkkQCNr_6RW9Pp__SZgUagtOo8n3Fzf8p0Yi5P7atQhY64wIf4Y8Hvoh2866XQJpbRvSKk4TKhd7H_RDYTTNqUP6UMwV7Eyzzxd1rxdJPG9O9PZdC0Sp2KLRzix7UsWN7p6BWyr3hJ0tMeVy7d4XjTl75kFh5AL-2u2Kllge7kNeG_Vy3I0mbELNhw8maxVRLeY_ZrKmclibkDi6jtfBXJqn3PPQ_O_V_sMRlq1VOFNIuGn9bdDD2ObFpPWZ582Q82LCvhn0vQDGTpE7ukg")
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
		registBeanDefinitions(definitions, "query", registry);
		
		// commands
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CommandService.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registBeanDefinitions(definitions, "command", registry);
		
		// repositories
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RepositoryService.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registBeanDefinitions(definitions, "repository", registry);
		
		// batches
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(BatchService.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registBeanDefinitions(definitions, "batch", registry);
	}
	
	
	private void registBeanDefinitions(Set<BeanDefinition> definitions, String beanType, BeanDefinitionRegistry registry) {
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


}
