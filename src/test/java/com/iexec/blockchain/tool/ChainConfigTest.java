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

    private final static int DEFAULT_CHAIN_ID = 1;
    private final static String DEFAULT_NODE_ADDRESS = "http://localhost:8545";
    private final static String DEFAULT_HUB_ADDRESS = "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002";
    private static final int DEFAULT_BLOCK_TIME = 1;

    // region Valid data
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
    // endregion

    // region Invalid chain ids
    static Stream<Integer> invalidChainIds() {
        return Stream.of(
                null,   // Chain id should not be null
                0,      // Chain id should be strictly positive
                -1      // Chain id should be strictly positive
        );
    }

    @ParameterizedTest
    @MethodSource("invalidChainIds")
    void shouldNotValidateChainId(Integer chainId) throws NoSuchMethodException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        final ChainConfig chainConfig = new ChainConfig(
                chainId,
                DEFAULT_NODE_ADDRESS,
                DEFAULT_BLOCK_TIME,
                DEFAULT_HUB_ADDRESS,
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
        ;
    }
    // endregion

    // region Invalid node addresses
    static Stream<String> invalidNodeAddresses() {
        return Stream.of(
                null,       // Node address should not be null
                "",         // Node address should be a valid URL
                "12345",    // Node address should be a valid URL
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002"    // Node address should be a valid URL
        );
    }

    @ParameterizedTest
    @MethodSource("invalidNodeAddresses")
    void shouldNotValidateNodeAddress(String nodeAddress) throws NoSuchMethodException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        final ChainConfig chainConfig = new ChainConfig(
                DEFAULT_CHAIN_ID,
                nodeAddress,
                DEFAULT_BLOCK_TIME,
                DEFAULT_HUB_ADDRESS,
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
                .hasMessageContaining("nodeAddress")
        ;
    }
    // endregion

    // region Invalid block time
    static Stream<Integer> invalidBlockTimes() {
        return Stream.of(
                null, // Block time should not be null
                0,    // Block time should be strictly positive
                -1    // Block time should be strictly positive
        );
    }

    @ParameterizedTest
    @MethodSource("invalidBlockTimes")
    void shouldNotValidateBlockTime(Integer blockTime) throws NoSuchMethodException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        final ChainConfig chainConfig = new ChainConfig(
                DEFAULT_CHAIN_ID,
                DEFAULT_NODE_ADDRESS,
                blockTime,
                DEFAULT_HUB_ADDRESS,
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
                .hasMessageContaining("blockTime")
        ;
    }
    // endregion

    // region Invalid hub address
    static Stream<String> invalidHubAddresses() {
        return Stream.of(
                null,       // Hub address should not be null
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200211111111111111", // Hub address size should be exactly 40
                "0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd200",    // Hub address size should be exactly 40
                "0x0000000000000000000000000000000000000000",    // Hub address should not be zero
                "http://hub.address"   // Hub address should be an Ethereum address
        );
    }

    @ParameterizedTest
    @MethodSource("invalidHubAddresses")
    void shouldNotValidateHubAddress(String hubAddress) throws NoSuchMethodException {
        Method validateConfig = ChainConfig.class.getDeclaredMethod("validate");
        validateConfig.setAccessible(true);

        final ChainConfig chainConfig = new ChainConfig(
                DEFAULT_CHAIN_ID,
                DEFAULT_NODE_ADDRESS,
                DEFAULT_BLOCK_TIME,
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
                .hasMessageContaining("hubAddress")
        ;
    }
    // endregion
}