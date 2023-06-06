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

package com.iexec.blockchain.tool;

import com.iexec.common.chain.validation.ValidNonZeroEthereumAddress;
import lombok.*;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Component
@Getter
@ToString
@Builder
@AllArgsConstructor
public class ChainConfig {

    @Value("${chain.id}")
    @Positive(message = "Chain id should be positive")
    @NotNull
    private Integer chainId;

    @Value("${chain.node-address}")
    @URL
    @NotEmpty
    private String nodeAddress;

    @Value("${chain.block-time}")
    @Positive(message = "Block time should be positive")
    @NotNull
    private Integer blockTime;

    @Value("${chain.hub-address}")
    @ValidNonZeroEthereumAddress
    private String hubAddress;

    @Value("${chain.is-sidechain}")
    private boolean isSidechain;

    @Value("${chain.gas-price-multiplier}")
    private float gasPriceMultiplier;

    @Value("${chain.gas-price-cap}")
    private long gasPriceCap;

    @Getter(AccessLevel.NONE) // no getter
    private final Validator validator;

    @Autowired
    public ChainConfig(Validator validator) {
        this.validator = validator;
    }

    @PostConstruct
    private void validate() {
        if (!validator.validate(this).isEmpty()) {
            throw new ConstraintViolationException(validator.validate(this));
        }
    }
}
