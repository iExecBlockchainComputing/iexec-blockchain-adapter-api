package com.iexec.blockchain.task;

import com.iexec.blockchain.task.initialize.TaskInitializeService;
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

    public TaskController(IexecHubService iexecHubService,
                          TaskInitializeService taskInitializeService) {
        this.iexecHubService = iexecHubService;
        this.taskInitializeService = taskInitializeService;
    }

    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping
    public ResponseEntity<ChainTask> getTask(@RequestParam String chainTaskId) {
        return iexecHubService.getChainTask(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeTask(@RequestParam String chainDealId,
                                                 @RequestParam int taskIndex) {
        String chainTaskId = taskInitializeService.initializeTask(chainDealId, taskIndex);
        if (!chainTaskId.isEmpty()) {
            return ResponseEntity.ok(chainTaskId);
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(security = @SecurityRequirement(name = SWAGGER_BASIC_AUTH))
    @GetMapping("/initialize/{chainTaskId}/status")
    public ResponseEntity<Status> getStatusForInitializeTaskRequest(@RequestParam String chainTaskId) {
        return taskInitializeService.getStatusForInitializeTaskRequest(chainTaskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
