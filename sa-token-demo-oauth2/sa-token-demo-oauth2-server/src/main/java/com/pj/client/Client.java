package com.pj.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import com.google.gson.JsonObject;
import com.pj.Exception.UnexceptedResponseException;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.pj.api.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


/**
 * A client for interacting with the Superset API. This client provides methods
 * for exporting and importing dashboards, as well as retrieving a list of
 * available dashboards.
 */
public class Client {

    @Getter
    private String host;

    @Getter
    private int port;
    private String authToken;
    private String csrfToken;
    private CloseableHttpClient client;
    private String refreshToken;





    /**
     *
     * Creates a new Superset API client with the given credentials.
     *
     * @param host     The hostname of the Superset server.
     * @param port     The port number of the Superset server.
     * @param username The username to use when authenticating.
     * @param password The password to use when authenticating.
     * @throws Exception If there was an error creating the client.
     */
    public Client(String host, int port, String username, String password) throws Exception {
        this(host, port, username, password, null);
    }

    /**
     *
     * Creates a new Superset API client with the given credentials.
     *
     * @param host     The hostname of the Superset server.
     * @param port     The port number of the Superset server.
     * @param username The username to use when authenticating.
     * @param password The password to use when authenticating.
     * @param client   The underlying HttpClient to use.
     * @throws Exception If there was an error creating the client.
     */
    public Client(String host, int port, String username, String password, CloseableHttpClient client)
            throws Exception {
        if (client == null) {
            this.client = HttpClientBuilder.create().build();
        } else {
            this.client = client;
        }
        HttpUriRequest request = Api.getAuthTokenRequest(host, port, username, password);
        ApiResponse resp = executeRequest(request);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        String token = respBody.getAsJsonObject().get("access_token").getAsString();
        String refreshToken = respBody.getAsJsonObject().get("refresh_token").getAsString();
        this.authToken = token;
        this.refreshToken = refreshToken;
        this.host = host;
        this.port = port;
    }

    /**
     *
     * Returns a list of available dashboards.
     *
     * @return A JSON array containing a list of dashboards.
     * @throws URISyntaxException      If the Superset URL is invalid.
     * @throws ClientProtocolException If there was an error communicating with the
     *                                 Superset server.
     * @throws IOException             If there was an error communicating with the
     *                                 Superset server.
     */
    public JsonElement dashboards()
            throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = Api.geListDashboardsRequest(host, port, authToken);
        return getRespondJsonElement(request);
    }


    /**
     * Executes a SQL Lab request with the provided JSON body.
     *
     * @param body The JSON body to include in the SQL Lab Execute request. This should contain the SQL query to be executed.
     * @return A JsonElement representing the response from the server.
     * @throws URISyntaxException If the URI of the request is not correctly formatted.
     * @throws ClientProtocolException If there is an error in the HTTP protocol used for the request.
     * @throws IOException If there is an error when sending the request or receiving the response.
     */
    public JsonElement sqllabExecute(JsonObject body)
    throws URISyntaxException, ClientProtocolException, IOException{
        HttpUriRequest request = Api.getSqlLabExecuteRequest(host, port, authToken, body);
        return getRespondJsonElement(request);
    }

    public JsonElement exploreFormData(JsonObject body)
            throws URISyntaxException, ClientProtocolException, IOException{
        HttpUriRequest request = Api.getExploreFormDataRequest(host, port, authToken, body);
        return getRespondJsonElement(request);
    }

    public JsonElement permalink(JsonObject body)
            throws URISyntaxException, ClientProtocolException, IOException{
        HttpUriRequest request = Api.getPermalinkRequest(host, port, authToken,body);
        return getRespondJsonElement(request);
    }

    private JsonElement getRespondJsonElement(HttpUriRequest request) throws IOException {
        ApiResponse resp;
        try {
            resp = executeRequest(request);
        }catch (UnexceptedResponseException e){
            // 登录失败，尝试刷新token
            // 在bean中获取ApplicationContext
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            ClientTemplateImpl clientTemplate = context.getBean(ClientTemplateImpl.class);
            clientTemplate.refreshClient(this);
            resp = executeRequest(request);
        }
        return JsonParser.parseString(resp.getBody());
    }

    /**
     * Exports a dashboard with the specified ID and saves it to the given file.
     *
     * @param dashboardId the ID of the dashboard to export
     * @param destination the file to save the exported dashboard to
     * @return the downloaded file
     * @throws URISyntaxException      if there is an error in the URI syntax
     * @throws ClientProtocolException if there is an error in the HTTP protocol
     * @throws IOException             if there is an I/O error while sending or
     *                                 receiving the HTTP request/response
     */
    public File exportDashboard(int dashboardId, File destination)
            throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = Api.getExportDashboardRequest(host, port, authToken, dashboardId);

        File downloadFile = client.execute(request, new FileDownloadResponseHandler(destination));
        return downloadFile;
    }

    /**
     * Imports a dashboard from a file.
     *
     * @param dashboardFile the file containing the dashboard definition to import
     * @param password      A JSON format database password, e.g:
     *                      {\"databases/database.yaml\":\"password\"}
     * @param override      override exist dashboard
     * @throws ClientProtocolException if there was a problem with the HTTP protocol
     * @throws URISyntaxException      if there was a problem with the URI syntax
     * @throws IOException             if there was a problem with the I/O
     */
    public void importDashboard(File dashboardFile, JsonElement password, boolean override)
            throws ClientProtocolException, URISyntaxException, IOException {
        csrf();
        HttpUriRequest request = Api.getImportDashboardRequest(host, port, authToken, csrfToken, dashboardFile,
                override, password);
        executeRequest(request);
    }

    private void csrf() throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = Api.getCsrfTokenRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        this.csrfToken = respBody.getAsJsonObject().get("result").getAsString();
    }

    private ApiResponse executeRequest(HttpUriRequest request)
            throws ClientProtocolException, IOException, UnexceptedResponseException {
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (code >= 300 || code < 200) {
                // 尝试重新登录，刷新token
                throw new UnexceptedResponseException(request.getURI().toString(), code, bodyAsString);
            }
            return new ApiResponse(code, bodyAsString);
        }
    }

    @AllArgsConstructor
    private static class FileDownloadResponseHandler implements ResponseHandler<File> {
        private File target;

        @Override
        public File handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                throw new UnexceptedResponseException("", code, EntityUtils.toString(response.getEntity()));
            }
            try (InputStream source = response.getEntity().getContent()) {
                FileUtils.copyInputStreamToFile(source, this.target);
            }
            return this.target;
        }
    }

    /**
     * Refresh the client with new credentials.
     *
     * @param username The username to use when authenticating.
     * @param password The password to use when authenticating.
     * @throws Exception If there was an error creating the client.
     */
    public void refreshClientByUsernameAndPassword(String username, String password) throws IOException, URISyntaxException {
        // 先尝试用refreshToken刷新token
        if(this.refreshToken != null) {
            HttpUriRequest request = Api.getRefreshTokenRequest(host, port, this.refreshToken);
            try {
                ApiResponse resp = executeRequest(request);
                JsonElement respBody = JsonParser.parseString(resp.getBody());
                String token = respBody.getAsJsonObject().get("access_token").getAsString();
                this.authToken = token;
            } catch (UnexceptedResponseException e) {
                // refresh token 失败，尝试用用户名密码登录
                if(e.getCode() == 401 && e.getMessage().contains("Signature verification failed")){
                    if(this.client != null) {
                        this.client.close();
                    }
                    this.client = HttpClientBuilder.create().build();;
                    HttpUriRequest newRequest = Api.getAuthTokenRequest(host, port, username, password);
                    ApiResponse resp = executeRequest(newRequest);
                    JsonElement respBody = JsonParser.parseString(resp.getBody());
                    String token = respBody.getAsJsonObject().get("access_token").getAsString();
                    String refreshToken = respBody.getAsJsonObject().get("refresh_token").getAsString();
                    this.authToken = token;
                    this.refreshToken = refreshToken;
                }else {
                    throw e;
                }
            }
        }
    }


    @AllArgsConstructor
    @Data
    public static class ApiResponse {
        private int code;
        private String body;
    }

}
