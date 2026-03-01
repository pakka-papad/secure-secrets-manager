package com.example.secrets_manager.config;

import com.example.secrets_manager.security.SecurityConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI/Swagger documentation. Configures the OAuth2 Password flow to enable
 * automatic login and token management in Swagger UI.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
        .info(
            new Info()
                .title("Secrets Manager API")
                .version("1.0")
                .description("Secure API for managing credentials, secret groups, and auditing."))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.OAUTH2)
                        .description(
                            "Enter your credentials to obtain an access token automatically.")
                        .flows(
                            new OAuthFlows()
                                .password(
                                    new OAuthFlow().tokenUrl(SecurityConstants.AUTH_LOGIN_URL)))));
  }
}
