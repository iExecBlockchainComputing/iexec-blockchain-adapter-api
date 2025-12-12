/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.chain;

import com.github.dockerjava.api.model.AuthConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.iexec.blockchain.chain.BlockchainListener.LATEST_BLOCK_METRIC_NAME;
import static com.iexec.blockchain.chain.BlockchainListener.TX_COUNT_METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BlockchainListenerTests {

    private static final String CHAIN_SVC_NAME = "ibaa-chain";
    private static final int CHAIN_SVC_PORT = 8545;
    private static final String MONGO_SVC_NAME = "ibaa-blockchain-adapter-mongo";
    private static final int MONGO_SVC_PORT = 27017;

    @BeforeAll
    static void setupAuth() {
        final AuthConfig authConfig = new AuthConfig()
                .withUsername(System.getProperty("registryUsername"))
                .withPassword(System.getProperty("registryPassword"))
                .withRegistryAddress(System.getProperty("registryAddress"));

        // Configure Docker client with auth
        DockerClientFactory.instance()
                .client()
                .authCmd()
                .withAuthConfig(authConfig)
                .exec();
    }

    @Container
    static ComposeContainer environment = new ComposeContainer(new File("docker-compose.yml"))
            .withExposedService(CHAIN_SVC_NAME, CHAIN_SVC_PORT, Wait.forListeningPort())
            .withExposedService(MONGO_SVC_NAME, MONGO_SVC_PORT, Wait.forListeningPort())
            .withPull(true);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("chain.id", () -> "65535");
        registry.add("chain.hub-address", () -> "0xc4b11f41746D3Ad8504da5B383E1aB9aa969AbC7");
        registry.add("chain.node-address", () -> getServiceUrl(
                environment.getServiceHost(CHAIN_SVC_NAME, CHAIN_SVC_PORT),
                environment.getServicePort(CHAIN_SVC_NAME, CHAIN_SVC_PORT)));
        registry.add("sprint.data.mongodb.host", () -> environment.getServiceHost(MONGO_SVC_NAME, MONGO_SVC_PORT));
        registry.add("spring.data.mongodb.port", () -> environment.getServicePort(MONGO_SVC_NAME, MONGO_SVC_PORT));
    }

    @Autowired
    private MeterRegistry meterRegistry;

    private static String getServiceUrl(String serviceHost, int servicePort) {
        log.info("service url http://{}:{}", serviceHost, servicePort);
        return "http://" + serviceHost + ":" + servicePort;
    }

    @Test
    void shouldConnect() {
        await().atMost(10L, TimeUnit.SECONDS)
                .until(() -> Objects.requireNonNull(meterRegistry.find(LATEST_BLOCK_METRIC_NAME).gauge()).value() != 0.0);
        assertThat(meterRegistry.find(TX_COUNT_METRIC_NAME).tag("block", "latest").gauge())
                .isNotNull()
                .extracting(Gauge::value)
                .isEqualTo(0.0);
        assertThat(meterRegistry.find(TX_COUNT_METRIC_NAME).tag("block", "pending").gauge())
                .isNotNull()
                .extracting(Gauge::value)
                .isEqualTo(0.0);
    }

}
