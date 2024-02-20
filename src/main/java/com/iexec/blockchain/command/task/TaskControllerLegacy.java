/*
 * Copyright 2021-2024 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.command.task;

import com.iexec.blockchain.api.CommandStatus;
import com.iexec.blockchain.command.task.finalize.TaskFinalizeService;
import com.iexec.blockchain.command.task.initialize.TaskInitializeService;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.iexec.blockchain.swagger.OpenApiConfig.SWAGGER_BASIC_AUTH;

/**
 * Call /v1/tasks endpoints in {@code TaskControllerV1}
 */
@Deprecated(forRemoval = true)
@RestController
@RequestMapping("/tasks")
public class TaskControllerLegacy extends TaskController {

    public TaskControllerLegacy(TaskInitializeService taskInitializeService, TaskFinalizeService taskFinalizeService) {
        super(taskInitializeService, taskFinalizeService);
    }

    /**
     * Start the asynchronous `initialize task` blockchain remote call.
     *
     * @param chainDealId blockchain deal ID
     * @param taskIndex   index of the task int the bag
     * @return blockchain task ID if successful
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping("/initialize")
    public ResponseEntity<String> requestInitializeTask(
            @RequestParam String chainDealId,
            @RequestParam int taskIndex) {
        return super.requestInitializeTask(chainDealId, taskIndex);
    }

    /**
     * Read status for the asynchronous `initialize task` blockchain remote call.
     *
     * @param chainTaskId blockchain ID of the task
     * @return status
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping("/initialize/{chainTaskId}/status")
    public ResponseEntity<CommandStatus> getStatusForInitializeTaskRequest(
            @PathVariable String chainTaskId) {
        return super.getStatusForInitializeTaskRequest(chainTaskId);
    }

    /**
     * Start the asynchronous `finalize task` blockchain remote call.
     *
     * @param chainTaskId blockchain task ID
     * @param args        input arguments for `finalize task`
     * @return blockchain task ID if successful
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping("/finalize/{chainTaskId}")
    public ResponseEntity<String> requestFinalizeTask(
            @PathVariable String chainTaskId,
            @RequestBody TaskFinalizeArgs args) {
        return super.requestFinalizeTask(chainTaskId, args);
    }

    /**
     * Read status for the asynchronous `finalize task` blockchain remote call.
     *
     * @param chainTaskId blockchain ID of the task
     * @return status
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping("/finalize/{chainTaskId}/status")
    public ResponseEntity<CommandStatus> getStatusForFinalizeTaskRequest(
            @PathVariable String chainTaskId) {
        return super.getStatusForFinalizeTaskRequest(chainTaskId);
    }

}
