/*
 * Copyright 2025 IEXEC BLOCKCHAIN TECH
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

import com.iexec.commons.poco.chain.SignerService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class BlockchainListener {

    static final String LATEST_BLOCK_METRIC_NAME = "iexec.chain.block.latest";
    static final String TX_COUNT_METRIC_NAME = "iexec.chain.wallet.tx-count";

    private final String walletAddress;
    private final Web3j web3Client;
    private final AtomicLong lastSeenBlock;
    private final AtomicLong latestTxGauge;
    private final AtomicLong pendingTxGauge;

    public BlockchainListener(final ChainConfig chainConfig, final SignerService signerService) {
        this.walletAddress = signerService.getAddress();
        this.web3Client = Web3j.build(new HttpService(chainConfig.getNodeAddress()),
                chainConfig.getBlockTime().toMillis(), Async.defaultExecutorService());
        lastSeenBlock = Metrics.gauge(LATEST_BLOCK_METRIC_NAME, new AtomicLong(0));
        latestTxGauge = Metrics.gauge(TX_COUNT_METRIC_NAME, List.of(Tag.of("block", "latest")), new AtomicLong(0));
        pendingTxGauge = Metrics.gauge(TX_COUNT_METRIC_NAME, List.of(Tag.of("block", "pending")), new AtomicLong(0));
    }

    @Scheduled(fixedRate = 5000)
    private void run() {
        try {
            final EthBlock.Block latestBlock =
                    web3Client.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, true).send().getBlock();
            final BigInteger blockNumber = Numeric.toBigInt(latestBlock.getNumberRaw());
            lastSeenBlock.set(blockNumber.longValue());
            final BigInteger pendingTxCount = web3Client.ethGetTransactionCount(walletAddress,
                    DefaultBlockParameterName.PENDING).send().getTransactionCount();
            if (pendingTxCount.longValue() > pendingTxGauge.get() || pendingTxGauge.get() != latestTxGauge.get()) {
                final BigInteger latestTxCount = web3Client.ethGetTransactionCount(walletAddress,
                        DefaultBlockParameterName.LATEST).send().getTransactionCount();
                pendingTxGauge.set(pendingTxCount.longValue());
                latestTxGauge.set(latestTxCount.longValue());
            }
            log.info("Transaction count [block:{}, pending:{}, latest:{}]",
                    lastSeenBlock, pendingTxGauge.get(), latestTxGauge.get());
        } catch (Exception e) {
            log.error("An error happened while fetching data on-chain", e);
        }
    }

}
