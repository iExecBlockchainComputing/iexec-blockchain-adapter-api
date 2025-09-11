/*
 * Copyright 2020-2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.blockchain.command.generic.SubmittedTx;
import com.iexec.commons.poco.chain.*;
import com.iexec.commons.poco.encoding.PoCoDataEncoder;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.commons.poco.utils.HashUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.iexec.commons.poco.utils.BytesUtils.stringToBytes;

@Slf4j
@Service
public class IexecHubService extends IexecHubAbstractService {

    private final ChainConfig chainConfig;
    private final SignerService signerService;
    private final Web3jService web3jService;
    private final Counter failureCounter = Metrics.counter("iexec.poco.transaction", "status", "failure");
    private final Counter successCounter = Metrics.counter("iexec.poco.transaction", "status", "success");

    public IexecHubService(final SignerService signerService,
                           final Web3jService web3jService,
                           final ChainConfig chainConfig) {
        super(
                signerService.getCredentials(),
                web3jService,
                chainConfig.getHubAddress()
        );
        this.chainConfig = chainConfig;
        this.signerService = signerService;
        this.web3jService = web3jService;
    }

    public static boolean isByte32(final String hexString) {
        return !StringUtils.isEmpty(hexString) &&
                BytesUtils.stringToBytes(hexString).length == 32;
    }

    public TransactionReceipt initializeTask(final String chainDealId,
                                             final int taskIndex) throws IOException, TransactionException {
        final String txData = PoCoDataEncoder.encodeInitialize(chainDealId, taskIndex);
        final SubmittedTx submittedTx = submit("initialize", txData);
        return waitForTxMined(submittedTx);
    }

    public TransactionReceipt contribute(final String chainTaskId,
                                         final String resultDigest,
                                         final String workerpoolSignature,
                                         final String enclaveChallenge,
                                         final String enclaveSignature) throws IOException, TransactionException {
        final String resultHash = HashUtils.concatenateAndHash(chainTaskId, resultDigest);
        final String resultSeal = HashUtils.concatenateAndHash(credentials.getAddress(), chainTaskId, resultDigest);

        final String txData = PoCoDataEncoder.encodeContribute(
                chainTaskId, resultHash, resultSeal, enclaveChallenge, enclaveSignature, workerpoolSignature);
        final SubmittedTx submittedTx = submit("contribute", txData);
        return waitForTxMined(submittedTx);
    }


    public TransactionReceipt reveal(final String chainTaskId,
                                     final String resultDigest) throws IOException, TransactionException {
        final String txData = PoCoDataEncoder.encodeReveal(chainTaskId, resultDigest);
        final SubmittedTx submittedTx = submit("reveal", txData);
        return waitForTxMined(submittedTx);
    }

    public TransactionReceipt finalizeTask(final String chainTaskId,
                                           final String resultLink,
                                           final String callbackData) throws IOException, TransactionException {
        final byte[] results = StringUtils.isNotEmpty(resultLink) ?
                resultLink.getBytes(StandardCharsets.UTF_8) : new byte[0];
        final byte[] resultsCallback = StringUtils.isNotEmpty(callbackData) ?
                stringToBytes(callbackData) : new byte[0];

        final String txData = PoCoDataEncoder.encodeFinalize(chainTaskId, results, resultsCallback);
        final SubmittedTx submittedTx = submit("finalize", txData);
        return waitForTxMined(submittedTx);
    }

    /**
     * Submits the transaction to the blockchain network mem-pool.
     * <p>
     * This method can be {@code synchronized} as there is only a single {@code IexecHubService} instance.
     * When a first transaction will be emitted, it will be emitted and registered on the pending block.
     * After a latency, a second transaction can be sent on the pending block and the nonce will be computed successfully.
     * With a correct nonce, it becomes possible to perform several transactions from the same wallet in the same block.
     */
    private synchronized SubmittedTx submit(final String function, final String txData) throws IOException {
        final BigInteger nonce = signerService.getNonce();
        final BigInteger gasLimit = switch (function) {
            case "initialize" -> signerService.estimateGas(chainConfig.getHubAddress(), txData);
            case "finalize" -> signerService.estimateGas(chainConfig.getHubAddress(), txData).add(getCallbackGas());
            default -> PoCoDataEncoder.getGasLimitForFunction(function);
        };
        final String txHash = signerService.signAndSendTransaction(
                nonce, web3jService.getUserGasPrice(), gasLimit, chainConfig.getHubAddress(), txData);
        log.info("Transaction submitted [nonce:{}, hash:{}]", nonce, txHash);
        return new SubmittedTx(nonce, gasLimit, txData, txHash);
    }

    private TransactionReceipt waitForTxMined(final SubmittedTx submittedTx) throws IOException, TransactionException {
        final TransactionReceipt receipt = txReceiptProcessor.waitForTransactionReceipt(submittedTx.hash());
        log.info("Transaction receipt [nonce:{}, hash:{}, status:{}, revert-reason:{}]",
                submittedTx.nonce(), submittedTx.hash(), receipt.getStatus(), receipt.getRevertReason());
        if (receipt.isStatusOK()) {
            successCounter.increment();
        } else {
            failureCounter.increment();
        }
        return receipt;
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
