/*
 * Copyright 2023-2023 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iexec.blockchain.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@Import(ProjectInfoAutoConfiguration.class)
class OpenApiConfigTests {

    @Autowired
    private BuildProperties buildProperties;

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig(buildProperties);
    }

    @Test
    void shouldReturnOpenAPIObjectWithCorrectInfo() {
        OpenAPI api = openApiConfig.api();
        assertThat(api).isNotNull();

        assertThat(api.getInfo()).isNotNull().
                extracting(
                        Info::getVersion,
                        Info::getTitle
                )
                .containsExactly(buildProperties.getVersion(), OpenApiConfig.TITLE);

        assertThat(api.getComponents()).isNotNull();
        SecurityScheme securityScheme = api.getComponents().getSecuritySchemes().get(OpenApiConfig.SWAGGER_SECURITY_SCHEME_KEY);

        assertThat(securityScheme)
                .isNotNull()
                .extracting(
                        SecurityScheme::getType,
                        SecurityScheme::getScheme,
                        SecurityScheme::getIn,
                        SecurityScheme::getName
                )
                .containsExactly(SecurityScheme.Type.HTTP, OpenApiConfig.SWAGGER_SECURITY_SCHEME, SecurityScheme.In.HEADER, OpenApiConfig.SWAGGER_BASIC_AUTH);
    }
}
