/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

import com.iexec.blockchain.tool.ChainConfig;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.ChainAccount;
import com.iexec.common.sdk.broker.BrokerOrder;
import com.iexec.common.sdk.order.payload.AppOrder;
import com.iexec.common.sdk.order.payload.DatasetOrder;
import com.iexec.common.sdk.order.payload.RequestOrder;
import com.iexec.common.sdk.order.payload.WorkerpoolOrder;
import com.iexec.common.utils.BytesUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class BrokerServiceTests {

    private IexecHubService iexecHubService;
    private BrokerService brokerService;

    private static final ChainAccount deposit = ChainAccount.builder().deposit(100L).build();
    private static final ChainAccount emptyDeposit = ChainAccount.builder().deposit(0L).build();

    @BeforeEach
    void init() {
        ChainConfig chainConfig = mock(ChainConfig.class);
        iexecHubService = mock(IexecHubService.class);
        when(chainConfig.getBrokerUrl()).thenReturn("http://localhost");
        brokerService = new BrokerService(chainConfig, iexecHubService);
    }

    AppOrder generateAppOrder() {
        return AppOrder.builder()
                .app(generateEthereumAddress())
                .price(BigInteger.ONE)
                .build();
    }

    DatasetOrder generateDatasetOrder() {
        return DatasetOrder.builder()
                .dataset(generateEthereumAddress())
                .price(BigInteger.ONE)
                .build();
    }

    WorkerpoolOrder generateWorkerpoolOrder() {
        return WorkerpoolOrder.builder()
                .workerpool(generateEthereumAddress())
                .price(BigInteger.ONE)
                .build();
    }

    BrokerOrder generateBrokerOrder(boolean withDataset) {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder.RequestOrderBuilder requestOrderBuilder = RequestOrder.builder()
                .app(appOrder.getApp())
                .appmaxprice(BigInteger.ONE)
                .workerpool(workerpoolOrder.getWorkerpool())
                .workerpoolmaxprice(BigInteger.ONE);
        BrokerOrder.BrokerOrderBuilder brokerOrderBuilder = BrokerOrder.builder()
                .appOrder(appOrder)
                .workerpoolOrder(workerpoolOrder);
        if (withDataset) {
            DatasetOrder datasetOrder = generateDatasetOrder();
            requestOrderBuilder
                    .dataset(datasetOrder.getDataset())
                    .datasetmaxprice(BigInteger.ONE);
            brokerOrderBuilder.datasetOrder(datasetOrder);
        }
        return brokerOrderBuilder.requestOrder(requestOrderBuilder.build())
                .build();
    }

    String generateEthereumAddress() {
        try {
            ECKeyPair ecKeypair = Keys.createEcKeyPair();
            return Credentials.create(ecKeypair).getAddress();
        } catch(Exception e) {
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
                .hasMessage("Worker pool order cannot be null");
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
                .hasMessage("App address is not the same in order and request order");
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
                .hasMessage("Worker pool address is not the same in order and request order");
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
                .hasMessage("Worker pool price cannot be null");
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
        DatasetOrder datasetOrder = generateDatasetOrder();
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
                .hasMessage("Dataset address is not the same in order and request order");
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
    void shouldFailToMatchOrdersWithDataset() {
        AppOrder appOrder = generateAppOrder();
        DatasetOrder datasetOrder = generateDatasetOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .requester(generateEthereumAddress())
                .beneficiary(generateEthereumAddress())
                .app(appOrder.getApp())
                .dataset(datasetOrder.getDataset())
                .workerpool(workerpoolOrder.getWorkerpool())
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .datasetOrder(datasetOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        assertThat(brokerService.fireMatchOrders(brokerOrder))
                .isEmpty();
    }

    @Test
    void shouldFailToMatchOrdersWithoutDataset() {
        AppOrder appOrder = generateAppOrder();
        WorkerpoolOrder workerpoolOrder = generateWorkerpoolOrder();
        RequestOrder requestOrder = RequestOrder.builder()
                .requester(generateEthereumAddress())
                .beneficiary(generateEthereumAddress())
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
        assertThat(brokerService.fireMatchOrders(brokerOrder))
                .isEmpty();
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
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ONE)
                .datasetmaxprice(BigInteger.ONE)
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        assertThat(brokerService.hasRequesterDepositedEnough(requestOrder, 5L, true)).isTrue();
        assertThat(brokerService.hasRequesterDepositedEnough(requestOrder, 0L, true)).isFalse();
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
