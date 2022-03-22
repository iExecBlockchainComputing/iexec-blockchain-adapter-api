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

package com.iexec.blockchain.api;

import com.iexec.blockchain.tool.Status;
import com.iexec.common.chain.ChainDataset;
import com.iexec.common.chain.ChainTask;
import com.iexec.common.chain.adapter.CommandStatus;
import com.iexec.common.chain.adapter.args.TaskContributeArgs;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import com.iexec.common.chain.adapter.args.TaskRevealArgs;
import com.iexec.common.config.PublicChainConfig;
import com.iexec.common.sdk.broker.BrokerOrder;
import feign.Param;
import feign.RequestLine;

/**
 * Interface allowing to instantiate a Feign client targeting Blockchain adapter API REST endpoints.
 * <p>
 * To create the client, call:
 * <pre>FeignBuilder.createBuilder(feignLogLevel)
 *         .target(BlockchainAdapterApiClient.class, blockchainAdapterUrl)</pre>
 * @see com.iexec.common.utils.FeignBuilder
 */
public interface BlockchainAdapterApiClient {

    // region authenticated APIs

    //TODO update endpoint
    @RequestLine("POST /broker/broker/orders/match")
    String matchOrders(BrokerOrder brokerOrder);

    @RequestLine("POST /datasets/requests?name={name}&multiAddress={multiAddress}&checksum={checksum}")
    String createDataset(@Param("name") String name,
                         @Param("multiAddress") String multiAddress,
                         @Param("checksum") String checksum);

    @RequestLine("GET /datasets/requests/{requestId}")
    String getAddressForCreateDatasetRequest(@Param("requestId") String requestId);

    @RequestLine("GET /datasets/requests/{requestId}/status")
    Status getStatusForCreateDatasetRequest(@Param("requestId") String requestId);

    @RequestLine("GET /datasets?address={address}")
    ChainDataset getDatasetByAddress(@Param("address") String address);

    @RequestLine("GET /metrics")
    String getMetrics();

    @RequestLine("GET /tasks/{chainTaskId}")
    ChainTask getTask(@Param("chainTaskId") String chainTaskId);

    @RequestLine("POST /tasks/initialize?chainDealId={chainDealId}&taskIndex={taskIndex}")
    String requestInitializeTask(@Param("chainDealId") String chainDealId,
                                 @Param("taskIndex") int taskIndex);

    @RequestLine("GET /tasks/initialize/{chainTaskId}/status")
    CommandStatus getStatusForInitializeTaskRequest(@Param("chainTaskId") String chainTaskId);

    @RequestLine("POST /tasks/contribute/{chainTaskId}")
    String requestContributeTask(@Param("chainTaskId") String chainTaskId, TaskContributeArgs taskContributeArgs);

    @RequestLine("GET /tasks/contribute/{chainTaskId}/status")
    CommandStatus getStatusForContributeTaskRequest(@Param("chainTaskId") String chainTaskId);

    @RequestLine("POST /tasks/reveal/{chainTaskId}")
    String requestRevealTask(@Param("chainTaskId") String chainTaskId, TaskRevealArgs taskRevealArgs);

    @RequestLine("GET /tasks/reveal/{chainTaskId}/status")
    CommandStatus getStatusForRevealTaskRequest(@Param("chainTaskId") String chainTaskId);

    @RequestLine("POST /tasks/finalize/{chainTaskId}")
    String requestFinalizeTask(@Param("chainTaskId") String chainTaskId, TaskFinalizeArgs taskFinalizeArgs);

    @RequestLine("GET /tasks/finalize/{chainTaskId}/status")
    CommandStatus getStatusForFinalizeTaskRequest(@Param("chainTaskId") String chainTaskId);

    // endregion

    // region unauthenticated APIs

    @RequestLine("GET /config/chain")
    PublicChainConfig getPublicChainConfig();

    //endregion

}
