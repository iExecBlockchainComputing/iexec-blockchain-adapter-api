package com.iexec.chain;

import com.iexec.chain.tool.IexecHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.iexec.chain.swagger.SpringFoxConfig.SWAGGER_BASIC_AUTH;

@RestController
@RequestMapping("/datasets")
public class DatasetController {

    private final IexecHubService iexecHubService;

    public DatasetController(IexecHubService iexecHubService) {
        this.iexecHubService = iexecHubService;
    }

    /**
     * Create dataset sync
     *
     * @param name         name of the dataset
     * @param multiAddress link of the dataset
     * @param checksum     checksum of the dataset
     * @return eth address of the dataset is deployed
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping
    public ResponseEntity<String> createDataset(@RequestParam String name,
                                                @RequestParam String multiAddress,
                                                @RequestParam String checksum) {
        String datasetAddress = iexecHubService.createDataset(name, multiAddress, checksum);
        if (StringUtils.hasText(datasetAddress)) {
            return ResponseEntity.ok(datasetAddress);
        }
        return ResponseEntity.badRequest().build();
    }

}
