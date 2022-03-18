package com.iexec.blockchain.api;

import com.iexec.common.utils.FeignBuilder;
import feign.Logger;

/**
 * Creates Feign client instances to query REST endpoints described in {@link BlockchainAdapterApiClient}.
 */
public class BlockchainAdapterApiClientBuilder {

    private BlockchainAdapterApiClientBuilder() {}

    /**
     * Create an unauthenticated feign client to query apis described in {@link BlockchainAdapterApiClient}.
     * @param logLevel Feign logging level to configure.
     * @param url Url targeted by the client.
     * @return Feign client for {@link BlockchainAdapterApiClient} apis.
     */
    public static BlockchainAdapterApiClient getInstance(Logger.Level logLevel, String url) {
        return FeignBuilder.createBuilder(logLevel)
                .target(BlockchainAdapterApiClient.class, url);
    }

    /**
     * Create an authenticated feign client to query apis described in {@link BlockchainAdapterApiClient}.
     * @param logLevel Feign logging level to configure.
     * @param url Url targeted by the client.
     * @param username Basic authentication username.
     * @param password Basic authentication password.
     * @return Feign client with basic authentication for {@link BlockchainAdapterApiClient} apis.
     */
    public static BlockchainAdapterApiClient getInstance(Logger.Level logLevel, String url,
                                                         String username, String password) {
        return FeignBuilder.createBuilder(logLevel, username, password)
                .target(BlockchainAdapterApiClient.class, url);
    }

}
