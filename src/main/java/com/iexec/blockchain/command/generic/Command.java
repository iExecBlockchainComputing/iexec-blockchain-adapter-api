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

package com.iexec.blockchain.command.generic;

import com.iexec.blockchain.api.CommandStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.Instant;

/**
 * Command refers to the "C" of the CQRS pattern where CQRS itself stands
 * for Command Query Responsibility Segregation.
 * Command is different from Query since it does alter internal
 * states (CRUD without the "R")
 * More info can be found at:
 * - https://martinfowler.com/bliki/CQRS.html
 * - https://microservices.io/patterns/microservices.html
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "command_task_idx",
        def = "{'chainObjectId': 1, 'commandName': 1}",
        unique = true)
public class Command {

    @Id
    private String id;
    @Version
    private Long version;

    private String chainObjectId;
    private CommandName commandName;
    private CommandStatus status;
    private Instant creationDate;
    private Instant processingDate;
    private Instant finalDate;
    private TransactionReceipt transactionReceipt;

    private CommandArgs args;

}
