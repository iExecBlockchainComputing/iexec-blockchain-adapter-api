package com.iexec.blockchain;

import com.iexec.common.chain.adapter.args.TaskContributeArgs;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import com.iexec.common.chain.adapter.args.TaskRevealArgs;
import feign.Param;
import feign.RequestLine;

interface BlockchainAdapterApiClient {

    @RequestLine("POST /tasks/initialize?chainDealId={chainDealId}&taskIndex={taskIndex}")
    String requestInitializeTask(@Param("chainDealId") String chainDealId,
                                 @Param("taskIndex") int taskIndex);

    @RequestLine("POST /tasks/contribute/{chainTaskId}")
    String requestContributeTask(@Param("chainTaskId") String chainTaskId, TaskContributeArgs taskContributeArgs);

    @RequestLine("POST /tasks/reveal/{chainTaskId}")
    String requestRevealTask(@Param("chainTaskId") String chainTaskId, TaskRevealArgs taskRevealArgs);

    @RequestLine("POST /tasks/finalize/{chainTaskId}")
    String requestFinalizeTask(@Param("chainTaskId") String chainTaskId, TaskFinalizeArgs taskFinalizeArgs);

}