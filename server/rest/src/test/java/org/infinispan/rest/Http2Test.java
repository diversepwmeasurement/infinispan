/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.infinispan.rest;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.Queue;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.ssl.OpenSsl;
import io.netty.util.CharsetUtil;
import org.assertj.core.api.Assertions;
import org.infinispan.commons.test.skip.SkipTestNG;
import org.infinispan.rest.helper.RestServerHelper;
import org.infinispan.rest.http2.NettyHttpClient;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.fwk.TestResourceTracker;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Most of the REST Server functionality is tested in {@link org.infinispan.rest.RestOperationsTest}. We can do that since
 * most of the implementation is exactly the same for both HTTP/1.1 and HTTP/2.0. Here we just do some basic sanity tests.
 *
 * @author Sebastian Łaskawiec
 */
@Test(groups = "functional", testName = "rest.Http2Test")
public final class Http2Test extends AbstractInfinispanTest {

    public static final String KEY_STORE_PATH = Http2Test.class.getClassLoader().getResource("./client.p12").getPath();

    private NettyHttpClient client;
    private RestServerHelper restServer;

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        if (restServer != null) {
            restServer.stop();
        }
        if (client != null) {
            client.stop();
        }
    }

    @Test
    public void shouldUpgradeUsingALPN() throws Exception {
        SkipTestNG.skipSinceJDK(10); // TODO: OpenSSL ALPN doesn't seem to work. Restructure the test to use internal JDK ALPN
        if (!OpenSsl.isAlpnSupported()) {
            throw new IllegalStateException("OpenSSL is not present, can not test TLS/ALPN support. Version: " + OpenSsl.versionString() + " Cause: " + OpenSsl.unavailabilityCause());
        }

        //given
        restServer = RestServerHelper.defaultRestServer("http2testcache")
              .withKeyStore(KEY_STORE_PATH, "secret", "pkcs12")
              .start(TestResourceTracker.getCurrentTestShortName());

        client = NettyHttpClient.newHttp2ClientWithALPN(KEY_STORE_PATH, "secret");
        client.start(restServer.getHost(), restServer.getPort());

        FullHttpRequest putValueInCacheRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, "/rest/http2testcache/test",
              wrappedBuffer("test".getBytes(CharsetUtil.UTF_8)));

        //when
        client.sendRequest(putValueInCacheRequest);
        Queue<FullHttpResponse> responses = client.getResponses();

        //then
        Assertions.assertThat(responses).hasSize(1);
        Assertions.assertThat(responses.element().status().code()).isEqualTo(200);
        Assertions.assertThat(restServer.getCacheManager().getCache("http2testcache").size()).isEqualTo(1);
    }

    @Test
    public void shouldUpgradeUsingHTTP11Upgrade() throws Exception {
        //given
        restServer = RestServerHelper.defaultRestServer("http2testcache").start(TestResourceTracker.getCurrentTestShortName());

        client = NettyHttpClient.newHttp2ClientWithHttp11Upgrade();
        client.start(restServer.getHost(), restServer.getPort());

        FullHttpRequest putValueInCacheRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, "/rest/http2testcache/test",
              wrappedBuffer("test".getBytes(CharsetUtil.UTF_8)));

        //when
        client.sendRequest(putValueInCacheRequest);
        Queue<FullHttpResponse> responses = client.getResponses();

        //then
        Assertions.assertThat(responses).hasSize(1);
        Assertions.assertThat(responses.element().status().code()).isEqualTo(200);
        Assertions.assertThat(restServer.getCacheManager().getCache("http2testcache").size()).isEqualTo(1);
    }
}
