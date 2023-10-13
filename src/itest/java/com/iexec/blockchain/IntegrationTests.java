/*
 * Copyright 2021-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain;

import com.iexec.blockchain.api.BlockchainAdapterApiClient;
import com.iexec.blockchain.api.BlockchainAdapterApiClientBuilder;
import com.iexec.blockchain.broker.BrokerService;
import com.iexec.blockchain.tool.ChainConfig;
import com.iexec.blockchain.tool.CredentialsService;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import com.iexec.common.sdk.broker.BrokerOrder;
import com.iexec.commons.poco.chain.*;
import com.iexec.commons.poco.eip712.OrderSigner;
import com.iexec.commons.poco.order.AppOrder;
import com.iexec.commons.poco.order.DatasetOrder;
import com.iexec.commons.poco.order.RequestOrder;
import com.iexec.commons.poco.order.WorkerpoolOrder;
import com.iexec.commons.poco.security.Signature;
import com.iexec.commons.poco.tee.TeeUtils;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.HashUtils;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.iexec.commons.poco.chain.ChainTaskStatus.ACTIVE;
import static com.iexec.commons.poco.chain.ChainTaskStatus.UNSET;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
@ActiveProfiles("itest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    public static final String USER = "admin";
    public static final String PASSWORD = "whatever";
    public static final int BLOCK_TIME_MS = 5000;
    public static final int MAX_BLOCK_TO_WAIT = 3;
    public static final int POLLING_PER_BLOCK = 2;
    public static final int POLLING_INTERVAL_MS = BLOCK_TIME_MS / POLLING_PER_BLOCK;
    public static final int MAX_POLLING_ATTEMPTS = MAX_BLOCK_TO_WAIT * POLLING_PER_BLOCK;

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(new File("docker-compose.yml"))
            .withExposedService("ibaa-chain", 8545, Wait.forListeningPort())
            .withExposedService("ibaa-blockchain-adapter-mongo", 13012, Wait.forListeningPort());

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("chain.id", () -> "65535");
        registry.add("chain.hubAddress", () -> "0xC129e7917b7c7DeDfAa5Fff1FB18d5D7050fE8ca");
        registry.add("chain.nodeAddress", () -> getServiceUrl(environment.getServicePort("ibaa-chain", 8545)));
        registry.add("spring.data.mongodb.port", () -> environment.getServicePort("ibaa-blockchain-adapter-mongo", 13012));
    }

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private IexecHubService iexecHubService;

    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private BrokerService brokerService;

    @Autowired
    private ChainConfig chainConfig;
    private BlockchainAdapterApiClient appClient;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        log.info(">>> {}", testInfo.getDisplayName());
        appClient = BlockchainAdapterApiClientBuilder
                .getInstanceWithBasicAuth(Logger.Level.FULL, getServiceUrl(randomServerPort), USER, PASSWORD);
    }

    private static String getServiceUrl(int servicePort) {
        log.info("service url http://localhost:{}", servicePort);
        return "http://localhost:" + servicePort;
    }

    @Test
    public void shouldBeFinalized() throws Exception {
        TransactionReceipt receipt;
        String dealId = triggerDeal(1);

        String chainTaskId = appClient.requestInitializeTask(dealId, 0);
        Assertions.assertTrue(StringUtils.isNotEmpty(chainTaskId));
        log.info("Requested task initialize: {}", chainTaskId);
        //should wait since returned taskID is computed, initialize is not mined yet
        waitStatus(chainTaskId, ACTIVE, MAX_POLLING_ATTEMPTS);

        String someBytes32Payload = TeeUtils.TEE_SCONE_ONLY_TAG; //any would be fine
        String enclaveChallenge = BytesUtils.EMPTY_ADDRESS;
        String enclaveSignature = BytesUtils.bytesToString(new byte[65]);
        WorkerpoolAuthorization workerpoolAuthorization =
                mockAuthorization(chainTaskId, enclaveChallenge);
        receipt =  iexecHubService.contribute(chainTaskId, someBytes32Payload, enclaveChallenge, enclaveSignature,
                workerpoolAuthorization.getSignature().getValue());
        log.info("receipt {}", receipt);
        waitStatus(chainTaskId, ChainTaskStatus.REVEALING,
            MAX_POLLING_ATTEMPTS);

        receipt = iexecHubService.reveal(chainTaskId, someBytes32Payload);
        log.info("receipt {}", receipt);

        waitBeforeFinalizing(chainTaskId);
        TaskFinalizeArgs taskFinalizeArgs = new TaskFinalizeArgs();
        String finalizeResponseBody = appClient.requestFinalizeTask(chainTaskId, taskFinalizeArgs);
        Assertions.assertTrue(StringUtils.isNotEmpty(finalizeResponseBody));
        log.info("Requested task finalize: {}", finalizeResponseBody);
        waitStatus(chainTaskId, ChainTaskStatus.COMPLETED,
            MAX_POLLING_ATTEMPTS);
    }

    @Test
    public void shouldBurstTransactionsWithAverageOfOneTxPerBlock() {
        int taskVolume = 10;//small volume ensures reasonable execution time on CI/CD
        String dealId = triggerDeal(taskVolume);
        List<CompletableFuture<Void>> txCompletionWatchers = new ArrayList<>();

        IntStream.range(0, taskVolume)
                .forEach(taskIndex -> {
                    //burst transactions (fast sequence) (send "initialize" tx examples for simplicity)
                    String chainTaskId = appClient.requestInitializeTask(dealId, taskIndex);
                    Assertions.assertTrue(StringUtils.isNotEmpty(chainTaskId));
                    log.info("Requested task initialize [index:{}, chainTaskId:{}]",
                            taskIndex, chainTaskId);
                    //wait tx completion outside
                    txCompletionWatchers.add(CompletableFuture.runAsync(() -> {
                        try {
                            //maximum waiting time equals nb of submitted txs
                            //1 tx/block means N txs / N blocks
                            waitStatus(chainTaskId, ACTIVE,
                                (taskVolume + 2) * MAX_POLLING_ATTEMPTS);
                            //no need to wait for propagation update in db
                            Assertions.assertTrue(true);
                        } catch (Exception e) {
                            log.error("Watcher failed with an exception", e);
                            Assertions.fail();
                        }
                    }));
                });
        txCompletionWatchers.forEach(CompletableFuture::join);
    }

    private String triggerDeal(int taskVolume) {
        int secondsPollingInterval = POLLING_INTERVAL_MS / 1000;
        int secondsTimeout = secondsPollingInterval * MAX_POLLING_ATTEMPTS;
        String appAddress = iexecHubService.createApp(buildRandomName("app"),
                "docker.io/repo/name:1.0.0",
                "DOCKER",
                BytesUtils.EMPTY_HEX_STRING_32,
                "",
                secondsTimeout, secondsPollingInterval);
        log.info("Created app: {}", appAddress);
        String workerpool = iexecHubService.createWorkerpool(buildRandomName("pool"),
            secondsTimeout, secondsPollingInterval);
        log.info("Created workerpool: {}", workerpool);
        String datasetAddress = iexecHubService.createDataset(buildRandomName("data"),
                "https://abc.com/def.jpeg",
                BytesUtils.EMPTY_HEX_STRING_32,
                secondsTimeout, secondsPollingInterval);
        log.info("Created datasetAddress: {}", datasetAddress);

        OrderSigner orderSigner = new OrderSigner(
                chainConfig.getId(), chainConfig.getHubAddress(), credentialsService.getCredentials().getEcKeyPair());
        AppOrder signedAppOrder = orderSigner.signAppOrder(buildAppOrder(appAddress, taskVolume));
        WorkerpoolOrder signedWorkerpoolOrder = orderSigner.signWorkerpoolOrder(buildWorkerpoolOrder(workerpool, taskVolume));
        DatasetOrder signedDatasetOrder = orderSigner.signDatasetOrder(buildDatasetOrder(datasetAddress, taskVolume));
        RequestOrder signedRequestOrder = orderSigner.signRequestOrder(buildRequestOrder(signedAppOrder,
                signedWorkerpoolOrder,
                signedDatasetOrder,
                credentialsService.getCredentials().getAddress(),
                DealParams.builder()
                        .iexecArgs("abc")
                        .iexecResultStorageProvider("ipfs")
                        .iexecResultStorageProxy("https://v6.result.goerli.iex.ec")
                        .build()));
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(signedAppOrder)
                .workerpoolOrder(signedWorkerpoolOrder)
                .requestOrder(signedRequestOrder)
                .datasetOrder(signedDatasetOrder)
                .build();

        String dealId = brokerService.matchOrders(brokerOrder);
        Assertions.assertTrue(StringUtils.isNotEmpty(dealId));
        log.info("Created deal: {}", dealId);
        // no need to wait since broker is synchronous, just checking deal existence
        Optional<ChainDeal> chainDeal = iexecHubService.getChainDeal(dealId);
        assertThat(chainDeal).isPresent();
        return dealId;
    }


    private String buildRandomName(String baseName) {
        return baseName + "-" + RandomStringUtils.randomAlphabetic(10);
    }

    private AppOrder buildAppOrder(String appAddress, int volume) {
        return AppOrder.builder()
                .app(appAddress)
                .appprice(BigInteger.ZERO)
                .volume(BigInteger.valueOf(volume))
                .tag(BytesUtils.EMPTY_HEX_STRING_32)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    private WorkerpoolOrder buildWorkerpoolOrder(String workerpoolAddress, int volume) {
        return WorkerpoolOrder.builder()
                .workerpool(workerpoolAddress)
                .workerpoolprice(BigInteger.ZERO)
                .volume(BigInteger.valueOf(volume))
                .tag(BytesUtils.EMPTY_HEX_STRING_32)
                .trust(BigInteger.ZERO)
                .category(BigInteger.ZERO)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    private DatasetOrder buildDatasetOrder(String datasetAddress, int volume) {
        return DatasetOrder.builder()
                .dataset(datasetAddress)
                .datasetprice(BigInteger.ZERO)
                .volume(BigInteger.valueOf(volume))
                .tag(BytesUtils.EMPTY_HEX_STRING_32)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    private RequestOrder buildRequestOrder(
            AppOrder appOrder,
            WorkerpoolOrder workerpoolOrder,
            DatasetOrder datasetOrder,
            String requesterAddress,
            DealParams dealParams) {
        boolean isCompatibleVolume =
                appOrder.getVolume().equals(workerpoolOrder.getVolume())
                        && appOrder.getVolume().equals(datasetOrder.getVolume());
        if (!isCompatibleVolume){
            log.info("Volumes are not compatible");
            return null;
        }
        return RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(appOrder.getAppprice())
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(workerpoolOrder.getWorkerpoolprice())
                .dataset(datasetOrder.getDataset())
                .datasetmaxprice(datasetOrder.getDatasetprice())
                .volume(appOrder.getVolume())
                .category(BigInteger.ZERO)
                .trust(BigInteger.ZERO)
                .tag(BytesUtils.EMPTY_HEX_STRING_32)
                .beneficiary(BytesUtils.EMPTY_ADDRESS)
                .requester(requesterAddress)
                .callback(BytesUtils.EMPTY_ADDRESS)
                //.params("{\"iexec_result_storage_provider\":\"ipfs\",\"iexec_result_storage_proxy\":\"https://v6.result.goerli.iex.ec\",\"iexec_args\":\"abc\"}")
                .params(dealParams.toJsonString())
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    private void waitStatus(String chainTaskId, ChainTaskStatus statusToWait, int maxAttempts) {
        final AtomicInteger attempts = new AtomicInteger();
        Awaitility.await()
                .pollInterval(POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .timeout((long) maxAttempts * POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                            final ChainTaskStatus status = iexecHubService.getChainTask(chainTaskId)
                                    .map(ChainTask::getStatus)
                                    .orElse(UNSET);
                            log.info("Status [status:{}, chainTaskId:{}, attempt:{}]", status, chainTaskId, attempts.incrementAndGet());
                            return status.equals(statusToWait);
                        }
                );
        log.info("Status reached [status:{}, chainTaskId:{}]", statusToWait, chainTaskId);
    }

    private void waitBeforeFinalizing(String chainTaskId) {
        final Optional<ChainTask> oChainTask = iexecHubService.getChainTask(chainTaskId);
        if (oChainTask.isEmpty()) {
            return;
        }
        final ChainTask chainTask = oChainTask.get();
        final int winnerCounter = chainTask.getWinnerCounter();
        log.info("{} {}", POLLING_INTERVAL_MS, MAX_POLLING_ATTEMPTS);

        final AtomicInteger attempts = new AtomicInteger();
        Awaitility.await()
                .pollInterval(POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .timeout((long) MAX_POLLING_ATTEMPTS * POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .until(() -> {
                            final int revealCounter = iexecHubService.getChainTask(chainTaskId)
                                    .map(ChainTask::getRevealCounter)
                                    .orElse(0);
                            log.info("Waiting for reveals ({}/{}), attempt {}", revealCounter, winnerCounter, attempts.incrementAndGet());
                            return revealCounter == winnerCounter;
                        }
                );

        log.info("All revealed ({}/{})", winnerCounter, winnerCounter);
    }

    public WorkerpoolAuthorization mockAuthorization(String chainTaskId,
                                                     String enclaveChallenge) {
        String workerWallet = credentialsService.getCredentials().getAddress();
        String hash = HashUtils.concatenateAndHash(
                workerWallet,
                chainTaskId,
                enclaveChallenge);

        Sign.SignatureData sign = Sign.signPrefixedMessage(BytesUtils.stringToBytes(hash),
                credentialsService.getCredentials().getEcKeyPair());

        return WorkerpoolAuthorization.builder()
                .workerWallet(workerWallet)
                .chainTaskId(chainTaskId)
                .enclaveChallenge(enclaveChallenge)
                .signature(new Signature(sign))
                .build();
    }

}
