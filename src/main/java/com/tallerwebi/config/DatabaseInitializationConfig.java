package com.tallerwebi.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class DatabaseInitializationConfig {

  @Autowired
  private DataSource dataSource;

  @Bean
  public DataSourceInitializer dataSourceInitializer() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    // Datos maestros: categorías y usuario de prueba. No pisa nada
    populator.addScript(new ClassPathResource("data.sql"));
    // Datos ficticios de la demo (generado en el repo de coordinación); va después porque usa
    // las categorías, y recarga los tres usuarios demo en cada arranque
    populator.addScript(new ClassPathResource("demo.sql"));

    DataSourceInitializer initializer = new DataSourceInitializer();
    initializer.setDataSource(dataSource);
    initializer.setDatabasePopulator(populator);

    return initializer;
  }
}
