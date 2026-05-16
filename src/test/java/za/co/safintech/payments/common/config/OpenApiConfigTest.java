package za.co.safintech.payments.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void shouldDescribeApiAndBearerJwtSecurity() {
        OpenAPI openAPI = new OpenApiConfig().saFintechOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("sa-fintech-payments-api");
        assertThat(openAPI.getInfo().getDescription()).contains("never processes real money");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfig.BEARER_AUTH_SCHEME);

        SecurityScheme bearerAuth = openAPI.getComponents()
                .getSecuritySchemes()
                .get(OpenApiConfig.BEARER_AUTH_SCHEME);

        assertThat(bearerAuth.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(bearerAuth.getScheme()).isEqualTo("bearer");
        assertThat(bearerAuth.getBearerFormat()).isEqualTo("JWT");
    }
}
