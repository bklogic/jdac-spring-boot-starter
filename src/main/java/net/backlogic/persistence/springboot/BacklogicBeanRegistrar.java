package net.backlogic.persistence.springboot;

import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import net.backlogic.persistence.client.PersistenceClient;
import net.backlogic.persistence.client.annotation.BacklogicCommand;
import net.backlogic.persistence.client.annotation.BacklogicQuery;
import net.backlogic.persistence.client.annotation.BacklogicRepository;

@Configuration
@Import(net.backlogic.persistence.springboot.BacklogicBeanRegistrar.class)
public class BacklogicBeanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

	private PersistenceClient client;
	
	private ClassPathScanningCandidateComponentProvider scanner;
	
	private BacklogicProperties backlogicProperties;

	@Autowired
	private ApplicationContext applicationContext;
	
	@Override
	public void setEnvironment(Environment environment) {
		//load backlogic properties
		this.backlogicProperties = new BacklogicProperties();
		this.backlogicProperties.setBaseUrl(environment.getProperty("backlogic.peristence.baseUrl"));
		this.backlogicProperties.setBasePackage(environment.getProperty("backlogic.peristence.basePackage"));
		
		//create persistence client
		this.client = new PersistenceClient(backlogicProperties.getBaseUrl());
		
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
		//scan and register beans
		Set<BeanDefinition> definitions;
		String basePackage = backlogicProperties.getBasePackage();
		
		
		//queries
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(BacklogicQuery.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registBeanDefinitions(definitions, "query", registry);
		
		//commands
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(BacklogicCommand.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registBeanDefinitions(definitions, "command", registry);
		
		//repositories
		scanner.resetFilters(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(BacklogicRepository.class));
		definitions = scanner.findCandidateComponents(basePackage);
		registBeanDefinitions(definitions, "repository", registry);
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
			}
			
			GenericBeanDefinition targetBeanDefinition = new GenericBeanDefinition();
			targetBeanDefinition.setBeanClass(beanClass);
			targetBeanDefinition.setInstanceSupplier(instanceSupplier);
			registry.registerBeanDefinition(beanClass.getName(), targetBeanDefinition);
		}
	}
	
	
	private Class<?> getBeanClass(String beanClassName){
		Class<?> beanClass = null;
		try {
			beanClass = Class.forName(beanClassName);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return beanClass;
	}


}
