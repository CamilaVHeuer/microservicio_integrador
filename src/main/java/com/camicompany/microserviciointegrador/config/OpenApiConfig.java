package com.camicompany.microserviciointegrador.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  final String securitySchemeName = "ApiKeyAuth";

  @Bean
  public OpenAPI openApi() {

    return new OpenAPI()
        .info(
            new Info()
                .title("Microservicio Integrador API")
                .description("API for payment integration with Helipagos")
                .version("1.0.0")
                .contact(
                    new Contact()
                        .name("Camila Villalba Heuer")
                        .email("cbvillalbaheuer@gmail.com")
                        .url("https://github.com/CamilaVHeuer")))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-KEY")
                        .description("API Key authentication")));
  }
}
