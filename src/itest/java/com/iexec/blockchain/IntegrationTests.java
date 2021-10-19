package com.iexec.blockchain;

import com.iexec.blockchain.broker.BrokerService;
import com.iexec.blockchain.signer.SignerService;
import com.iexec.blockchain.tool.ChainConfig;
import com.iexec.blockchain.tool.CredentialsService;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.*;
import com.iexec.common.chain.adapter.args.TaskContributeArgs;
import com.iexec.common.sdk.broker.BrokerOrder;
import com.iexec.common.sdk.order.OrderSigner;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.iexec.common.chain.ChainTaskStatus.ACTIVE;
import static com.iexec.common.chain.ChainTaskStatus.UNSET;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class IntegrationTests {

    public static final String USER = "admin";
    public static final String PASSWORD = "whatever";
    public static final String BASE_URL = "http://localhost:13010";

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

    //@Test
    public void getMetrics() {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(BASE_URL + "/metrics");
        ResponseEntity<String> responseEntity =
                this.restTemplate.exchange(uri.toUriString(), HttpMethod.GET, buildLoggedRequest(), String.class);
        System.out.println("Metrics response code: " + responseEntity.getStatusCode());
        System.out.println("Metrics response body: " + responseEntity.getBody());
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertFalse(responseEntity.getBody().isEmpty());
    }

    /**
     * TODO when match order is ready
     */
    @Test
    public void requestInitialize() throws Exception {
        String appAddress = iexecHubService.createApp(buildRandomName("app"),
                "docker.io/repo/name:1.0.0",
                "DOCKER",
                BytesUtils.EMPTY_HEXASTRING_64,
                "",
                30, 1);
        System.out.println("Created app: " + appAddress);
        String workerpool = iexecHubService.createWorkerpool(buildRandomName("pool"),
                30, 1);
        System.out.println("Created workerpool: " + workerpool);
        String datasetAddress = iexecHubService.createDataset(buildRandomName("data"),
                "https://abc.com/def.jpeg",
                BytesUtils.EMPTY_HEXASTRING_64,
                30, 1);
        System.out.println("Created datasetAddress: " + datasetAddress);

        AppOrder signedAppOrder = signerService.signAppOrder(buildAppOrder(appAddress));
        WorkerpoolOrder signedWorkerpoolOrder = signerService.signWorkerpoolOrder(buildWorkerpoolOrder(workerpool));
        DatasetOrder signedDatasetOrder = signerService.signDatasetOrder(buildDatasetOrder(datasetAddress));
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

        ResponseEntity<String> initialize = requestInitialize(dealId, 0);
        Assertions.assertTrue(initialize.getStatusCode().is2xxSuccessful());
        String chainTaskId = initialize.getBody();
        Assertions.assertTrue(StringUtils.isNotEmpty(chainTaskId));
        System.out.println("Requested task initialize: " + chainTaskId);
        //should wait since returned taskID is computed, initialize is not mined yet
        waitStatus(chainTaskId, ACTIVE);

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
        ResponseEntity<String> contribute = requestContribute(chainTaskId, contributeArgs);
        Assertions.assertTrue(contribute.getStatusCode().is2xxSuccessful());
        Assertions.assertTrue(StringUtils.isNotEmpty(contribute.getBody()));
        System.out.println("Requested task contribute: " + contribute.getBody());
        waitStatus(chainTaskId, ChainTaskStatus.REVEALING);
    }

    private String buildRandomName(String baseName) {
        return baseName + "-" + RandomStringUtils.randomAlphabetic(10);
    }

    private AppOrder buildAppOrder(String appAddress) {
        return AppOrder.builder()
                .app(appAddress)
                .price(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(BytesUtils.EMPTY_HEXASTRING_64)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    private WorkerpoolOrder buildWorkerpoolOrder(String workerpoolAddress) {
        return WorkerpoolOrder.builder()
                .workerpool(workerpoolAddress)
                .price(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(BytesUtils.EMPTY_HEXASTRING_64)
                .trust(BigInteger.ZERO)
                .category(BigInteger.ZERO)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    private DatasetOrder buildDatasetOrder(String datasetAddress) {
        return DatasetOrder.builder()
                .dataset(datasetAddress)
                .price(BigInteger.ZERO)
                .volume(BigInteger.ONE)
                .tag(BytesUtils.EMPTY_HEXASTRING_64)
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
        return RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(appOrder.getAppprice())
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(workerpoolOrder.getWorkerpoolprice())
                .dataset(datasetOrder.getDataset())
                .datasetmaxprice(datasetOrder.getDatasetprice())
                .volume(BigInteger.ONE)
                .category(BigInteger.ZERO)
                .trust(BigInteger.ZERO)
                .tag(BytesUtils.EMPTY_HEXASTRING_64)
                .beneficiary(BytesUtils.EMPTY_ADDRESS)
                .requester(requesterAddress)
                .callback(BytesUtils.EMPTY_ADDRESS)
                //.params("{\"iexec_result_storage_provider\":\"ipfs\",\"iexec_result_storage_proxy\":\"https://v6.result.goerli.iex.ec\",\"iexec_args\":\"abc\"}")
                .params(RequestOrder.toStringParams(dealParams))
                .salt(Hash.sha3String(RandomStringUtils.randomAlphanumeric(20)))
                .build();
    }

    private void waitStatus(String chainTaskId, ChainTaskStatus statusToWait) throws Exception {
        ChainTaskStatus status = iexecHubService.getChainTask(chainTaskId)
                .map(ChainTask::getStatus)
                .orElse(UNSET);
        int maxAttempts = 20;
        int attempts = 0;
        while (!status.equals(statusToWait)) {
            System.out.println("Status is: " + status);
            Thread.sleep(100);
            status = iexecHubService.getChainTask(chainTaskId)
                    .map(ChainTask::getStatus)
                    .orElse(UNSET);
            attempts++;
            if (attempts == maxAttempts) {
                throw new Exception("Too long to wait for task: " + chainTaskId);
            }
        }
        System.out.println("Status reached: " + status);
    }

    private ResponseEntity<String> requestInitialize(String dealId, int taskIndex) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(BASE_URL + "/tasks/initialize")
                .queryParam("chainDealId", dealId)
                .queryParam("taskIndex", taskIndex);
        ResponseEntity<String> responseEntity =
                this.restTemplate.postForEntity(uri.toUriString(), buildLoggedRequest(), String.class);
        System.out.println("Initialize response code: " + responseEntity.getStatusCode());
        System.out.println("Initialize response body: " + responseEntity.getBody());
        return responseEntity;
    }

    private ResponseEntity<String> requestContribute(String chainTaskId, TaskContributeArgs taskContributeArgs) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(BASE_URL + "/tasks/contribute/{chainTaskId}");
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("chainTaskId", chainTaskId);
        ResponseEntity<String> responseEntity =
                this.restTemplate.postForEntity(uri.buildAndExpand(urlParams).toUriString(),
                        new HttpEntity<>(taskContributeArgs, getLoggedHttpHeaders()),
                        String.class);
        System.out.println("Contribute response code: " + responseEntity.getStatusCode());
        System.out.println("Contribute response body: " + responseEntity.getBody());
        return responseEntity;
    }

    private HttpEntity<Object> buildLoggedRequest() {
        HttpHeaders headers = getLoggedHttpHeaders();
        return new HttpEntity<>(headers);
    }

    private HttpHeaders getLoggedHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(USER, PASSWORD);
        return headers;
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
