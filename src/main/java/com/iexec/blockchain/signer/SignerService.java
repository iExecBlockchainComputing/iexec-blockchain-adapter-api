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

package com.iexec.blockchain.signer;

import com.iexec.blockchain.tool.ChainConfig;
import com.iexec.blockchain.tool.CredentialsService;
import com.iexec.commons.poco.eip712.OrderSigner;
import com.iexec.commons.poco.order.AppOrder;
import com.iexec.commons.poco.order.DatasetOrder;
import com.iexec.commons.poco.order.RequestOrder;
import com.iexec.commons.poco.order.WorkerpoolOrder;
import org.springframework.stereotype.Service;

@Service
public class SignerService {

    private final OrderSigner orderSigner;

    public SignerService(ChainConfig chainConfig, CredentialsService credentialsService) {
        this.orderSigner = new OrderSigner(
                chainConfig.getId(),
                chainConfig.getHubAddress(),
                credentialsService.getCredentials().getEcKeyPair());
    }

    public AppOrder signAppOrder(AppOrder appOrder) {
        return orderSigner.signAppOrder(appOrder);
    }

    public WorkerpoolOrder signWorkerpoolOrder(WorkerpoolOrder workerpoolOrder) {
        return orderSigner.signWorkerpoolOrder(workerpoolOrder);
    }

    public DatasetOrder signDatasetOrder(DatasetOrder datasetOrder) {
        return orderSigner.signDatasetOrder(datasetOrder);
    }

    public RequestOrder signRequestOrder(RequestOrder requestOrder) {
        return orderSigner.signRequestOrder(requestOrder);
    }

}
