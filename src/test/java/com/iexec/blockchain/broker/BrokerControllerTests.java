/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

package com.iexec.blockchain.broker;

import com.iexec.common.sdk.broker.BrokerOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class BrokerControllerTests {

    @Mock
    private BrokerOrder brokerOrder;
    @Mock
    private BrokerService brokerService;

    @InjectMocks
    private BrokerController brokerController;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnBadRequestOnIllegalStateException(CapturedOutput output) {
        when(brokerService.matchOrders(brokerOrder)).thenThrow(IllegalStateException.class);
        assertThat(brokerController.matchOrders(brokerOrder))
                .isEqualTo(ResponseEntity.badRequest().build());
        assertThat(output.getOut()).contains("Match order failed with illegal state");
    }

    @Test
    void shouldReturnBadRequestOnNullPointerException(CapturedOutput output) {
        when(brokerService.matchOrders(brokerOrder)).thenThrow(NullPointerException.class);
        assertThat(brokerController.matchOrders(brokerOrder))
                .isEqualTo(ResponseEntity.badRequest().build());
        assertThat(output.getOut()).contains("Match order failed with null value");
    }

    @Test
    void shouldReturnBadRequestOnEmptyDealId() {
        when(brokerService.matchOrders(brokerOrder)).thenReturn("");
        assertThat(brokerController.matchOrders(brokerOrder))
                .isEqualTo(ResponseEntity.badRequest().build());
    }

    @Test
    void shouldReturnOk() {
        when(brokerService.matchOrders(brokerOrder)).thenReturn("dealId");
        assertThat(brokerController.matchOrders(brokerOrder))
                .isEqualTo(ResponseEntity.ok("dealId"));
    }

}
