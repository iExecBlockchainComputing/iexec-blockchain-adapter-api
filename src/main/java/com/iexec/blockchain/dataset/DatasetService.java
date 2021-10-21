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

package com.iexec.blockchain.dataset;


import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.blockchain.tool.QueueService;
import com.iexec.blockchain.tool.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final IexecHubService iexecHubService;
    private final QueueService queueService;

    public DatasetService(DatasetRepository datasetRepository,
                          IexecHubService iexecHubService,
                          QueueService queueService) {
        this.datasetRepository = datasetRepository;
        this.iexecHubService = iexecHubService;
        this.queueService = queueService;
    }

    /**
     * Prepare a dataset locally with its requestId (sync) and create a dataset
     * on-chain (async)
     *
     * @param name         name of the dataset
     * @param multiAddress uri of the dataset
     * @param checksum     checksum of the dataset
     * @return ID of the create dataset request
     */
    public String createDataset(String name, String multiAddress, String checksum) {
        Dataset dataset = datasetRepository.save(Dataset.builder()
                .status(Status.RECEIVED)
                .name(name)
                .multiAddress(multiAddress)
                .checksum(checksum)
                .build());

        Runnable runnable = () -> createDatasetOnChainAndStore(dataset.getRequestId());
        queueService.addExecutionToQueue(runnable, false);
        return dataset.getRequestId();
    }

    /**
     * Create a dataset on-chain, update status and retrieve address
     *
     * @param requestId ID of the create dataset request
     */
    void createDatasetOnChainAndStore(String requestId) {
        Optional<Dataset> localDataset = datasetRepository.findByRequestId(requestId);
        if (localDataset.isEmpty()) {
            return;
        }
        Dataset dataset = localDataset.get();

        dataset.setStatus(Status.PROCESSING);
        datasetRepository.save(dataset);
        String datasetAddress = iexecHubService.createDataset(dataset.getName(),
                dataset.getMultiAddress(),
                dataset.getChecksum());
        if (StringUtils.hasText(datasetAddress)) {
            log.info("Created dataset [name:{}, url:{}, checksum:{}]",
                    dataset.getName(),
                    dataset.getMultiAddress(),
                    dataset.getChecksum());
            dataset.setStatus(Status.SUCCESS);
            dataset.setAddress(datasetAddress);
        } else {
            log.error("Failed to create dataset [name:{}, url:{}, checksum:{}]",
                    dataset.getName(),
                    dataset.getMultiAddress(),
                    dataset.getChecksum());
            dataset.setStatus(Status.FAILURE);
        }
        datasetRepository.save(dataset);
    }

    /**
     * Get status for create dataset request
     *
     * @param requestId requestId ID of the create dataset request
     * @return status for on-chain create dataset
     */
    public Optional<Status> getStatusForCreateDatasetRequest(String requestId) {
        Status status = datasetRepository.findByRequestId(requestId)
                .map(Dataset::getStatus)
                .orElse(null);
        if (status != null) {
            return Optional.of(status);
        }
        return Optional.empty();
    }

    /**
     * Get deployed dataset address for create dataset request
     *
     * @param requestId requestId ID of the create dataset request
     * @return dataset address for on-chain created dataset
     */
    public Optional<String> getDatasetAddressForCreateDatasetRequest(String requestId) {
        String datasetAddress = datasetRepository.findByRequestId(requestId)
                .map(Dataset::getAddress)
                .orElse("");
        if (!datasetAddress.isEmpty()) {
            return Optional.of(datasetAddress);
        }
        return Optional.empty();
    }


    /**
     * Find in cache or fetch dataset on-chain
     *
     * @param address address of the dataset
     * @return dataset
     */
    public Optional<Dataset> getDatasetByAddress(String address) {
        Optional<Dataset> datasetFromCache = datasetRepository.findByAddress(address)
                .filter(dataset -> dataset.getStatus() != null)
                .filter(dataset -> dataset.getStatus().equals(Status.SUCCESS));
        if (datasetFromCache.isPresent()) {
            return datasetFromCache;
        }

        return iexecHubService.getChainDataset(iexecHubService.getDatasetContract(address))
                .map(chainDataset -> {
                    Dataset dataset = datasetRepository.save(Dataset.builder()
                            .status(Status.SUCCESS)
                            .address(chainDataset.getChainDatasetId())
                            .name(chainDataset.getName())
                            .multiAddress(chainDataset.getUri())
                            .checksum(chainDataset.getChecksum())
                            .build());
                    log.info("Found dataset and added it to cache " +
                                    "[address:{}, name:{}, url:{}, checksum:{}]",
                            dataset.getAddress(),
                            dataset.getName(),
                            dataset.getMultiAddress(),
                            dataset.getChecksum());
                    return dataset;
                });
    }

}
