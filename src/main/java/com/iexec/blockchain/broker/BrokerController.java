package com.iexec.blockchain.broker;

import com.iexec.common.sdk.broker.BrokerOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.iexec.blockchain.swagger.OpenApiConfig.SWAGGER_BASIC_AUTH;

@Slf4j
@Profile("itest")
@RestController
@RequestMapping("/broker")
public class BrokerController {

    private final BrokerService brokerService;

    public BrokerController(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    /**
     * Match compatible orders over a broker.
     * This endpoint is an API wrapper for the broker API.
     *
     * @param brokerOrder compatible orders
     * @return deal ID if orders are matched on-chain
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping("/broker/orders/match")
    public ResponseEntity<String> matchOrders(@RequestBody BrokerOrder brokerOrder) {
        try {
            String dealId = brokerService.matchOrders(brokerOrder);
            if (!StringUtils.isEmpty(dealId)) {
                return ResponseEntity.ok(dealId);
            }
        } catch (IllegalStateException e) {
            log.error("Match order failed with illegal state", e);
        } catch (NullPointerException e) {
            log.error("Match order failed with null value", e);
        }
        return ResponseEntity.badRequest().build();
    }

}
