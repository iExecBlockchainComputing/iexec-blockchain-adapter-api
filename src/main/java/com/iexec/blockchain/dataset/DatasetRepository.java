package com.iexec.blockchain.dataset;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DatasetRepository extends MongoRepository<Dataset, String> {

    Optional<Dataset> findByRequestId(String requestId);

    Optional<Dataset> findByAddress(String id);

}
