/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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
import com.iexec.common.sdk.cli.FillOrdersCliOutput;
import com.iexec.common.sdk.order.payload.AppOrder;
import com.iexec.common.sdk.order.payload.DatasetOrder;
import com.iexec.common.sdk.order.payload.RequestOrder;
import com.iexec.common.sdk.order.payload.WorkerpoolOrder;
import com.iexec.common.utils.BytesUtils;
import com.iexec.common.utils.FeignBuilder;
import feign.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class BrokerService {

    private final BrokerClient brokerClient;
    private final IexecHubService iexecHubService;


    public BrokerService(ChainConfig chainConfig, IexecHubService iexecHubService) {
        //TODO Assert broker is up
        this.iexecHubService = iexecHubService;
        this.brokerClient = FeignBuilder.createBuilder(Logger.Level.BASIC)
                .target(BrokerClient.class, chainConfig.getBrokerUrl());
    }

    void checkAssetOrder(String requestOrderAddress, String assetAddress, BigInteger assetPrice, String assetType) {
        Objects.requireNonNull(requestOrderAddress,  assetType + " address cannot be null in request order");
        Objects.requireNonNull(assetAddress, assetType + " address cannot be null");
        Objects.requireNonNull(assetPrice, assetType + " price cannot be null");
        if (!requestOrderAddress.equalsIgnoreCase(assetAddress)) {
            throw new IllegalStateException(assetType + " address is not the same in order and request order");
        }
    }

    void checkRequestOrder(RequestOrder requestOrder) {
        Objects.requireNonNull(requestOrder, "Request order cannot be null");
        Objects.requireNonNull(requestOrder.getAppmaxprice(), "Requester application max price cannot be null");
        Objects.requireNonNull(requestOrder.getWorkerpoolmaxprice(), "Requester worker pool max price cannot be null");
        if (withDataset(requestOrder.getDataset())) {
            Objects.requireNonNull(requestOrder.getDatasetmaxprice(), "Requester dataset max price cannot be null");
        }
    }

    void checkBrokerOrder(BrokerOrder brokerOrder) {
        Objects.requireNonNull(brokerOrder, "Broker order cannot be null");
        AppOrder appOrder = brokerOrder.getAppOrder();
        DatasetOrder datasetOrder = brokerOrder.getDatasetOrder();
        RequestOrder requestOrder = brokerOrder.getRequestOrder();
        WorkerpoolOrder workerpoolOrder = brokerOrder.getWorkerpoolOrder();
        Objects.requireNonNull(appOrder, "App order cannot be null");
        Objects.requireNonNull(workerpoolOrder, "Worker pool order cannot be null");

        checkRequestOrder(requestOrder);
        checkAssetOrder(requestOrder.getApp(), appOrder.getApp(), appOrder.getAppprice(), "App");
        checkAssetOrder(requestOrder.getWorkerpool(), workerpoolOrder.getWorkerpool(), workerpoolOrder.getWorkerpoolprice(),"Worker pool");
        if (withDataset(requestOrder.getDataset())) {
            Objects.requireNonNull(datasetOrder, "Dataset order cannot be null");
            checkAssetOrder(requestOrder.getDataset(), datasetOrder.getDataset(), datasetOrder.getDatasetprice(), "Dataset");
        }
    }

    // TODO return status to controller and API requester, errors are only written in logs
    public String matchOrders(BrokerOrder brokerOrder) {
        checkBrokerOrder(brokerOrder);
        AppOrder appOrder = brokerOrder.getAppOrder();
        WorkerpoolOrder workerpoolOrder = brokerOrder.getWorkerpoolOrder();
        DatasetOrder datasetOrder = brokerOrder.getDatasetOrder();
        RequestOrder requestOrder = brokerOrder.getRequestOrder();
        final boolean withDataset = withDataset(requestOrder.getDataset());
        BigInteger datasetPrice = withDataset ? datasetOrder.getDatasetprice() : BigInteger.ZERO;
        if (!hasRequesterAcceptedPrices(brokerOrder.getRequestOrder(),
                appOrder.getAppprice(),
                workerpoolOrder.getWorkerpoolprice(),
                datasetPrice,
                withDataset)) {
            throw new IllegalStateException("Incompatible prices");
        }
        //TODO check workerpool stake
        long deposit = iexecHubService.getChainAccount(requestOrder.getRequester())
                .map(ChainAccount::getDeposit)
                .orElse(-1L);
        if (!hasRequesterDepositedEnough(brokerOrder.getRequestOrder(), deposit, withDataset)) {
            throw new IllegalStateException("Deposit too low");
        }
        String beneficiary = brokerOrder.getRequestOrder().getBeneficiary();
        String messageDetails = MessageFormat.format("requester:{0}, beneficiary:{1}, pool:{2}, app:{3}",
                requestOrder.getRequester(), beneficiary, workerpoolOrder.getWorkerpool(), appOrder.getApp());
        if (withDataset) {
            messageDetails += ", dataset:" + datasetOrder.getDataset();
        }
        log.info("Matching valid orders on-chain [{}]", messageDetails);
        return fireMatchOrders(brokerOrder).orElse("");
    }

    Optional<String> fireMatchOrders(BrokerOrder brokerOrder) {
        try {
            FillOrdersCliOutput dealResponse = brokerClient.matchOrders(brokerOrder);
            log.info("Matched orders [chainDealId:{}, tx:{}]", dealResponse.getDealid(), dealResponse.getTxHash());
            return Optional.of(dealResponse.getDealid());
        } catch (Exception e) {
            log.error("Failed to request match order [requester:{}, app:{}, workerpool:{}, dataset:{}]",
                    brokerOrder.getRequestOrder().getRequester(),
                    brokerOrder.getRequestOrder().getApp(),
                    brokerOrder.getRequestOrder().getWorkerpool(),
                    brokerOrder.getRequestOrder().getDataset(), e);
        }
        return Optional.empty();
    }

    boolean hasRequesterAcceptedPrices(
            RequestOrder requestOrder,
            BigInteger appPrice,
            BigInteger workerpoolPrice,
            BigInteger datasetPrice,
            boolean withDataset
    ) {
        boolean isAppPriceAccepted = requestOrder.getAppmaxprice().longValue() >= appPrice.longValue();
        boolean isPoolPriceAccepted = requestOrder.getWorkerpoolmaxprice().longValue() >= workerpoolPrice.longValue();
        boolean isAccepted = isAppPriceAccepted && isPoolPriceAccepted;
        String messageDetails = MessageFormat.format("[isAppPriceAccepted:{0}, isPoolPriceAccepted:{1}]",
                isAppPriceAccepted, isPoolPriceAccepted);
        if (withDataset) {
            boolean isDatasetPriceAccepted = requestOrder.getDatasetmaxprice().longValue() >= datasetPrice.longValue();
            isAccepted = isAccepted && isDatasetPriceAccepted;
            messageDetails = MessageFormat.format("[isAppPriceAccepted:{0}, isPoolPriceAccepted:{1}, isDatasetPriceAccepted:{2}]",
                    isAppPriceAccepted, isPoolPriceAccepted, isDatasetPriceAccepted
            );
        }
        if (!isAccepted) {
            log.error("Prices not accepted (too expensive) {}", messageDetails);
        }
        return isAccepted;
    }

    boolean hasRequesterDepositedEnough(RequestOrder requestOrder, long deposit, boolean withDataset) {
        BigInteger price = requestOrder.getWorkerpoolmaxprice().add(requestOrder.getAppmaxprice());
        if (withDataset) {
            price = price.add(requestOrder.getDatasetmaxprice());
        }
        if (price.longValue() > deposit) {
            log.error("Deposit too low [price:{}, deposit:{}]", price, deposit);
            return false;
        }
        return true;
    }

    /**
     * Checks if a dataset is part of the order.
     * A valid request requires a dataset address which is not empty and not the empty address.
     *
     * @param datasetAddress Dataset address to check
     * @return true if a dataset is part of the request, false otherwise.
     */
     boolean withDataset(String datasetAddress) {
        return StringUtils.isNotEmpty(datasetAddress) && !datasetAddress.equals(BytesUtils.EMPTY_ADDRESS);
    }

}
