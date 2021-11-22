/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.blockchain.tool.validation;

import com.iexec.common.utils.EthAddress;
import org.web3j.utils.Numeric;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class EthereumNonZeroAddressValidator
        implements ConstraintValidator<ValidNonZeroEthereumAddress, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return EthAddress.validate(value) && !Arrays.equals(
                Numeric.hexStringToByteArray(value),
                // An Ethereum address is a 40-hex characters string = 20 bytes
                new byte[20]
        );
    }
}
