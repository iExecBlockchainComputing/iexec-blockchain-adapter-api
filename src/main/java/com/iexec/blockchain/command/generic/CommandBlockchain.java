/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.command.generic;


import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface CommandBlockchain<A extends CommandArgs> {


    /**
     * Check if a blockchain command can be made. It mostly makes business
     * checks such as verifying `require(..)`s of business contracts.
     * <p>
     * Note: Because of decentralized actors/components and asynchronous
     * transactions, it doesn't guarantee a blockchain command will be
     * successful.
     *
     * @param args input arguments for the blockchain command
     * @return true if blockchain command could succeed
     */
    boolean canSendBlockchainCommand(A args);

    /**
     * Synchronously perform a blockchain command.
     *
     * @param args input arguments for the blockchain command
     * @return transaction receipt
     */
    TransactionReceipt sendBlockchainCommand(A args) throws Exception;
}
