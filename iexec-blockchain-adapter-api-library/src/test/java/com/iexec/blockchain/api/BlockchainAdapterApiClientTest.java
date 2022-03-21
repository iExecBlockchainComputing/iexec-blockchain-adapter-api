package com.iexec.blockchain.api;

import feign.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlockchainAdapterApiClientTest {

    @Test
    void instantiationTest() {
        Assertions.assertNotNull(BlockchainAdapterApiClientBuilder
                .getInstance(Logger.Level.FULL, "localhost"));
        Assertions.assertNotNull(BlockchainAdapterApiClientBuilder
                .getInstanceWithBasicAuth(Logger.Level.FULL, "localhost", "username", "password"));
    }

}
