/*
 * Copyright 2022-2023 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.chain.adapter.CommandStatus;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import com.iexec.common.config.PublicChainConfig;
import com.iexec.common.sdk.broker.BrokerOrder;
import com.iexec.commons.poco.chain.ChainTask;
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
    @RequestLine("POST /broker/orders/match")
    String matchOrders(BrokerOrder brokerOrder);

    @RequestLine("GET /metrics")
    String getMetrics();

    @RequestLine("GET /tasks/{chainTaskId}")
    ChainTask getTask(@Param("chainTaskId") String chainTaskId);

    @RequestLine("POST /tasks/initialize?chainDealId={chainDealId}&taskIndex={taskIndex}")
    String requestInitializeTask(@Param("chainDealId") String chainDealId,
                                 @Param("taskIndex") int taskIndex);

    @RequestLine("GET /tasks/initialize/{chainTaskId}/status")
    CommandStatus getStatusForInitializeTaskRequest(@Param("chainTaskId") String chainTaskId);

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
