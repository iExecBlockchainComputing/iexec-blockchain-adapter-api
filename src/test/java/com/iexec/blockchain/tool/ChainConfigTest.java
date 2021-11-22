package com.iexec.blockchain.tool;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChainConfigTest {
    final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    static Stream<Arguments> validData() {
        return Stream.of(
                Arguments.of(100, "http://localhost:8545", "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002", 1),
                Arguments.of(42, "https://localhost:8545", "0x0000000000000000000000000000000000000001", 10),
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
            throws NoSuchMethodException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        final ChainConfig chainConfig = new ChainConfig(
                chainId,
                nodeAddress,
                blockTime,
                hubAddress,
                false,
                0f,
                0L,
                "",
                validator
        );

        System.out.println(chainConfig);
        assertThatCode(() -> validateConfig.invoke(chainConfig))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> invalidData() {
        return Stream.of(
                Arguments.of(
                        null,   // Chain id should not be null
                        "12345",  // Node address should be a valid URL
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
                        0,  // Chain id should be strictly positive
                        null,   // Node address should not be null
                        "0x0000000000000000000000000000000000000000",    // Hub address should not be zero
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
            throws NoSuchMethodException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        final ChainConfig chainConfig = new ChainConfig(
                chainId,
                nodeAddress,
                blockTime,
                hubAddress,
                false,
                0f,
                0L,
                "",
                validator
        );

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
}