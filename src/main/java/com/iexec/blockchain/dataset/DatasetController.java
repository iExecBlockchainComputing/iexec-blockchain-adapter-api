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

import com.iexec.blockchain.tool.Status;
import com.iexec.commons.poco.chain.ChainDataset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.iexec.blockchain.swagger.OpenApiConfig.SWAGGER_BASIC_AUTH;

@RestController
@RequestMapping("/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /**
     * Create dataset async
     *
     * @param name         name of the dataset
     * @param multiAddress link of the dataset
     * @param checksum     checksum of the dataset
     * @return requestId ID of the request
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping("/requests")
    public ResponseEntity<String> createDataset(@RequestParam String name,
                                                @RequestParam String multiAddress,
                                                @RequestParam String checksum) {

        String requestId = datasetService.createDataset(name, multiAddress, checksum);
        if (requestId != null) {
            return ResponseEntity.ok(requestId);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Get create dataset response
     *
     * @param requestId ID of the request
     * @return dataset address
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<String> getAddressForCreateDatasetRequest(@PathVariable String requestId) {
        return datasetService.getDatasetAddressForCreateDatasetRequest(requestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get create dataset status
     *
     * @param requestId ID of the request
     * @return status of the request
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping("/requests/{requestId}/status")
    public ResponseEntity<Status> getStatusForCreateDatasetRequest(@PathVariable String requestId) {
        return datasetService.getStatusForCreateDatasetRequest(requestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Read dataset on the blockchain
     *
     * @param address address of the dataset
     * @return dataset
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping
    public ResponseEntity<ChainDataset> getDatasetByAddress(@RequestParam String address) {
        return datasetService.getDatasetByAddress(address)
                .map(dataset -> ResponseEntity.ok(toChainDataset(dataset)))
                .orElse(ResponseEntity.notFound().build());
    }

    private ChainDataset toChainDataset(Dataset dataset) {
        return ChainDataset.builder()
                .chainDatasetId(dataset.getAddress())
                .name(dataset.getName())
                .uri(dataset.getMultiAddress())
                .checksum(dataset.getChecksum())
                .build();
    }

}
