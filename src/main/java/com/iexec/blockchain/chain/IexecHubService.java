/*
 * Copyright 2020-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.chain;

import com.iexec.common.worker.result.ResultUtils;
import com.iexec.commons.poco.chain.*;
import com.iexec.commons.poco.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.iexec.commons.poco.utils.BytesUtils.stringToBytes;

@Service
public class IexecHubService extends IexecHubAbstractService {

    public IexecHubService(final SignerService signerService,
                           final Web3jService web3jService,
                           final ChainConfig chainConfig) {
        super(
                signerService.getCredentials(),
                web3jService,
                chainConfig.getHubAddress()
        );
    }

    public static boolean isByte32(final String hexString) {
        return !StringUtils.isEmpty(hexString) &&
                BytesUtils.stringToBytes(hexString).length == 32;
    }

    public TransactionReceipt initializeTask(final String chainDealId,
                                             final int taskIndex) throws Exception {
        addLatency();
        return iexecHubContract
                .initialize(
                        stringToBytes(chainDealId),
                        BigInteger.valueOf(taskIndex))
                .send();
    }

    public TransactionReceipt contribute(final String chainTaskId,
                                         final String resultDigest,
                                         final String workerpoolSignature,
                                         final String enclaveChallenge,
                                         final String enclaveSignature) throws Exception {
        final String resultHash = ResultUtils.computeResultHash(chainTaskId, resultDigest);
        final String resultSeal =
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


    public TransactionReceipt reveal(final String chainTaskId,
                                     final String resultDigest) throws Exception {
        return iexecHubContract
                .reveal(
                        stringToBytes(chainTaskId),
                        stringToBytes(resultDigest))
                .send();
    }

    public TransactionReceipt finalizeTask(final String chainTaskId,
                                           final String resultLink,
                                           final String callbackData) throws Exception {
        addLatency();
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

    /**
     * Synchronized sleep to ensure several transactions will never be sent in the same time interval.
     * <p>
     * This synchronized sleep is required for nonce computation on pending block.
     * When a first transaction will be emitted, it will be emitted and registered on the pending block.
     * After a latency, a second transaction can be sent on the pending block and the nonce will be computed successfully.
     * With a correct nonce, it becomes possible to perform several transactions from the same wallet in the same block.
     *
     * @throws InterruptedException if the calling thread is interrupted
     */
    private synchronized void addLatency() throws InterruptedException {
        final long deadline = Instant.now().plus(1L, ChronoUnit.SECONDS).toEpochMilli();
        while (Instant.now().toEpochMilli() < deadline) {
            wait(deadline - Instant.now().toEpochMilli());
        }
    }

    public boolean hasEnoughGas() {
        return hasEnoughGas(credentials.getAddress());
    }

    /**
     * Check if the task is defined on-chain and has the {@link ChainTaskStatus#UNSET} status.
     *
     * @param chainTaskId blockchain ID of the task
     * @return true if the task is found with the status UNSET, false otherwise.
     */
    public boolean isTaskInUnsetStatusOnChain(final String chainTaskId) {
        final ChainTask chainTask = getChainTask(chainTaskId).orElse(null);
        return chainTask == null || chainTask.getStatus() == ChainTaskStatus.UNSET;
    }

    /**
     * Check if a deal's contribution deadline
     * is still not reached.
     *
     * @param chainDealId blockchain ID of the deal
     * @return true if deadline is not reached, false otherwise.
     */
    public boolean isBeforeContributionDeadline(final String chainDealId) {
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
    private boolean isBeforeContributionDeadline(final ChainDeal chainDeal) {
        return getContributionDeadline(chainDeal)
                .after(new Date());
    }

    /**
     * Get deal's contribution deadline date.
     * <p>
     * The deadline is calculated as follows:
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
    private Date getContributionDeadline(final ChainDeal chainDeal) {
        long startTime = chainDeal.getStartTime().longValue() * 1000;
        long maxTime = chainDeal.getChainCategory().getMaxExecutionTime();
        long maxNbOfPeriods = getMaxNbOfPeriodsForConsensus();
        maxNbOfPeriods = (maxNbOfPeriods == -1) ? 10 : maxNbOfPeriods;
        return new Date(startTime + maxTime * maxNbOfPeriods);
    }

}
