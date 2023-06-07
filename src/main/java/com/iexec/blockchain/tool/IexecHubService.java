/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.tool;

import com.iexec.common.utils.EthAddress;
import com.iexec.common.worker.result.ResultUtils;
import com.iexec.commons.poco.chain.*;
import com.iexec.commons.poco.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static com.iexec.commons.poco.utils.BytesUtils.stringToBytes;

@Service
public class IexecHubService extends IexecHubAbstractService {

    public IexecHubService(CredentialsService credentialsService,
                           Web3jService web3jService,
                           ChainConfig chainConfig) {
        super(
                credentialsService.getCredentials(),
                web3jService,
                chainConfig.getHubAddress()
        );
    }

    public static boolean isSignature(String hexString) {
        return !StringUtils.isEmpty(hexString) &&
                BytesUtils.stringToBytes(hexString).length == 65; // 32 + 32 + 1
    }

    public static boolean isByte32(String hexString) {
        return !StringUtils.isEmpty(hexString) &&
                BytesUtils.stringToBytes(hexString).length == 32;
    }

    public static boolean isAddress(String hexString) {
        return EthAddress.validate(hexString);
    }

    public TransactionReceipt initializeTask(String chainDealId,
                                             int taskIndex) throws Exception {
        return iexecHubContract
                .initialize(
                        stringToBytes(chainDealId),
                        BigInteger.valueOf(taskIndex))
                .send();
    }

    public TransactionReceipt contribute(String chainTaskId,
                                         String resultDigest,
                                         String workerpoolSignature,
                                         String enclaveChallenge,
                                         String enclaveSignature) throws Exception {
        String resultHash = ResultUtils.computeResultHash(chainTaskId, resultDigest);
        String resultSeal =
                ResultUtils.computeResultSeal(credentials.getAddress(),
                        chainTaskId,
                        resultDigest);

        return iexecHubContract
                .contribute(
                        stringToBytes(chainTaskId),
                        stringToBytes(resultHash),
                        stringToBytes(resultSeal),
                        enclaveChallenge,
                        stringToBytes(enclaveSignature),
                        stringToBytes(workerpoolSignature))
                .send();
    }


    public TransactionReceipt reveal(String chainTaskId,
                                     String resultDigest) throws Exception {
        return iexecHubContract
                .reveal(
                        stringToBytes(chainTaskId),
                        stringToBytes(resultDigest))
                .send();
    }

    public TransactionReceipt finalizeTask(String chainTaskId,
                                       String resultLink,
                                       String callbackData) throws Exception {
        byte[] results = StringUtils.isNotEmpty(resultLink) ?
                resultLink.getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] resultsCallback = StringUtils.isNotEmpty(callbackData) ?
                stringToBytes(callbackData) : new byte[0];

        return iexecHubContract
                .finalize(
                        stringToBytes(chainTaskId),
                        results,
                        resultsCallback)
                .send();
    }

    public boolean hasEnoughGas() {
        return hasEnoughGas(credentials.getAddress());
    }

    /**
     * Check if the task is defined onchain and
     * has the status {@link ChainTaskStatus#UNSET}.
     *
     * @param chainTaskId blockchain ID of the task
     * @return true if the task is found with the status UNSET, false otherwise.
     */
    public boolean isTaskInUnsetStatusOnChain(String chainTaskId) {
        final Optional<ChainTask> chainTask = getChainTask(chainTaskId);
        return chainTask.isEmpty()
                || ChainTaskStatus.UNSET.equals(chainTask.get().getStatus());
    }

    /**
     * Check if a deal's contribution deadline
     * is still not reached.
     *
     * @param chainDealId blockchain ID of the deal
     * @return true if deadline is not reached, false otherwise.
     */
    public boolean isBeforeContributionDeadline(String chainDealId) {
        return getChainDeal(chainDealId)
                .map(this::isBeforeContributionDeadline)
                .orElse(false);
    }

    /**
     * Check if a deal's contribution deadline
     * is still not reached.
     *
     * @param chainDeal blockchain ID of the deal
     * @return true if deadline is not reached, false otherwise.
     */
    private boolean isBeforeContributionDeadline(ChainDeal chainDeal) {
        return getContributionDeadline(chainDeal)
                .after(new Date());
    }

    /**
     * <p> Get deal's contribution deadline date. The deadline
     * is calculated as follow:
     * start + maxCategoryTime * maxNbOfPeriods.
     *
     * <ul>
     * <li> start: the start time of the deal.
     * <li> maxCategoryTime: duration of the deal's category.
     * <li> nbOfCategoryUnits: number of category units dedicated
     *      for the contribution phase.
     *
     * @param chainDeal blockchain ID of the deal
     * @return contribution deadline
     */
    public Date getContributionDeadline(ChainDeal chainDeal) {
        long startTime = chainDeal.getStartTime().longValue() * 1000;
        long maxTime = chainDeal.getChainCategory().getMaxExecutionTime();
        long maxNbOfPeriods = getMaxNbOfPeriodsForConsensus();
        maxNbOfPeriods = (maxNbOfPeriods == -1) ? 10 : maxNbOfPeriods;
        return new Date(startTime + maxTime * maxNbOfPeriods);
    }

    public boolean hasEnoughStakeToContribute(String chainDealId, String workerWallet) {
        Optional<ChainAccount> optionalChainAccount = getChainAccount(workerWallet);
        Optional<ChainDeal> optionalChainDeal = getChainDeal(chainDealId);
        if (optionalChainAccount.isEmpty() || optionalChainDeal.isEmpty()) {
            return false;
        }
        return optionalChainAccount.get().getDeposit() >= optionalChainDeal.get().getWorkerStake().longValue();
    }

    public boolean isContributionUnsetToContribute(String chainTaskId, String workerWallet) {
        Optional<ChainContribution> optionalContribution =
                getChainContribution(chainTaskId, workerWallet);
        if (optionalContribution.isEmpty()) return false;

        ChainContribution chainContribution = optionalContribution.get();
        return chainContribution.getStatus().equals(ChainContributionStatus.UNSET);
    }

}
