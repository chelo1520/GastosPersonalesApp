package com.tallerwebi;

import com.tallerwebi.config.DatabaseInitializationConfig;
import com.tallerwebi.config.HibernateConfig;
import com.tallerwebi.config.SpringWebConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MyServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
  private static final long MAX_REQUEST_SIZE = MAX_FILE_SIZE + 64 * 1024;

  // services and data sources
  @Override
  protected Class<?>[] getRootConfigClasses() {
    return new Class[0];
  }

  // controller, view resolver, handler mapping
  @Override
  protected Class<?>[] getServletConfigClasses() {
    return new Class[] {
      SpringWebConfig.class,
      HibernateConfig.class,
      DatabaseInitializationConfig.class,
    };
  }

  @Override
  protected String[] getServletMappings() {
    return new String[] { "/" };
  }

  @Override
  protected void customizeRegistration(ServletRegistration.Dynamic registration) {
    registration.setMultipartConfig(
      new MultipartConfigElement("", MAX_FILE_SIZE, MAX_REQUEST_SIZE, (int) MAX_FILE_SIZE)
    );
  }

  // Fuerza UTF-8 en requests y responses (incluye JS y CSS estáticos)
  @Override
  protected Filter[] getServletFilters() {
    return new Filter[] { new CharacterEncodingFilter("UTF-8", true) };
  }
}
