/*
 * Copyright 2021-2023 IEXEC BLOCKCHAIN TECH
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
import com.iexec.commons.poco.chain.ChainDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DatasetServiceTest {

    public static final String NAME = "name";
    public static final String MULTI_ADDRESS = "multiAddress";
    public static final String CHECKSUM = "checksum";
    public static final String ID = "id";
    public static final String DATASET_ADDRESS = "datasetAddress";
    public static final String REQUEST_ID = "requestId";

    @InjectMocks
    private DatasetService datasetService;
    @Mock
    private DatasetRepository datasetRepository;
    @Mock
    private IexecHubService iexecHubService;
    @Mock
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateDataset() {
        Dataset dataset = mock(Dataset.class);
        when(dataset.getRequestId()).thenReturn(REQUEST_ID);
        when(datasetRepository.save(any())).thenReturn(dataset);
        String requestId =
                datasetService.createDataset(NAME, MULTI_ADDRESS, CHECKSUM);
        Assertions.assertEquals(dataset.getRequestId(), requestId);
        verify(queueService, times(1))
                .addExecutionToQueue(any(), eq(false));
        ArgumentCaptor<Dataset> datasetCaptor =
                ArgumentCaptor.forClass(Dataset.class);
        verify(datasetRepository, times(1))
                .save(datasetCaptor.capture());
        Assertions.assertEquals(Status.RECEIVED,
                datasetCaptor.getValue().getStatus());
        Assertions.assertEquals(NAME,
                datasetCaptor.getValue().getName());
        Assertions.assertEquals(MULTI_ADDRESS,
                datasetCaptor.getValue().getMultiAddress());
        Assertions.assertEquals(CHECKSUM,
                datasetCaptor.getValue().getChecksum());
    }

    @Test
    void shouldNotCreateDatasetOnChainAndStoreSinceLocallyMissing() {
        when(datasetRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.empty());

        datasetService.createDatasetOnChainAndStore(REQUEST_ID);
        verify(datasetRepository, times(0)).save(any());
    }

    @Test
    void shouldCreateDatasetOnChainAndStoreSuccess() {
        Dataset dataset = Dataset.builder()
                .id(ID)
                .requestId(REQUEST_ID)
                .status(Status.RECEIVED)
                .name(NAME)
                .multiAddress(MULTI_ADDRESS)
                .checksum(CHECKSUM)
                .build();

        when(datasetRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.save(dataset)).thenReturn(dataset);
        when(iexecHubService.createDataset(NAME, MULTI_ADDRESS, CHECKSUM))
                .thenReturn(DATASET_ADDRESS);

        datasetService.createDatasetOnChainAndStore(REQUEST_ID);
        verify(datasetRepository, times(2))
                .save(dataset);
        assertThat(dataset.getStatus()).isEqualTo(Status.SUCCESS);
    }

    @Test
    void shouldTryCreateDatasetOnChainAndStoreFailure() {
        Dataset dataset = Dataset.builder()
                .id(ID)
                .requestId(REQUEST_ID)
                .status(Status.RECEIVED)
                .name(NAME)
                .multiAddress(MULTI_ADDRESS)
                .checksum(CHECKSUM)
                .build();
        when(datasetRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(dataset));
        when(datasetRepository.save(dataset)).thenReturn(dataset);
        when(iexecHubService.createDataset(NAME, MULTI_ADDRESS, CHECKSUM))
                .thenReturn("");

        datasetService.createDatasetOnChainAndStore(REQUEST_ID);
        verify(datasetRepository, times(2))
                .save(dataset);
        assertThat(dataset.getStatus()).isEqualTo(Status.FAILURE);
    }

    @Test
    void shouldGetStatusForCreateDatasetRequest() {
        Dataset dataset = mock(Dataset.class);
        when(dataset.getStatus()).thenReturn(Status.PROCESSING);
        when(datasetRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(dataset));

        Assertions.assertEquals(Optional.of(Status.PROCESSING),
                datasetService.getStatusForCreateDatasetRequest(REQUEST_ID));
    }

    @Test
    void shouldNotGetStatusForCreateDatasetRequest() {
        Dataset dataset = mock(Dataset.class);
        when(dataset.getStatus()).thenReturn(null);
        when(datasetRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(dataset));

        Assertions.assertEquals(Optional.empty(),
                datasetService.getStatusForCreateDatasetRequest(REQUEST_ID));
    }


    @Test
    void shouldGetDatasetAddressByRequestId() {
        Dataset dataset = mock(Dataset.class);
        when(dataset.getAddress()).thenReturn(DATASET_ADDRESS);
        when(datasetRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(dataset));

        Assertions.assertEquals(Optional.of(DATASET_ADDRESS),
                datasetService.getDatasetAddressForCreateDatasetRequest(REQUEST_ID));
    }

    @Test
    void shouldNotGetDatasetAddressByRequestId() {
        Dataset dataset = mock(Dataset.class);
        when(datasetRepository.findByRequestId(REQUEST_ID))
                .thenReturn(Optional.of(dataset));

        Assertions.assertEquals(Optional.empty(),
                datasetService.getDatasetAddressForCreateDatasetRequest(REQUEST_ID));
    }

    @Test
    void shouldGetDatasetByAddressFromCache() {
        Dataset dataset = mock(Dataset.class);
        when(dataset.getStatus()).thenReturn(Status.SUCCESS);
        when(datasetRepository.findByAddress(DATASET_ADDRESS))
                .thenReturn(Optional.of(dataset));

        Assertions.assertEquals(Optional.of(dataset),
                datasetService.getDatasetByAddress(DATASET_ADDRESS));
    }

    @Test
    void shouldGetDatasetByAddressWithFetch() {
        when(datasetRepository.findByAddress(DATASET_ADDRESS))
                .thenReturn(Optional.empty());
        com.iexec.commons.poco.contract.generated.Dataset datasetContract =
                mock(com.iexec.commons.poco.contract.generated.Dataset.class);
        when(iexecHubService.getDatasetContract(DATASET_ADDRESS))
                .thenReturn(datasetContract);
        ChainDataset chainDataset = ChainDataset.builder()
                .chainDatasetId(DATASET_ADDRESS)
                .name(NAME)
                .uri(MULTI_ADDRESS)
                .checksum(CHECKSUM)
                .build();
        when(iexecHubService.getChainDataset(datasetContract))
                .thenReturn(Optional.of(chainDataset));
        Dataset dataset = mock(Dataset.class);
        when(datasetRepository.save(any())).thenReturn(dataset);

        Optional<Dataset> datasetByAddress =
                datasetService.getDatasetByAddress(DATASET_ADDRESS);

        Assertions.assertEquals(Optional.of(dataset), datasetByAddress);
        ArgumentCaptor<Dataset> datasetCaptor =
                ArgumentCaptor.forClass(Dataset.class);
        verify(datasetRepository, times(1))
                .save(datasetCaptor.capture());
        Assertions.assertEquals(chainDataset.getChainDatasetId(),
                datasetCaptor.getValue().getAddress());
        Assertions.assertEquals(chainDataset.getName(),
                datasetCaptor.getValue().getName());
        Assertions.assertEquals(chainDataset.getUri(),
                datasetCaptor.getValue().getMultiAddress());
        Assertions.assertEquals(chainDataset.getChecksum(),
                datasetCaptor.getValue().getChecksum());
    }

    @Test
    void shouldGetDatasetByAddressWithFailedFetchSinceNoDatasetContract() {
        when(datasetRepository.findByAddress(DATASET_ADDRESS))
                .thenReturn(Optional.empty());
        when(iexecHubService.getDatasetContract(DATASET_ADDRESS))
                .thenReturn(null);

        Optional<Dataset> datasetByAddress =
                datasetService.getDatasetByAddress(DATASET_ADDRESS);

        Assertions.assertTrue(datasetByAddress.isEmpty());
    }

    @Test
    void shouldGetDatasetByAddressWithFailedFetchSinceNoChainDataset() {
        when(datasetRepository.findByAddress(DATASET_ADDRESS))
                .thenReturn(Optional.empty());
        com.iexec.commons.poco.contract.generated.Dataset datasetContract =
                mock(com.iexec.commons.poco.contract.generated.Dataset.class);
        when(iexecHubService.getDatasetContract(DATASET_ADDRESS))
                .thenReturn(datasetContract);
        when(iexecHubService.getChainDataset(datasetContract))
                .thenReturn(Optional.empty());

        Optional<Dataset> datasetByAddress =
                datasetService.getDatasetByAddress(DATASET_ADDRESS);

        Assertions.assertTrue(datasetByAddress.isEmpty());
    }
}