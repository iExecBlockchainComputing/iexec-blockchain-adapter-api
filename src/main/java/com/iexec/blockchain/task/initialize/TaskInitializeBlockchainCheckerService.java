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

package com.iexec.blockchain.task.initialize;


import com.iexec.blockchain.tool.IexecHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskInitializeBlockchainCheckerService {

    private final IexecHubService iexecHubService;

    public TaskInitializeBlockchainCheckerService(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    /**
     * Check if task can be initialized.
     *
     * @param chainDealId blockchain ID of the deal
     * @param taskIndex   index of the task int the bag
     * @param chainTaskId blockchain ID of the task
     * @return true if task can be initialized on-chain
     */
    public boolean canInitializeTask(String chainDealId, int taskIndex, String chainTaskId) {
        boolean hasEnoughGas =
                iexecHubService.hasEnoughGas();
        boolean isTaskUnsetOnChain =
                iexecHubService.isTaskInUnsetStatusOnChain(chainTaskId);
        boolean isBeforeContributionDeadline =
                iexecHubService.isBeforeContributionDeadline(chainDealId);

        if (!hasEnoughGas || !isTaskUnsetOnChain || !isBeforeContributionDeadline) {
            log.error("Cannot initialize task [chainDealId:{}, taskIndex:{}, chainTaskId:{}, " +
                            "hasEnoughGas:{}, isTaskUnsetOnChain:{}, isBeforeContributionDeadline:{}]",
                    chainDealId, taskIndex, chainTaskId,
                    hasEnoughGas, isTaskUnsetOnChain, isBeforeContributionDeadline);
            //TODO eventually return cause
            return false;
        }
        return true;
    }

}
