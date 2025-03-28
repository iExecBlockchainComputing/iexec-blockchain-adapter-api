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

package com.iexec.blockchain.command.task.initialize;

import com.iexec.blockchain.command.generic.CommandArgs;
import com.iexec.blockchain.command.generic.CommandName;
import lombok.Data;


@Data
public class TaskInitializeArgs implements CommandArgs {

    private final String chainTaskId;
    private final String chainDealId;
    private final int taskIndex;

    @Override
    public CommandName getCommandName() {
        return CommandName.TASK_INITIALIZE;
    }

    @Override
    public String getChainObjectId() {
        return chainTaskId;
    }
}
