package nz.ac.auckland.grocerfy.util;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Optional;

public class HttpUtils {
    private static final String[] GENERIC_HEADERS = {
        "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36",
        "Accept-Language", "en-US",
        "Sec-Ch-Ua", "\"Chromium\";v=\"154\", \"Google Chrome\";v=\"154\", \"Not A(Brand\";v=\"99\"",
        "Sec-Ch-Ua-Mobile", "?0",
        "Sec-Ch-Ua-Platform", "\"Windows\""
    };

    private static final String[] GET_HEADERS = {
        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Sec-Fetch-Dest", "document",
        "Sec-Fetch-Mode", "navigate",
        "Sec-Fetch-Site", "none"
    };

    private static final String[] POST_HEADERS = {
        "Accept", "*/*",
        "Sec-Fetch-Dest", "empty",
        "Sec-Fetch-Mode", "cors",
        "Content-Type", "application/json"
    };

    private static final CookieManager cookieManager = new CookieManager();
    private static final HttpClient client;

    static {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    private HttpUtils() {}

    public static <T> Optional<HttpResponse<T>> sendHttpRequest(HttpRequest request, BodyHandler<T> bodyHandler) {
        try {
            HttpResponse<T> response = client.send(request, bodyHandler);
            if (response.statusCode() >= 400) {
                System.err.println("This link has returned an error code " + response.statusCode() + ": " + request.uri());
            }
            return Optional.of(response);
        } catch (InterruptedException exc) {
            System.err.println("Caught interrupt from GET " + request.uri().toString() + ", no data returned.");
            Thread.currentThread().interrupt();
        } catch (IOException exc) {
            System.err.println("Caught IO error from GET " + request.uri().toString() + ", no data returned.");
        }
        return Optional.empty();
    }

    public static String[] getGenericHeaders() {
        return GENERIC_HEADERS;
    }

    public static String[] getGetHeaders() {
        return GET_HEADERS;
    }

    public static String[] getPostHeaders() {
        return POST_HEADERS;
    }
}
