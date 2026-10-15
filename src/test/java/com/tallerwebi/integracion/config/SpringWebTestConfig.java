package com.tallerwebi.integracion.config;

import com.tallerwebi.dominio.comprobante.LectorComprobante;
import com.tallerwebi.dominio.indicadores.ClienteIndicadores;
import com.tallerwebi.dominio.indicadores.CotizacionDolar;
import com.tallerwebi.dominio.indicadores.InflacionMensual;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

@EnableWebMvc
@Configuration
@ComponentScan(
  { "com.tallerwebi.presentacion", "com.tallerwebi.dominio", "com.tallerwebi.infraestructura" }
)
public class SpringWebTestConfig implements WebMvcConfigurer {

  // Spring + Thymeleaf need this
  @Autowired
  private ApplicationContext applicationContext;

  // Indicadores fijos para los tests de integración: nada de red. Dólar a 1.500 todos los
  // días de 2026 e inflación del 2 % mensual hasta agosto de 2026
  @Bean
  @Primary
  public ClienteIndicadores clienteIndicadoresDePrueba() {
    return new ClienteIndicadores() {
      @Override
      public List<CotizacionDolar> obtenerDolarOficialHistorico() {
        List<CotizacionDolar> serie = new ArrayList<>();
        for (
          LocalDate dia = LocalDate.of(2025, 1, 1);
          dia.isBefore(LocalDate.of(2027, 1, 1));
          dia = dia.plusDays(1)
        ) {
          serie.add(new CotizacionDolar(dia, 1450.0, 1500.0));
        }
        return serie;
      }

      @Override
      public CotizacionDolar obtenerDolarOficialDeHoy() {
        return new CotizacionDolar(LocalDate.now(), 1450.0, 1500.0);
      }

      @Override
      public List<InflacionMensual> obtenerInflacionMensual() {
        List<InflacionMensual> serie = new ArrayList<>();
        for (
          YearMonth mes = YearMonth.of(2025, 1);
          !mes.isAfter(YearMonth.of(2026, 8));
          mes = mes.plusMonths(1)
        ) {
          serie.add(new InflacionMensual(mes, 2.0, false));
        }
        return serie;
      }
    };
  }

  @Bean
  @Primary
  public LectorComprobante lectorComprobanteDePrueba() {
    return Mockito.mock(LectorComprobante.class);
  }

  @Bean
  public MultipartResolver multipartResolver() {
    return new StandardServletMultipartResolver();
  }

  @Override
  public void addResourceHandlers(final ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/css/**").addResourceLocations("/resources/core/css/");
    registry.addResourceHandler("/js/**").addResourceLocations("/resources/core/js/");
    registry.addResourceHandler("/img/**").addResourceLocations("/resources/core/img/");
    registry.addResourceHandler("/webjars/**").addResourceLocations("/webjars/");
  }

  // https://www.thymeleaf.org/doc/tutorials/3.0/thymeleafspring.html
  // Spring + Thymeleaf
  @Bean
  public SpringResourceTemplateResolver templateResolver() {
    // SpringResourceTemplateResolver automatically integrates with Spring's own
    // resource resolution infrastructure, which is highly recommended.
    SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
    templateResolver.setApplicationContext(this.applicationContext);
    templateResolver.setPrefix("/WEB-INF/views/thymeleaf/");
    templateResolver.setSuffix(".html");
    // HTML is the default value, added here for the sake of clarity.
    templateResolver.setTemplateMode(TemplateMode.HTML);
    // Template cache is true by default. Set to false if you want
    // templates to be automatically updated when modified.
    templateResolver.setCacheable(true);
    return templateResolver;
  }

  // Spring + Thymeleaf
  @Bean
  public SpringTemplateEngine templateEngine() {
    // SpringTemplateEngine automatically applies SpringStandardDialect and
    // enables Spring's own MessageSource message resolution mechanisms.
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver());
    // Enabling the SpringEL compiler with Spring 4.2.4 or newer can
    // speed up execution in most scenarios, but might be incompatible
    // with specific cases when expressions in one template are reused
    // across different data types, so this flag is "false" by default
    // for safer backwards compatibility.
    templateEngine.setEnableSpringELCompiler(true);
    return templateEngine;
  }

  // Spring + Thymeleaf
  // Configure Thymeleaf View Resolver
  @Bean
  public ThymeleafViewResolver viewResolver() {
    ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(templateEngine());
    return viewResolver;
  }
}
