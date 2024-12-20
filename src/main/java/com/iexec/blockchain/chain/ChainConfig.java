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

package com.iexec.blockchain.chain;

import com.iexec.common.chain.validation.ValidNonZeroEthereumAddress;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import javax.annotation.PostConstruct;
import javax.validation.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Set;

@Slf4j
@Value
@Builder
@ConstructorBinding
@ConfigurationProperties(prefix = "chain")
public class ChainConfig {

    @Positive(message = "Chain id should be positive")
    @NotNull
    int id;

    @URL
    @NotEmpty
    String nodeAddress;

    @Positive(message = "Block time should be positive")
    @NotNull
    int blockTime;

    @ValidNonZeroEthereumAddress
    String hubAddress;

    boolean isSidechain;

    float gasPriceMultiplier;

    long gasPriceCap;

    @Positive
    @Max(value = 4)
    int maxAllowedTxPerBlock;

    @PostConstruct
    private void validate() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<ChainConfig>> violations = validator.validate(this);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }
}
