package org.rythmengine.spring.boot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import org.rythmengine.RythmEngine;
import org.rythmengine.exception.RythmException;
import org.rythmengine.spring.RythmEngineFactory;
import org.rythmengine.spring.web.RythmConfigurer;
import org.rythmengine.spring.web.RythmViewResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;


@Configuration
@ConditionalOnClass({ RythmEngine.class})
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(RythmProperties.class)
public class RythmAutoConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(RythmAutoConfiguration.class);

	private final ApplicationContext applicationContext;

	private final RythmProperties properties;

	public RythmAutoConfiguration(ApplicationContext applicationContext, RythmProperties properties) {
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	@PostConstruct
	public void checkTemplateLocationExists() {
		if (this.properties.isCheckTemplateLocation()) {
			TemplateLocation templatePathLocation = null;
			List<TemplateLocation> locations = new ArrayList<TemplateLocation>();
			for (String templateLoaderPath : this.properties.getTemplateLoaderPath()) {
				TemplateLocation location = new TemplateLocation(templateLoaderPath);
				locations.add(location);
				if (location.exists(this.applicationContext)) {
					templatePathLocation = location;
					break;
				}
			}
			if (templatePathLocation == null) {
				logger.warn("Cannot find template location(s): " + locations
						+ " (please add some templates, "
						+ "check your Beetl configuration, or set "
						+ "spring.Beetl.checkTemplateLocation=false)");
			}
		}
	}

	protected static class BeetlConfiguration {

		@Autowired
		protected RythmProperties properties;

		protected void applyProperties(BeetlConfiguration factory) {
			/*factory.setTemplateLoaderPaths(this.properties.getTemplateLoaderPath());
			factory.setPreferFileSystemAccess(this.properties.isPreferFileSystemAccess());
			factory.setDefaultEncoding(this.properties.getCharsetName());*/
			Properties settings = new Properties();
			settings.putAll(this.properties.getSettings());
			//factory.setFreemarkerSettings(settings);
		}

	}
	
	@Configuration
	@ConditionalOnNotWebApplication
	public static class BeetlNonWebConfiguration extends BeetlConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ResourceLoader resourceLoader() {
			return new PathMatchingResourcePatternResolver();
		}
		
		@Bean
		@ConditionalOnMissingBean
		public RythmEngineFactory rythmEngineFactory(ResourceLoader resourceLoader) {
			RythmEngineFactory factory = new RythmEngineFactory();
			//factory.setEnableCache(enableCache);
			factory.setEngineConfig(this.properties.getSettings());
			factory.setResourceLoader(resourceLoader);
			return factory;
		}
		
		@Bean
		@ConditionalOnMissingBean
		public RythmEngine rythmEngine(RythmEngineFactory rythmEngineFactory) throws RythmException, IOException {
			return rythmEngineFactory.createRythmEngine();
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, RythmEngine.class })
	@ConditionalOnWebApplication
	public static class BeetlWebConfiguration extends BeetlConfiguration {
		
		@Bean
		@ConditionalOnMissingBean
		public RythmConfigurer rythmConfigurer(ResourceLoader resourceLoader) {
			RythmConfigurer configurer = new RythmConfigurer();
			//configurer.setEnableCache(enableCache);
			configurer.setEngineConfig(this.properties.getSettings());
			configurer.setResourceLoader(resourceLoader);
			return configurer;
		}
		
		@Bean
		@ConditionalOnMissingBean
		public ResourceLoader resourceLoader() {
			return new PathMatchingResourcePatternResolver();
		}
		
		@Bean
		@ConditionalOnMissingBean(name = "rythmViewResolver")
		@ConditionalOnProperty(name = "spring.rythm.enabled", matchIfMissing = true)
		public RythmViewResolver beetlViewResolver() {
			RythmViewResolver resolver = new RythmViewResolver();
			this.properties.applyToViewResolver(resolver);
			return resolver;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledResourceChain
		public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
			return new ResourceUrlEncodingFilter();
		}

	}
	
}
