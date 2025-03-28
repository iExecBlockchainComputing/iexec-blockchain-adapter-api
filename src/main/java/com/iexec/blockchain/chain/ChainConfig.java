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

    @Positive(message = "Chain id must be greater than 0")
    @NotNull(message = "Chain id must not be null")
    int id;

    boolean sidechain;

    @URL(message = "Node address must be a valid URL")
    @NotEmpty(message = "Node address must not be empty")
    String nodeAddress;

    @ValidNonZeroEthereumAddress(message = "Hub address must be a valid non zero Ethereum address")
    String hubAddress;

    @DurationMin(millis = 100, message = "Block time must be greater than 100ms")
    @DurationMax(seconds = 20, message = "Block time must be less than 20s")
    @NotNull(message = "Block time must not be null")
    Duration blockTime;

    @Positive(message = "Gas price multiplier must be greater than 0")
    float gasPriceMultiplier;

    @PositiveOrZero(message = "Gas price cap must be greater or equal to 0")
    long gasPriceCap;

    @Positive(message = "Max allowed tx per block must be greater than 0")
    @Max(value = 4, message = "Max allowed tx per block must be less or equal to 4")
    int maxAllowedTxPerBlock;

}
