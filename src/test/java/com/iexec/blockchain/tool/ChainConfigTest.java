package com.iexec.blockchain.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChainConfigTest {
    private ChainConfig chainConfig;

    @BeforeEach
    void setUp() {
        this.chainConfig = new ChainConfig(
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }

    static Stream<Arguments> validData() {
        return Stream.of(
                Arguments.of(100, "http://localhost:8545", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", 1),
                Arguments.of(42, "https://localhost:8545", "0x0000000000000000000000000000000000000000", 10),
                Arguments.of(10, "https://www.classic-url.com", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", 42),
                Arguments.of(1, "http://ibaa.iex.ec:443/test?validation=should:be@OK", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", 100)
        );
    }

    @ParameterizedTest
    @MethodSource("validData")
    void shouldValidate(Integer chainId,
                        String nodeAddress,
                        String hubAddress,
                        Integer blockTime)
            throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        setValueForPrivateField(chainConfig, "chainId", chainId);
        setValueForPrivateField(chainConfig, "nodeAddress", nodeAddress);
        setValueForPrivateField(chainConfig, "hubAddress", hubAddress);
        setValueForPrivateField(chainConfig, "blockTime", blockTime);

        System.out.println(chainConfig);
        assertThatCode(() -> validateConfig.invoke(chainConfig))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> invalidData() {
        return Stream.of(
                Arguments.of(
                        null,   // Chain id should not be null
                        "http://",  // Node address should be a valid URL
                        "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200211111111111111", // Hub address size should be exactly 40
                        0   // Block time should be strictly positive
                ),
                Arguments.of(
                        0,  // Chain id should be strictly positive
                        null,   // Node address should not be null
                        "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200",    // Hub address size should be exactly 40
                        -1  // Block time should be strictly positive
                ),
                Arguments.of(
                        -1, // Chain id should be strictly positive
                        "", // Node address should be a valid URL
                        null,   // Hub address should not be null
                        -42 // Block time should be strictly positive
                ),
                Arguments.of(
                        -42,    // Chain id should be strictly positive
                        "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002",   // Node address should be a valid URL
                        "http://hub.address",   // Hub address should be an Ethereum address
                        null    // Block time should not be null
                )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidData")
    void shouldNotValidate(Integer chainId,
                           String nodeAddress,
                           String hubAddress,
                           Integer blockTime)
            throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        setValueForPrivateField(chainConfig, "chainId", chainId);
        setValueForPrivateField(chainConfig, "nodeAddress", nodeAddress);
        setValueForPrivateField(chainConfig, "hubAddress", hubAddress);
        setValueForPrivateField(chainConfig, "blockTime", blockTime);


        System.out.println(chainConfig);
        assertThatThrownBy(() -> validateConfig.invoke(chainConfig))
                .getRootCause()
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("chainId")
                .hasMessageContaining("nodeAddress")
                .hasMessageContaining("hubAddress")
                .hasMessageContaining("blockTime")
        ;
    }

    // Some memoization to speed up the tests.
    private static final Map<String, Field> fieldsCache = new HashMap<>();

    private <T, R> void setValueForPrivateField(T object, String fieldName, R value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field;
        if (!fieldsCache.containsKey(fieldName)) {
            field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            fieldsCache.put(fieldName, field);
        } else {
            field = fieldsCache.get(fieldName);
        }
        field.set(object, value);
    }
}