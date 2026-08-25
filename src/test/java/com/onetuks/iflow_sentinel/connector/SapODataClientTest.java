package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.component.OAuth2TokenProvider;
import com.onetuks.iflow_sentinel.connector.component.SapODataClient;
import com.onetuks.iflow_sentinel.connector.domain.tenant.Tenant;
import com.onetuks.iflow_sentinel.connector.dto.ODataCollectionResponse;
import com.onetuks.iflow_sentinel.connector.dto.SapPackageDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SapODataClientTest {

        private static final String ODATA_URL = "https://tenant.example.com/api/v1";
        private static final String TOKEN_URL = "https://tenant.example.com/oauth/token";

        private MockRestServiceServer mockServer;
        private SapODataClient odataClient;
        private Tenant tenant;

        @BeforeEach
        void setUp() {
                RestClient.Builder builder = RestClient.builder();
                mockServer = MockRestServiceServer.bindTo(builder).build();
                RestClient testRestClient = builder.build();
                OAuth2TokenProvider tokenProvider = new OAuth2TokenProvider(testRestClient);
                odataClient = new SapODataClient(tokenProvider, testRestClient);
                tenant = TenantTestFixtures.tenant(1L, ODATA_URL, TOKEN_URL);
        }

        @Test
        void getCollectionFetchesTokenThenCallsODataWithBearerAuth() {
                mockServer.expect(requestTo(TOKEN_URL))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess(
                                                "{\"access_token\":\"tok-abc\",\"expires_in\":3600,\"token_type\":\"bearer\"}",
                                                MediaType.APPLICATION_JSON));
                mockServer.expect(requestTo(ODATA_URL + "/IntegrationPackages"))
                                .andExpect(method(HttpMethod.GET))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-abc"))
                                .andRespond(withSuccess(
                                                "{\"d\":{\"results\":[{\"Id\":\"PKG1\",\"Name\":\"Package One\"}]}}",
                                                MediaType.APPLICATION_JSON));

                List<SapPackageDto> packages = odataClient.getCollection(
                                tenant, "/IntegrationPackages",
                                new ParameterizedTypeReference<ODataCollectionResponse<SapPackageDto>>() {
                                });

                assertThat(packages).hasSize(1);
                assertThat(packages.get(0).Id()).isEqualTo("PKG1");
                assertThat(packages.get(0).Name()).isEqualTo("Package One");
                mockServer.verify();
        }

        @Test
        void getBinaryReturnsRawBytes() {
                mockServer.expect(requestTo(TOKEN_URL))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess(
                                                "{\"access_token\":\"tok-abc\",\"expires_in\":3600,\"token_type\":\"bearer\"}",
                                                MediaType.APPLICATION_JSON));
                mockServer.expect(requestTo(
                                ODATA_URL + "/IntegrationDesigntimeArtifacts(Id='ART1',Version='1.0.0')/$value"))
                                .andExpect(method(HttpMethod.GET))
                                .andRespond(withSuccess(new byte[] { 1, 2, 3, 4 }, MediaType.APPLICATION_OCTET_STREAM));

                byte[] result = odataClient.getBinary(tenant,
                                "/IntegrationDesigntimeArtifacts(Id='ART1',Version='1.0.0')/$value");

                assertThat(result).containsExactly(1, 2, 3, 4);
                mockServer.verify();
        }

        @Test
        void executeActionFetchesCsrfTokenThenSendsMutatingRequestWithTokenAndCookie() {
                mockServer.expect(requestTo(TOKEN_URL))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess(
                                                "{\"access_token\":\"tok-abc\",\"expires_in\":3600,\"token_type\":\"bearer\"}",
                                                MediaType.APPLICATION_JSON));
                mockServer.expect(requestTo(ODATA_URL))
                                .andExpect(method(HttpMethod.GET))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-abc"))
                                .andExpect(header("X-CSRF-Token", "Fetch"))
                                .andRespond(withSuccess()
                                                .header("X-CSRF-Token", "csrf-xyz")
                                                .header(HttpHeaders.SET_COOKIE, "JSESSIONID=abc123; Path=/; HttpOnly"));
                mockServer.expect(requestTo(ODATA_URL + "/DeployIntegrationDesigntimeArtifact?Id='ART1'&Version='active'"))
                                .andExpect(method(HttpMethod.POST))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-abc"))
                                .andExpect(header("X-CSRF-Token", "csrf-xyz"))
                                .andExpect(header(HttpHeaders.COOKIE, "JSESSIONID=abc123"))
                                .andRespond(withSuccess());

                odataClient.executeAction(tenant, HttpMethod.POST,
                                "/DeployIntegrationDesigntimeArtifact?Id='ART1'&Version='active'");

                mockServer.verify();
        }

        @Test
        void executeActionSupportsDeleteMethod() {
                mockServer.expect(requestTo(TOKEN_URL))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess(
                                                "{\"access_token\":\"tok-abc\",\"expires_in\":3600,\"token_type\":\"bearer\"}",
                                                MediaType.APPLICATION_JSON));
                mockServer.expect(requestTo(ODATA_URL))
                                .andExpect(method(HttpMethod.GET))
                                .andExpect(header("X-CSRF-Token", "Fetch"))
                                .andRespond(withSuccess()
                                                .header("X-CSRF-Token", "csrf-xyz")
                                                .header(HttpHeaders.SET_COOKIE, "JSESSIONID=abc123; Path=/; HttpOnly"));
                mockServer.expect(requestTo(ODATA_URL + "/IntegrationRuntimeArtifacts('ART1')"))
                                .andExpect(method(HttpMethod.DELETE))
                                .andExpect(header("X-CSRF-Token", "csrf-xyz"))
                                .andExpect(header(HttpHeaders.COOKIE, "JSESSIONID=abc123"))
                                .andRespond(withSuccess());

                odataClient.executeAction(tenant, HttpMethod.DELETE, "/IntegrationRuntimeArtifacts('ART1')");

                mockServer.verify();
        }

        @Test
        void callInterfaceEndpointUsesBasicAuthWhenConfigured() {
                // Basic Auth 헤더: iflow-user:iflow-password -> aWZsb3ctdXNlcjppZmxvdy1wYXNzd29yZA==
                String expectedBasic = "Basic " + java.util.Base64.getEncoder().encodeToString("iflow-user:iflow-password".getBytes(java.nio.charset.StandardCharsets.UTF_8));

                mockServer.expect(requestTo("https://test-rt.cfapps.eu10.hana.ondemand.com/http/my-iflow"))
                                .andExpect(method(HttpMethod.POST))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, expectedBasic))
                                .andExpect(header(HttpHeaders.CONTENT_TYPE, "application/json"))
                                .andRespond(withSuccess("{\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

                org.springframework.http.ResponseEntity<String> response = odataClient.callInterfaceEndpoint(
                                tenant, "/http/my-iflow", "{\"data\":\"test\"}", "application/json"
                );

                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody()).contains("SUCCESS");
                mockServer.verify();
        }

        @Test
        void getServiceEndpointsFetchesCollectionWithBearerAuth() {
                mockServer.expect(requestTo(TOKEN_URL))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess(
                                                "{\"access_token\":\"tok-abc\",\"expires_in\":3600,\"token_type\":\"bearer\"}",
                                                MediaType.APPLICATION_JSON));
                mockServer.expect(requestTo(ODATA_URL + "/ServiceEndpoints"))
                                .andExpect(method(HttpMethod.GET))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-abc"))
                                .andRespond(withSuccess(
                                                "{\"d\":{\"results\":[{\"Name\":\"IFLOW_1\",\"Url\":\"https://tenant.example.com/http/iflow1\",\"Protocol\":\"HTTPS\"}]}}",
                                                MediaType.APPLICATION_JSON));

                List<com.onetuks.iflow_sentinel.reprocess.dto.SapServiceEndpointDto> endpoints = odataClient.getServiceEndpoints(tenant);

                assertThat(endpoints).hasSize(1);
                assertThat(endpoints.get(0).Name()).isEqualTo("IFLOW_1");
                assertThat(endpoints.get(0).Url()).isEqualTo("https://tenant.example.com/http/iflow1");
                mockServer.verify();
        }
}
