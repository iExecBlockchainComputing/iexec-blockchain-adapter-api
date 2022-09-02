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
import com.iexec.common.sdk.order.payload.RequestOrder;
import com.iexec.common.sdk.order.payload.WorkerpoolOrder;
import com.iexec.common.utils.BytesUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrokerServiceTests {

    private IexecHubService iexecHubService;
    private BrokerService brokerService;

    @BeforeEach
    void init() {
        ChainConfig chainConfig = mock(ChainConfig.class);
        iexecHubService = mock(IexecHubService.class);
        when(chainConfig.getBrokerUrl()).thenReturn("localhost");
        brokerService = new BrokerService(chainConfig, iexecHubService);
    }

    //region matchOrders
    @Test
    void shouldNotMatchOrderWhenBrokerOrderIsNull() {
        assertThatThrownBy(() -> brokerService.matchOrders(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Broker order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenAppOrderIsNull() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .requestOrder(RequestOrder.builder().build())
                .workerpoolOrder(WorkerpoolOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("App order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenRequestOrderIsNull() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(AppOrder.builder().build())
                .workerpoolOrder(WorkerpoolOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenWorkerpoolOrderIsNull() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(AppOrder.builder().build())
                .requestOrder(RequestOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workerpool order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenRequestOrderNeedsDatasetAndDatasetOrderIsNull() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(AppOrder.builder().build())
                .requestOrder(RequestOrder.builder().dataset("0x1").datasetmaxprice(BigInteger.ONE).build())
                .workerpoolOrder(WorkerpoolOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dataset order cannot be null");
    }

    @Test
    void shouldNotMatchOrderWhenPricesAreNotDefined() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(AppOrder.builder().build())
                .requestOrder(RequestOrder.builder().build())
                .workerpoolOrder(WorkerpoolOrder.builder().build())
                .build();
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Incompatible prices");
    }

    @Test
    void shouldFailToMatchOrderWhenDepositIsToLow() {
        AppOrder appOrder = AppOrder.builder()
                .price(BigInteger.ONE)
                .build();
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ONE)
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder()
                .price(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        ChainAccount chainAccount = ChainAccount.builder()
                .deposit(0L)
                .build();
        when(iexecHubService.getChainAccount(anyString())).thenReturn(Optional.of(chainAccount));
        assertThatThrownBy(() -> brokerService.matchOrders(brokerOrder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Deposit too low");
    }

    @Test
    void shouldNotMatchOrderAndReturnEmptyString() {
        AppOrder appOrder = AppOrder.builder()
                .price(BigInteger.ONE)
                .build();
        RequestOrder requestOrder = RequestOrder.builder()
                .appmaxprice(BigInteger.ONE)
                .workerpoolmaxprice(BigInteger.ONE)
                .build();
        WorkerpoolOrder workerpoolOrder = WorkerpoolOrder.builder()
                .price(BigInteger.ONE)
                .build();
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(appOrder)
                .requestOrder(requestOrder)
                .workerpoolOrder(workerpoolOrder)
                .build();
        ChainAccount chainAccount = ChainAccount.builder()
                .deposit(3L)
                .build();
        when(iexecHubService.getChainAccount(anyString())).thenReturn(Optional.of(chainAccount));
        assertThat(brokerService.matchOrders(brokerOrder)).isEmpty();
    }
    //endregion

    //region fireMatchOrders
    @Test
    void shouldFailToMatchOrders() {
        BrokerOrder brokerOrder = BrokerOrder.builder()
                .appOrder(AppOrder.builder().build())
                .requestOrder(RequestOrder.builder().build())
                .workerpoolOrder(WorkerpoolOrder.builder().build())
                .build();
        assertThat(brokerService.fireMatchOrders(brokerOrder))
                .isEmpty();
    }
    //endregion

    //region hasRequesterAcceptedPrices
    @Test
    void shouldFailForEmptyRequestOrder() {
        RequestOrder requestOrder = RequestOrder.builder().build();
        assertThat(brokerService.hasRequesterAcceptedPrices(requestOrder, null, null, null, false))
                .isFalse();
    }

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
    void shouldNotAcceptDepositWhenRequestOrderIsNull() {
        RequestOrder requestOrder = RequestOrder.builder().build();
        assertThat(brokerService.hasRequesterDepositedEnough(null, 0L, true))
                .isFalse();
        assertThat(brokerService.hasRequesterDepositedEnough(requestOrder, 0L, false))
                .isFalse();
    }

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
