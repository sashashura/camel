/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jetty.async;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jetty.BaseJettyTest;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CAMEL-4795, there should be no exceptions in the logs (before the fix there was a NPE)
 */
public class JettyAsyncThrottleTest extends BaseJettyTest {

    private static final Logger LOG = LoggerFactory.getLogger(JettyAsyncThrottleTest.class);

    @RegisterExtension
    protected AvailablePortFinder.Port port3 = AvailablePortFinder.find();

    @Test
    public void testJettyAsync() throws Exception {
        getMockEndpoint("mock:result").expectedMinimumMessageCount(1);

        template.asyncRequestBody("http://localhost:{{port}}/myservice", null);
        template.asyncRequestBody("http://localhost:{{port}}/myservice", null);
        template.asyncRequestBody("http://localhost:{{port}}/myservice", null);
        template.asyncRequestBody("http://localhost:{{port}}/myservice", null);
        template.asyncRequestBody("http://localhost:{{port}}/myservice", null);

        assertMockEndpointsSatisfied();
        int size = getMockEndpoint("mock:result").getReceivedExchanges().size();

        for (int i = 0; i < size; i++) {
            Exchange exchange = getMockEndpoint("mock:result").getReceivedExchanges().get(i);
            LOG.info("Reply " + exchange);
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("jetty:http://localhost:{{port}}/myservice").removeHeaders("*").throttle(2).asyncDelayed().loadBalance()
                        .failover().to("http://localhost:" + port2 + "/foo")
                        .to("http://localhost:" + port3 + "/bar").end().to("mock:result");

                from("jetty:http://localhost:" + port2 + "/foo").transform().constant("foo").to("mock:foo");

                from("jetty:http://localhost:" + port3 + "/bar").transform().constant("bar").to("mock:bar");
            }
        };
    }
}
