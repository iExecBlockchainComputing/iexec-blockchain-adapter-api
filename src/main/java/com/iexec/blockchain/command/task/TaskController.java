/*
 * Copyright 2021-2023 IEXEC BLOCKCHAIN TECH
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
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.iexec.blockchain.swagger.OpenApiConfig.SWAGGER_BASIC_AUTH;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final IexecHubService iexecHubService;
    private final TaskInitializeService taskInitializeService;
    private final TaskFinalizeService taskFinalizeService;

    public TaskController(IexecHubService iexecHubService,
                          TaskInitializeService taskInitializeService,
                          TaskFinalizeService taskFinalizeService) {
        this.iexecHubService = iexecHubService;
        this.taskInitializeService = taskInitializeService;
        this.taskFinalizeService = taskFinalizeService;
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
        String chainTaskId =
                taskInitializeService.start(chainDealId, taskIndex);
        if (!chainTaskId.isEmpty()) {
            return ResponseEntity.ok(chainTaskId);
        }
        return ResponseEntity.badRequest().build();
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
        return taskInitializeService.getStatusForCommand(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
        if (!taskFinalizeService.start(chainTaskId, args).isEmpty()) {
            return ResponseEntity.ok(chainTaskId);
        }
        return ResponseEntity.badRequest().build();
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
        return taskFinalizeService.getStatusForCommand(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
