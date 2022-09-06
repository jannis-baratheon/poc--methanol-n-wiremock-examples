package wiremockScratch;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class LibraryTest {
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .proxyMode(true)
            .build();

    private HttpClient httpClient;

    @BeforeEach
    void init() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Test
    void configures_jvm_proxy_and_enables_browser_proxying() throws Exception {
        wm.stubFor(get("/things").willReturn(ok("1")));

        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(wm.url("/things")))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.body(), is("1"));
    }
}
