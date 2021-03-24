package com.iexec.blockchain.task.initialize;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TaskInitializeRepository extends MongoRepository<TaskInitialize, String> {

    Optional<TaskInitialize> findByChainTaskId(String chainTaskId);

}
