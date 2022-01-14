package com.iexec.blockchain;

import com.iexec.blockchain.broker.BrokerService;
import com.iexec.blockchain.signer.SignerService;
import com.iexec.blockchain.tool.ChainConfig;
import com.iexec.blockchain.tool.CredentialsService;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.*;
import com.iexec.common.chain.adapter.args.TaskContributeArgs;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import com.iexec.common.chain.adapter.args.TaskRevealArgs;
import com.iexec.common.sdk.broker.BrokerOrder;
import com.iexec.common.sdk.order.payload.AppOrder;
import com.iexec.common.sdk.order.payload.DatasetOrder;
import com.iexec.common.sdk.order.payload.RequestOrder;
import com.iexec.common.sdk.order.payload.WorkerpoolOrder;
import com.iexec.common.security.Signature;
import com.iexec.common.tee.TeeUtils;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.HashUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.iexec.common.chain.ChainTaskStatus.ACTIVE;
import static com.iexec.common.chain.ChainTaskStatus.UNSET;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTests {

    public static final String USER = "admin";
    public static final String PASSWORD = "whatever";
    public static final int BLOCK_TIME_MS = 1000;

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IexecHubService iexecHubService;

    @Autowired
    private CredentialsService credentialsService;

    @Autowired
    private ChainConfig chainConfig;

    @Autowired
    private BrokerService brokerService;

    @Autowired
    private SignerService signerService;
    private BlockchainAdapterApiClient appClient;

    @BeforeEach
    void setUp() {
        appClient = FeignUtils.getFeignBuilder(USER, PASSWORD)
                .target(BlockchainAdapterApiClient.class, getBaseUrl());
    }

    @Test
    public void shouldBeFinalized() throws Exception {
        String dealId = triggerDeal(1);

        String chainTaskId = appClient.requestInitializeTask(dealId, 0);
        Assertions.assertTrue(StringUtils.isNotEmpty(chainTaskId));
        System.out.println("Requested task initialize: " + chainTaskId);
        //should wait since returned taskID is computed, initialize is not mined yet
        waitStatus(chainTaskId, ACTIVE, 1000, 10);

        String someBytes32Payload = TeeUtils.TEE_TAG;
        String enclaveChallenge = BytesUtils.EMPTY_ADDRESS;
        String enclaveSignature = BytesUtils.bytesToString(new byte[65]);
        WorkerpoolAuthorization workerpoolAuthorization =
                mockAuthorization(chainTaskId, enclaveChallenge);
        TaskContributeArgs contributeArgs = new TaskContributeArgs(
                someBytes32Payload,
                workerpoolAuthorization.getSignature().getValue(),
                enclaveChallenge,
                enclaveSignature);
        String contributeResponseBody = appClient.requestContributeTask(chainTaskId, contributeArgs);
        Assertions.assertTrue(StringUtils.isNotEmpty(contributeResponseBody));
        System.out.println("Requested task contribute: " + contributeResponseBody);
        waitStatus(chainTaskId, ChainTaskStatus.REVEALING, 1000, 10);

        TaskRevealArgs taskRevealArgs = new TaskRevealArgs(someBytes32Payload);
        String revealResponseBody = appClient.requestRevealTask(chainTaskId, taskRevealArgs);
        Assertions.assertTrue(StringUtils.isNotEmpty(revealResponseBody));
        System.out.println("Requested task reveal: " + revealResponseBody);

        waitBeforeFinalizing(chainTaskId);
        TaskFinalizeArgs taskFinalizeArgs = new TaskFinalizeArgs();
        String finalizeResponseBody = appClient.requestFinalizeTask(chainTaskId, taskFinalizeArgs);
        Assertions.assertTrue(StringUtils.isNotEmpty(finalizeResponseBody));
        System.out.println("Requested task finalize: " + finalizeResponseBody);
        waitStatus(chainTaskId, ChainTaskStatus.COMPLETED, 1000, 10);
    }

    @Test
    public void shouldBurstTransactionsWithAverageOfOneTxPerBlock(){
        int taskVolume = 10;//small volume ensures reasonable execution time on CI/CD
        String dealId = triggerDeal(taskVolume);
        List<CompletableFuture<Void>> txCompletionWatchers = new ArrayList<>();

        IntStream.range(0, taskVolume)
                .forEach(taskIndex -> {
                    //burst transactions (fast sequence) (send "initialize" tx examples for simplicity)
                    String chainTaskId = appClient.requestInitializeTask(dealId, taskIndex);
                    Assertions.assertTrue(StringUtils.isNotEmpty(chainTaskId));
                    System.out.printf("Requested task initialize " +
                            "[index:%s, chainTaskId:%s]\n", taskIndex, chainTaskId);
                    //wait tx completion outside
                    txCompletionWatchers.add(CompletableFuture.runAsync(() -> {
                        try {
                            //maximum waiting time equals nb of submitted txs
                            //1 tx/block means N txs / N blocks
                            waitStatus(chainTaskId, ACTIVE, BLOCK_TIME_MS, taskVolume + 2);
                            //no need to wait for propagation update in db
                            Assertions.assertTrue(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Assertions.fail();
                        }
                    }));
                });
        txCompletionWatchers.forEach(CompletableFuture::join);
    }

    private String triggerDeal(int taskVolume) {
        String appAddress = iexecHubService.createApp(buildRandomName("app"),
                "docker.io/repo/name:1.0.0",
                "DOCKER",
                BytesUtils.EMPTY_HEX_STRING_32,
                "",
                30, 1);
        System.out.println("Created app: " + appAddress);
        String workerpool = iexecHubService.createWorkerpool(buildRandomName("pool"),
                30, 1);
        System.out.println("Created workerpool: " + workerpool);
        String datasetAddress = iexecHubService.createDataset(buildRandomName("data"),
                "https://abc.com/def.jpeg",
                BytesUtils.EMPTY_HEX_STRING_32,
                30, 1);
        System.out.println("Created datasetAddress: " + datasetAddress);

        AppOrder signedAppOrder = signerService.signAppOrder(buildAppOrder(appAddress, taskVolume));
        WorkerpoolOrder signedWorkerpoolOrder = signerService.signWorkerpoolOrder(buildWorkerpoolOrder(workerpool, taskVolume));
        DatasetOrder signedDatasetOrder = signerService.signDatasetOrder(buildDatasetOrder(datasetAddress, taskVolume));
        RequestOrder signedRequestOrder = signerService.signRequestOrder(buildRequestOrder(signedAppOrder,
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
        System.out.println("Created deal: " + dealId);
        //no need to wait since broker is synchronous, just checking deal
        //existence for double checking
        Optional<ChainDeal> chainDeal = iexecHubService.getChainDeal(dealId);
        Assertions.assertTrue(chainDeal.isPresent());
        return dealId;
    }


    private String buildRandomName(String baseName) {
        return baseName + "-" + RandomStringUtils.randomAlphabetic(10);
    }

    private AppOrder buildAppOrder(String appAddress, int volume) {
        return AppOrder.builder()
                .app(appAddress)
                .price(BigInteger.ZERO)
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
                .price(BigInteger.ZERO)
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
                .price(BigInteger.ZERO)
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
            System.out.println("Volumes are not compatible");
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
                .params(RequestOrder.toStringParams(dealParams))
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    /**
     *
     * @param pollingTimeMs recommended value is block time
     */
    private void waitStatus(String chainTaskId, ChainTaskStatus statusToWait, int pollingTimeMs, int maxAttempts) throws Exception {
        ChainTaskStatus status = null;
        int attempts = 0;
        while(true) {
            System.out.printf("Status [status:%s, chainTaskId:%s]\n", status, chainTaskId);
            status = iexecHubService.getChainTask(chainTaskId)
                    .map(ChainTask::getStatus)
                    .orElse(UNSET);
            attempts++;
            if (status.equals(statusToWait) || attempts > maxAttempts) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(pollingTimeMs);
        }
        if (!status.equals(statusToWait)) {
            throw new Exception("Too long to wait for task: " + chainTaskId);
        }
        System.out.printf("Status reached [status:%s, chainTaskId:%s]\n", status, chainTaskId);
    }

    private void waitBeforeFinalizing(String chainTaskId) throws Exception {
        Optional<ChainTask> oChainTask = iexecHubService.getChainTask(chainTaskId);
        if (oChainTask.isEmpty()) {
            return;
        }
        ChainTask chainTask = oChainTask.get();
        int winnerCounter = chainTask.getWinnerCounter();
        int revealCounter = chainTask.getRevealCounter();
        int maxAttempts = 20;
        int attempts = 0;
        while (revealCounter != winnerCounter) {
            System.out.println("Waiting for reveals (" + revealCounter + "/" + winnerCounter + ")");
            Thread.sleep(100);
            revealCounter = iexecHubService.getChainTask(chainTaskId)
                    .map(ChainTask::getRevealCounter)
                    .orElse(0);
            attempts++;
            if (attempts == maxAttempts) {
                throw new Exception("Too long to wait for reveal: " + chainTaskId);
            }
        }
        System.out.println("All revealed (" + revealCounter + "/" + winnerCounter + ")");
    }

    private String getBaseUrl() {
        return "http://localhost:" + randomServerPort;
    }

    public WorkerpoolAuthorization mockAuthorization(String chainTaskId,
                                                     String enclaveChallenge) {
        String workerWallet = credentialsService.getCredentials().getAddress();
        String hash =
                HashUtils.concatenateAndHash(workerWallet,
                        chainTaskId,
                        enclaveChallenge);

        Sign.SignatureData sign =
                Sign.signPrefixedMessage(BytesUtils.stringToBytes(hash),
                        credentialsService.getCredentials().getEcKeyPair());

        return WorkerpoolAuthorization.builder()
                .workerWallet(workerWallet)
                .chainTaskId(chainTaskId)
                .enclaveChallenge(enclaveChallenge)
                .signature(new Signature(sign))
                .build();
    }

}
