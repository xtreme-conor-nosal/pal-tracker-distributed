package test.pivotal.pal.tracker;


import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.pivotal.pal.tracker.testsupport.TestScenarioSupport;
import okhttp3.Headers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.pivotal.pal.tracker.support.ApplicationServer;
import test.pivotal.pal.tracker.support.HttpClient;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static test.pivotal.pal.tracker.support.MapBuilder.jsonMapBuilder;

public class DeployedFlowTest {

    private final HttpClient httpClient = new HttpClient();

    private String registrationServerUrl(String path) {
        return "https://registration-pal-cn.apps.quandary.pal.pivotal.io" + path;
    }

    private String allocationsServerUrl(String path) {
        return "https://allocations-pal-cn.apps.quandary.pal.pivotal.io" + path;
    }

    private String backlogServerUrl(String path) {
        return "https://backlog-pal-cn.apps.quandary.pal.pivotal.io" + path;
    }

    private String timesheetsServerUrl(String path) {
        return "https://timesheets-pal-cn.apps.quandary.pal.pivotal.io" + path;
    }

    private String authUrl() {
        return "https://p-identity.login.sys.quandary.pal.pivotal.io/oauth/token";
    }

    private String getBasicAuthHeader() {
        String id = "0aba9516-5b49-47ed-850f-61433b4eeab8";
        String secret = "4fac007c-ec9e-445e-86f9-43ac247b973a";
        return new String(Base64.getEncoder().encode((id + ":" + secret).getBytes()));
    }

    private long findResponseId(HttpClient.Response response) {
        try {
            return JsonPath.parse(response.body).read("$.id", Long.class);
        } catch (PathNotFoundException e) {
            try {
                return JsonPath.parse(response.body).read("$[0].id", Long.class);
            } catch (PathNotFoundException e1) {
                fail("Could not find id in response body. Response was: \n" + response);
                return -1;
            }
        }
    }

    private String findAuthToken(HttpClient.Response response) {
        try {
            return JsonPath.parse(response.body).read("$.access_token", String.class);
        } catch (PathNotFoundException e) {
            try {
                return JsonPath.parse(response.body).read("$[0].access_token", String.class);
            } catch (PathNotFoundException e1) {
                fail("Could not find access_token in response body. Response was: \n" + response);
                return "";
            }
        }
    }

    @Before
    public void setup() throws Exception {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testBasicFlow() throws Exception {

        String name = UUID.randomUUID().toString();
        String project = UUID.randomUUID().toString();
        String story = UUID.randomUUID().toString();

        HttpClient.Response response;

        httpClient.setHeaders(new Headers.Builder()
                .add("Authorization: Basic " + getBasicAuthHeader())
                .add("Accept: application/json")
                .add("Content-Type: application/x-www-form-urlencoded").build());
        response = httpClient.post(authUrl(), "grant_type=client_credentials&response_type=token");

        String authToken = findAuthToken(response);
        httpClient.setHeaders(new Headers.Builder()
                        .add("Authorization: Bearer " + authToken).build());

        response = httpClient.get(registrationServerUrl("/"));
        assertThat(response.body).isEqualTo("Noop!");

        response = httpClient.post(registrationServerUrl("/registration"), jsonMapBuilder()
            .put("name", name)
            .build()
        );
        long createdUserId = findResponseId(response);
        assertThat(createdUserId).isGreaterThan(0);

        response = httpClient.get(registrationServerUrl("/users/" + createdUserId));
        assertThat(response.body).isNotNull().isNotEmpty();

        response = httpClient.get(registrationServerUrl("/accounts?ownerId=" + createdUserId));
        long createdAccountId = findResponseId(response);
        assertThat(createdAccountId).isGreaterThan(0);

        response = httpClient.post(registrationServerUrl("/projects"), jsonMapBuilder()
            .put("accountId", createdAccountId)
            .put("name", project)
            .build()
        );
        long createdProjectId = findResponseId(response);
        assertThat(createdProjectId).isGreaterThan(0);

        response = httpClient.get(registrationServerUrl("/projects?accountId=" + createdAccountId));
        assertThat(response.body).isNotNull().isNotEmpty();


        response = httpClient.get(allocationsServerUrl("/"));
        assertThat(response.body).isEqualTo("Noop!");

        response = httpClient.post(
            allocationsServerUrl("/allocations"), jsonMapBuilder()
                .put("projectId", createdProjectId)
                .put("userId", createdUserId)
                .put("firstDay", "2015-05-17")
                .put("lastDay", "2015-05-26")
                .build()
        );

        long createdAllocationId = findResponseId(response);
        assertThat(createdAllocationId).isGreaterThan(0);

        response = httpClient.get(allocationsServerUrl("/allocations?projectId=" + createdProjectId));
        assertThat(response.body).isNotNull().isNotEmpty();


        response = httpClient.get(backlogServerUrl("/"));
        assertThat(response.body).isEqualTo("Noop!");

        response = httpClient.post(backlogServerUrl("/stories"), jsonMapBuilder()
            .put("projectId", createdProjectId)
            .put("name", story)
            .build()
        );
        long createdStoryId = findResponseId(response);
        assertThat(createdStoryId).isGreaterThan(0);

        response = httpClient.get(backlogServerUrl("/stories?projectId" + createdProjectId));
        assertThat(response.body).isNotNull().isNotEmpty();


        response = httpClient.get(timesheetsServerUrl("/"));
        assertThat(response.body).isEqualTo("Noop!");

        response = httpClient.post(timesheetsServerUrl("/time-entries"), jsonMapBuilder()
            .put("projectId", createdProjectId)
            .put("userId", createdUserId)
            .put("date", "2015-12-17")
            .put("hours", 8)
            .build()
        );
        long createdTimeEntryId = findResponseId(response);
        assertThat(createdTimeEntryId).isGreaterThan(0);

        response = httpClient.get(timesheetsServerUrl("/time-entries?projectId" + createdProjectId));
        assertThat(response.body).isNotNull().isNotEmpty();
    }
}
