package com.iexec.blockchain.api;

import com.iexec.common.utils.FeignBuilder;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;

public class BlockchainAdapterApiClientBuilder {

    private BlockchainAdapterApiClientBuilder() {}

    public static BlockchainAdapterApiClient getInstance(Logger.Level logLevel,
                                                         String url, String username, String password) {
        BasicAuthRequestInterceptor requestInterceptor =
                new BasicAuthRequestInterceptor(
                        username,
                        password
                );
        return FeignBuilder.createBuilder(logLevel)
                .requestInterceptor(requestInterceptor)
                .target(BlockchainAdapterApiClient.class, url);
    }

}
