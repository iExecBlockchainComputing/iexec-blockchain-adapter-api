package com.iexec.blockchain.dataset;

import com.iexec.blockchain.tool.Status;
import com.iexec.common.chain.ChainDataset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.iexec.blockchain.swagger.SpringFoxConfig.SWAGGER_BASIC_AUTH;

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
    public ResponseEntity<String> getAddressForCreateDatasetRequest(@RequestParam String requestId) {
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
    public ResponseEntity<Status> getStatusForCreateDatasetRequest(@RequestParam String requestId) {
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
