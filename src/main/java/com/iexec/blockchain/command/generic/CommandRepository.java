package com.iexec.blockchain.command.generic;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CommandRepository<C extends Command<? extends CommandArgs>>
        extends MongoRepository<C, String> {

    Optional<C> findByChainObjectId(String chainObjectId);

}
