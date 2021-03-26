package com.iexec.blockchain.command.task;

import com.iexec.blockchain.command.task.contribute.TaskContributeArgs;
import com.iexec.blockchain.command.task.contribute.TaskContributeService;
import com.iexec.blockchain.command.task.finalize.TaskFinalizeArgs;
import com.iexec.blockchain.command.task.finalize.TaskFinalizeService;
import com.iexec.blockchain.command.task.initialize.TaskInitializeService;
import com.iexec.blockchain.command.task.reveal.TaskRevealArgs;
import com.iexec.blockchain.command.task.reveal.TaskRevealService;
import com.iexec.blockchain.tool.IexecHubService;
import com.iexec.blockchain.tool.Status;
import com.iexec.common.chain.ChainTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.iexec.blockchain.swagger.SpringFoxConfig.SWAGGER_BASIC_AUTH;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final IexecHubService iexecHubService;
    private final TaskInitializeService taskInitializeService;
    private final TaskContributeService taskContributeService;
    private final TaskRevealService taskRevealService;
    private final TaskFinalizeService taskFinalizeService;

    public TaskController(IexecHubService iexecHubService,
                          TaskInitializeService taskInitializeService,
                          TaskContributeService taskContributeService,
                          TaskRevealService taskRevealService,
                          TaskFinalizeService taskFinalizeService) {
        this.iexecHubService = iexecHubService;
        this.taskInitializeService = taskInitializeService;
        this.taskContributeService = taskContributeService;
        this.taskRevealService = taskRevealService;
        this.taskFinalizeService = taskFinalizeService;
    }

    /**
     * Read task metadata on the blockchain.
     *
     * @param chainTaskId blockchain ID of the task
     * @return task metadata
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping
    public ResponseEntity<ChainTask> getTask(
            @RequestParam String chainTaskId) {
        return iexecHubService.getChainTask(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<String> initializeTask(
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
    public ResponseEntity<Status> getStatusForInitializeTaskRequest(
            @RequestParam String chainTaskId) {
        return taskInitializeService.getStatusForCommand(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Start the asynchronous `contribute task` blockchain remote call.
     *
     * @param chainTaskId blockchain task ID
     * @param args        input arguments for `contribute task`
     * @return blockchain task ID if successful
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping("/contribute")
    public ResponseEntity<String> contributeTask(
            @RequestParam String chainTaskId,
            @RequestBody TaskContributeArgs args) {
        if (!taskContributeService.start(chainTaskId, args).isEmpty()) {
            return ResponseEntity.ok(chainTaskId);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Read status for the asynchronous `contribute task` blockchain remote call.
     *
     * @param chainTaskId blockchain ID of the task
     * @return status
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping("/contribute/{chainTaskId}/status")
    public ResponseEntity<Status> getStatusForContributeTaskRequest(
            @RequestParam String chainTaskId) {
        return taskContributeService.getStatusForCommand(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Start the asynchronous `reveal task` blockchain remote call.
     *
     * @param chainTaskId blockchain task ID
     * @param args        input arguments for `reveal task`
     * @return blockchain task ID if successful
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping("/reveal")
    public ResponseEntity<String> revealTask(
            @RequestParam String chainTaskId,
            @RequestBody TaskRevealArgs args) {
        if (!taskRevealService.start(chainTaskId, args).isEmpty()) {
            return ResponseEntity.ok(chainTaskId);
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Read status for the asynchronous `reveal task` blockchain remote call.
     *
     * @param chainTaskId blockchain ID of the task
     * @return status
     */
    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping("/reveal/{chainTaskId}/status")
    public ResponseEntity<Status> getStatusForRevealTaskRequest(
            @RequestParam String chainTaskId) {
        return taskRevealService.getStatusForCommand(chainTaskId)
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
    @PostMapping("/finalize")
    public ResponseEntity<String> finalizeTask(
            @RequestParam String chainTaskId,
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
    public ResponseEntity<Status> getStatusForFinalizeTaskRequest(
            @RequestParam String chainTaskId) {
        return taskFinalizeService.getStatusForCommand(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
