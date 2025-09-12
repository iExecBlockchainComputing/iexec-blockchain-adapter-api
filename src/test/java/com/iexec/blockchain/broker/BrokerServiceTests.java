/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.broker;

import com.iexec.blockchain.chain.ChainConfig;
import com.iexec.blockchain.chain.IexecHubService;
import com.iexec.blockchain.command.generic.SubmittedTx;
import com.iexec.common.sdk.broker.BrokerOrder;
import com.iexec.commons.poco.chain.ChainAccount;
import com.iexec.commons.poco.chain.SignerService;
import com.iexec.commons.poco.order.*;
import com.iexec.commons.poco.utils.BytesUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import static com.iexec.blockchain.broker.BrokerService.SCHEDULER_NOTICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class BrokerServiceTests {

    @Mock
    private ChainConfig chainConfig;
    @Mock
    private IexecHubService iexecHubService;
    @Mock
    private SignerService signerService;
    @InjectMocks
    private BrokerService brokerService;

    @Mock
    RemoteFunctionCall<TransactionReceipt> remoteCall;

    private static final ChainAccount deposit = ChainAccount.builder().deposit(100L).build();
    private static final ChainAccount emptyDeposit = ChainAccount.builder().deposit(0L).build();

    AppOrder generateAppOrder() {
        return AppOrder.builder()
                .app(generateEthereumAddress())
                .appprice(BigInteger.ONE)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(BytesUtils.toByte32HexString(1))
                .sign("sign")
                .build();
    }

    DatasetOrder generateDatasetOrder(boolean withDataset) {
        String address = withDataset ? generateEthereumAddress() : BytesUtils.EMPTY_ADDRESS;
        return DatasetOrder.builder()
                .dataset(address)
                .datasetprice(BigInteger.ONE)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .workerpoolrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(BytesUtils.toByte32HexString(1))
                .sign("sign")
                .build();
    }

    WorkerpoolOrder generateWorkerpoolOrder() {
        return WorkerpoolOrder.builder()
                .workerpool(generateEthereumAddress())
                .workerpoolprice(BigInteger.ONE)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .trust(BigInteger.ZERO)
                .category(BigInteger.ZERO)
                .apprestrict(BytesUtils.EMPTY_ADDRESS)
                .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                .salt(BytesUtils.toByte32HexString(1))
                .sign("sign")
                .build();
    }

    RequestOrder generateRequestOrder(
            AppOrder appOrder, DatasetOrder datasetOrder, WorkerpoolOrder workerpoolOrder) {
        String requestAddress = generateEthereumAddress();
        return RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .dataset(datasetOrder.getDataset())
                .datasetmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE)
                .requester(requestAddress)
                .volume(BigInteger.ONE)
                .tag(OrderTag.TEE_SCONE.getValue())
                .category(BigInteger.ZERO)
                .trust(BigInteger.ONE)
                .beneficiary(requestAddress)
                .callback(BytesUtils.EMPTY_ADDRESS)
                .params("")
                .salt(BytesUtils.toByte32HexString(1))
                .sign("sign")
                .build();
    }

    BrokerOrder generateBrokerOrder(boolean withDataset) {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        DatasetOrder datasetOrder = generateDatasetOrder(withDataset);
        RequestOrder requestOrder = generateRequestOrder(
                appOrder, datasetOrder, workerpoolOrder);
        return BrokerOrder.builder()
                .appOrder(appOrder)
                .datasetOrder(datasetOrder)
                .workerpoolOrder(workerpoolOrder)
                .requestOrder(requestOrder)
                .build();
    }

    String generateEthereumAddress() {
        try {
            ECKeyPair ecKeypair = Keys.createEcKeyPair();
            return Credentials.create(ecKeypair).getAddress();
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate ethereum address", e);
        }
    }

    //region matchOrders
    @Test
    void shouldNotMatchOrderWhenBrokerOrderIsNull() {
        assertThatThrownBy(() -> brokerService.matchOrders(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Broker order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenAppOrderIsNull() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .requestOrder(RequestOrder.builder().build())
                .workerpoolOrder(WorkerpoolOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("App order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenRequestOrderIsNull() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(AppOrder.builder().build())
                .workerpoolOrder(WorkerpoolOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Request order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenWorkerpoolOrderIsNull() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(AppOrder.builder().build())
                .requestOrder(RequestOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Workerpool order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenAppAddressDoesNotMatch() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .app("")
                .appmaxprice(BigInteger.ZERO)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ZERO)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .workerpoolOrder(workerpoolOrder)
                .requestOrder(requestOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ethereum address is not the same in app order and request order");
    }

    @Test
    void shouldNotMatchOrderWhenWorkerpoolAddressDoesNotMatch() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ZERO)
                .workerpool("")
                .workerpoolmaxprice(BigInteger.ZERO)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .workerpoolOrder(workerpoolOrder)
                .requestOrder(requestOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ethereum address is not the same in workerpool order and request order");
    }

    @Test
    void shouldNotMatchOrderWhenAppPriceIsNull() {
        AppOrder appOrder = AppOrder.builder().app(generateEthereumAddress()).build();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("App price cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenWorkerpoolPriceIsNull() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder().workerpool(generateEthereumAddress()).build();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Workerpool price cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenRequestOrderNeedsDatasetAndDatasetOrderIsNull() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .dataset(generateEthereumAddress())
                .datasetmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Dataset order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenRequestOrderNeedsDatasetAndDatasetAddressDoesNotMatch() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        DatasetOrder datasetOrder = generateDatasetOrder(true);
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .dataset(generateEthereumAddress())
                .datasetmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .datasetOrder(datasetOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ethereum address is not the same in dataset order and request order");
    }

    @Test
    void shouldNotMatchOrderWhenRequestOrderNeedsDatasetAndDatasetPriceIsNull() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        DatasetOrder datasetOrder = DatasetOrder.builder().dataset(generateEthereumAddress()).build();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .dataset(datasetOrder.getDataset())
                .datasetmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .datasetOrder(datasetOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Dataset price cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenPricesNotAccepted() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ZERO)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ZERO)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Incompatible prices");
    }

    @Test
    void shouldNotMatchOrderWhenDepositIsToLow() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        when(iexecHubService.getChainAccount(anyString())).thenReturn(Optional.of(emptyDeposit));
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Deposit too low");
    }

    @Test
    void shouldNotMatchOrderAndReturnEmptyStringWithDataset(CapturedOutput output) {
        BrokerOrder brokerOrder = generateBrokerOrder(true);
        RequestOrder requestOrder = brokerOrder.getRequestOrder();
        when(iexecHubService.getChainAccount(anyString())).thenReturn(Optional.of(deposit));
        assertThat(brokerService.matchOrders(brokerOrder)).isEmpty();
        String expectedMessage = MessageFormat.format("Matching valid orders on-chain [requester:{0}, beneficiary:{1}, pool:{2}, app:{3}, dataset:{4}]",
                requestOrder.getRequester(), requestOrder.getBeneficiary(), requestOrder.getWorkerpool(), requestOrder.getApp(), requestOrder.getDataset());
        assertThat(output.getOut()).contains(expectedMessage);
    }

    @Test
    void shouldNotMatchOrderAndReturnEmptyStringWithoutDataset(CapturedOutput output) {
        BrokerOrder brokerOrder = generateBrokerOrder(false);
        RequestOrder requestOrder = brokerOrder.getRequestOrder();
        when(iexecHubService.getChainAccount(anyString())).thenReturn(Optional.of(deposit));
        assertThat(brokerService.matchOrders(brokerOrder)).isEmpty();
        String expectedMessage = MessageFormat.format("Matching valid orders on-chain [requester:{0}, beneficiary:{1}, pool:{2}, app:{3}]",
                requestOrder.getRequester(), requestOrder.getBeneficiary(), requestOrder.getWorkerpool(), requestOrder.getApp());
        assertThat(output.getOut()).contains(expectedMessage);
    }
    //endregion

    //region fireMatchOrders
    @Test
    void shouldFailToMatchOrdersWithDataset() throws Exception {
        AppOrder appOrder = generateAppOrder();
        DatasetOrder datasetOrder = generateDatasetOrder(true);
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = generateRequestOrder(
                appOrder, datasetOrder, workerpoolOrder);
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any())).thenThrow(IOException.class);
        assertThat(brokerService.fireMatchOrders(appOrder, datasetOrder, workerpoolOrder, requestOrder))
                .isEmpty();
    }

    @Test
    void shouldFailToMatchOrdersWithoutDataset() throws Exception {
        AppOrder appOrder = generateAppOrder();
        DatasetOrder datasetOrder = generateDatasetOrder(false);
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = generateRequestOrder(
                appOrder, datasetOrder, workerpoolOrder);
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any())).thenThrow(IOException.class);
        assertThat(brokerService.fireMatchOrders(appOrder, datasetOrder, workerpoolOrder, requestOrder))
                .isEmpty();
    }

    @Test
    void shouldMatchOrdersWithDataset() throws Exception {
        String dealId = "dealId";
        AppOrder appOrder = generateAppOrder();
        DatasetOrder datasetOrder = generateDatasetOrder(true);
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = generateRequestOrder(
                appOrder, datasetOrder, workerpoolOrder);
        when(signerService.signAndSendTransaction(any(), any(), any(), any(), any())).thenReturn("txHash");
        String workerpoolAddress = Numeric.toHexStringWithPrefixZeroPadded(
                Numeric.toBigInt(workerpoolOrder.getWorkerpool()), 64);
        Log web3Log = new Log();
        web3Log.setData(dealId);
        web3Log.setTopics(List.of(SCHEDULER_NOTICE, workerpoolAddress));
        TransactionReceipt receipt = new TransactionReceipt();
        receipt.setTransactionHash("txHash");
        receipt.setBlockNumber("0x1");
        receipt.setStatus("1");
        receipt.setLogs(List.of(web3Log));
        when(iexecHubService.waitForTxMined(any(SubmittedTx.class))).thenReturn(receipt);
        assertThat(brokerService.fireMatchOrders(appOrder, datasetOrder, workerpoolOrder, requestOrder))
                .isNotEmpty()
                .contains(dealId);
    }
    //endregion

    //region hasRequesterAcceptedPrices
    @Test
    void shouldFailWhenPricesUnderThreshold() {
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ZERO)
                .datasetmaxprice(BigInteger.ZERO)
                .workerpoolmaxprice(BigInteger.ZERO)
                .build();
        assertThat(brokerService.hasRequesterAcceptedPrices(requestOrder, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE, true))
                .isFalse();
    }

    @Test
    void shouldFailForBigAppPrice() {
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ONE)
                .datasetmaxprice(BigInteger.ONE)
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        assertThat(brokerService.hasRequesterAcceptedPrices(requestOrder, BigInteger.TEN, BigInteger.ONE, BigInteger.ONE, true))
                .isFalse();
    }

    @Test
    void shouldFailForBigWorkerpoolPrice() {
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ONE)
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        assertThat(brokerService.hasRequesterAcceptedPrices(requestOrder, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, false))
                .isFalse();
    }

    @Test
    void shouldFailForBigDatasetPrice() {
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ONE)
                .datasetmaxprice(BigInteger.ONE)
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        assertThat(brokerService.hasRequesterAcceptedPrices(requestOrder, BigInteger.ONE, BigInteger.ONE, BigInteger.TEN, true))
                .isFalse();
    }

    @Test
    void shouldSucceedWhenPricesAboveThreshold() {
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ONE)
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        assertThat(brokerService.hasRequesterAcceptedPrices(requestOrder, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO, false))
                .isTrue();
    }
    //endregion

    //region hasRequesterDepositedEnough
    @Test
    void shouldCheckDepositAgainstRequiredPrices() {
        assertThat(brokerService.hasRequesterDepositedEnough(5L, 1L, 1L, 1L)).isTrue();
        assertThat(brokerService.hasRequesterDepositedEnough(0L, 1L, 1L, 1L)).isFalse();
    }
    //endregion

    //region withDataset
    @Test
    void testWithDataset() {
        assertThat(brokerService.withDataset(null)).isFalse();
        assertThat(brokerService.withDataset("")).isFalse();
        assertThat(brokerService.withDataset("0x1")).isTrue();
        assertThat(brokerService.withDataset(BytesUtils.EMPTY_ADDRESS)).isFalse();
    }
    //endregion

}
