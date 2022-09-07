package wiremockScratch;

import com.github.mizosoft.methanol.*;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
    void simple_get() throws Exception {
        wm.stubFor(get("/things").willReturn(ok("1")));

        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(wm.url("/things")))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.body(), is("1"));
    }

    @Test
    void multipart_post() throws Exception {
        String fileContent = UUID.randomUUID().toString();

        wm.stubFor(
                post("/things")
                        .withHeader("Content-Type", containing("multipart/form-data"))
                        .withMultipartRequestBody(
                                aMultipart()
                                        .withName("file")
                                        .withHeader("Content-Type", equalTo("application/octet-stream"))
                                        .withBody(binaryEqualTo(fileContent.getBytes())))
                        .withMultipartRequestBody(
                                aMultipart()
                                        .withHeader("Content-Type", equalTo("application/json"))
                                        .withName("metadata")
                                        .withBody(equalToJson("{ \"name\": \"some name\" }")))
                        .willReturn(ok("ok")));

        MultipartBodyPublisher multipartBodyPublisher = MultipartBodyPublisher.newBuilder()
                .mediaType(MediaType.of("multipart", "form-data"))
                .formPart(
                        "metadata",
                        MoreBodyPublishers.ofMediaType(
                                HttpRequest.BodyPublishers.ofString("{ \"name\": \"some name\" }"),
                                MediaType.APPLICATION_JSON))
                .formPart(
                        "file",
                        "filename.txt",
                        MoreBodyPublishers.ofMediaType(
                                HttpRequest.BodyPublishers.ofByteArray(fileContent.getBytes(StandardCharsets.UTF_8)),
                                MediaType.APPLICATION_OCTET_STREAM))
                .build();

        HttpResponse<String> response = Methanol.create()
                .send(
                        MutableRequest.POST(
                                wm.url("/things"),
                                multipartBodyPublisher),
                        HttpResponse.BodyHandlers.ofString());

        System.out.println(response);


        assertThat(response.body(), is("ok"));
    }
}
