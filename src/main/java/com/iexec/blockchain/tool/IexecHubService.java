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

package com.iexec.blockchain.tool;

import com.iexec.commons.poco.chain.*;
import com.iexec.commons.poco.encoding.PoCoDataEncoder;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.HashUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.iexec.commons.poco.utils.BytesUtils.stringToBytes;

@Service
public class IexecHubService extends IexecHubAbstractService {

    private final ChainConfig chainConfig;
    private final SignerService signerService;
    private final Web3jService web3jService;

    public IexecHubService(SignerService signerService,
                           Web3jService web3jService,
                           ChainConfig chainConfig) {
        super(
                signerService.getCredentials(),
                web3jService,
                chainConfig.getHubAddress()
        );
        this.chainConfig = chainConfig;
        this.signerService = signerService;
        this.web3jService = web3jService;
    }

    public static boolean isByte32(String hexString) {
        return !StringUtils.isEmpty(hexString) &&
                BytesUtils.stringToBytes(hexString).length == 32;
    }

    public TransactionReceipt initializeTask(String chainDealId,
                                             int taskIndex) throws IOException {
        final String txData = PoCoDataEncoder.encodeInitialize(chainDealId, taskIndex);
        final String txHash = submit(txData);
        return waitTxMined(txHash);
    }

    public TransactionReceipt contribute(String chainTaskId,
                                         String resultDigest,
                                         String workerpoolSignature,
                                         String enclaveChallenge,
                                         String enclaveSignature) throws IOException {
        final String resultHash = HashUtils.concatenateAndHash(chainTaskId, resultDigest);
        final String resultSeal = HashUtils.concatenateAndHash(credentials.getAddress(), chainTaskId, resultDigest);
        final String txData = PoCoDataEncoder.encodeContribute(
                chainTaskId, resultHash, resultSeal, enclaveChallenge, enclaveSignature, workerpoolSignature);
        final String txHash = submit(txData);
        return waitTxMined(txHash);
    }


    public TransactionReceipt reveal(String chainTaskId,
                                     String resultDigest) throws IOException {
        final String txData = PoCoDataEncoder.encodeReveal(chainTaskId, resultDigest);
        final String txHash = submit(txData);
        return waitTxMined(txHash);
    }

    public TransactionReceipt finalizeTask(String chainTaskId,
                                           String resultLink,
                                           String callbackData) throws IOException {
        final byte[] results = StringUtils.isNotEmpty(resultLink) ?
                resultLink.getBytes(StandardCharsets.UTF_8) : new byte[0];
        final byte[] resultsCallback = StringUtils.isNotEmpty(callbackData) ?
                stringToBytes(callbackData) : new byte[0];
        final String txData = PoCoDataEncoder.encodeFinalize(chainTaskId, results, resultsCallback);
        final String txHash = submit(txData);
        return waitTxMined(txHash);
    }

    /**
     * Synchronized sleep to ensure several transactions will never be sent in the same time interval.
     * <p>
     * This synchronized sleep is required for nonce computation on pending block.
     * When a first transaction will be emitted, it will be emitted and registered on the pending block.
     * After a latency, a second transaction can be sent on the pending block and the nonce will be computed successfully.
     * With a correct nonce, it becomes possible to perform several transactions from the same wallet in the same block.
     */
    private synchronized String submit(String txData) throws IOException {
        final BigInteger nonce = signerService.getNonce();
        return signerService.signAndSendTransaction(
                nonce, web3jService.getUserGasPrice(), chainConfig.getHubAddress(), txData);
    }

    private TransactionReceipt waitTxMined(final String txHash) {
        int attempt = 0;
        try {
            while (web3jService.getTransactionReceipt(txHash) == null && attempt < 3 * chainConfig.getBlockTime()) {
                TimeUnit.SECONDS.sleep(1L);
                attempt++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return web3jService.getTransactionReceipt(txHash);
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

}
