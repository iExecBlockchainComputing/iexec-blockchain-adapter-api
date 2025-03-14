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

import com.iexec.commons.poco.chain.validation.ValidNonZeroEthereumAddress;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Slf4j
@Value
@Builder
@Validated
@ConfigurationProperties(prefix = "chain")
public class ChainConfig {

    @Positive(message = "Chain id should be positive")
    @NotNull
    int id;

    boolean sidechain;

    @URL
    @NotEmpty
    String nodeAddress;

    @ValidNonZeroEthereumAddress
    String hubAddress;

    @DurationMin(millis=100)
    @DurationMax(seconds=20)
    @NotNull
    Duration blockTime;

    @Positive
    float gasPriceMultiplier;

    @PositiveOrZero
    long gasPriceCap;

    @Positive
    @Max(value = 4)
    int maxAllowedTxPerBlock;

}
