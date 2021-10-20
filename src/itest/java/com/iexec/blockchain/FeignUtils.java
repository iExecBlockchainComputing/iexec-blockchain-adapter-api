package com.iexec.blockchain;

import feign.Feign;
import feign.Logger;
import feign.Response;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.StringDecoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

import java.io.IOException;
import java.lang.reflect.Type;


public class FeignUtils {

    private FeignUtils() {
    }

    public static Feign.Builder getFeignBuilder(String username, String password) {
        return Feign.builder()
                .logger(new Slf4jLogger()).logLevel(Logger.Level.BASIC)
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder() { //handle both String or Json response depending on call return type
                    @Override
                    public Object decode(Response response, Type type) throws IOException {
                        if (type.equals(String.class)) {
                            return new StringDecoder().decode(response, type);
                        }
                        return super.decode(response, type);
                    }
                }) // parse response
                .requestInterceptor(new BasicAuthRequestInterceptor(username, password))
                .requestInterceptor(template ->
                        template.header("Content-Type", "application/json")) // build proper request
                ;
    }

}