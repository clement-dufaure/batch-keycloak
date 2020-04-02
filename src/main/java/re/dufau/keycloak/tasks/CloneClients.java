package re.dufau.keycloak.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@Component
@ConfigurationProperties("re.dufau.keycloak")
public class CloneClients implements Tasklet {

    public String url;
    public String adminUsername;
    public String adminPassword;
    public String realmImport;
    public String realmExport;
    public List<String> clientsToExclude;

    @Autowired
    private OkHttpClient httpClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String token = getBearer();
        HttpUrl httpUrlBase = HttpUrl.parse(url);
        TypeReference<List<HashMap<String, Object>>> typeListeClients = new TypeReference<List<HashMap<String, Object>>>() {
        };
        TypeReference<HashMap<String, Object>> typeCredentials = new TypeReference<HashMap<String, Object>>() {
        };

        HttpUrl urlClients = httpUrlBase.newBuilder().addPathSegment("admin").addPathSegment("realms")
                .addPathSegment(realmImport).addPathSegment("clients").build();
        Request requestClients = new Request.Builder().url(urlClients).header("Authorization", "Bearer " + token).get()
                .build();
        List<HashMap<String, Object>> clients = objectMapper
                .readValue(httpClient.newCall(requestClients).execute().body().string(), typeListeClients);
        clients = clients.stream().filter(c -> !clientsToExclude.contains(c.get("clientId")))
                .collect(Collectors.toList());

        for (HashMap<String, Object> client : clients) {
            // get secret
            HttpUrl urlSecret = httpUrlBase.newBuilder().addPathSegment("admin").addPathSegment("realms")
                    .addPathSegment(realmImport).addPathSegment("clients")
                    .addEncodedPathSegment((String) client.get("id")).addPathSegment("client-secret").build();
            Request requestSecret = new Request.Builder().url(urlSecret).header("Authorization", "Bearer " + token)
                    .get().build();
            HashMap<String, Object> credentials = objectMapper
                    .readValue(httpClient.newCall(requestSecret).execute().body().string(), typeCredentials);
            String secret = (String) credentials.get("value");

            // put secret and remove id
            client.put("secret", secret);
            client.remove("id");

            // post client to server
            RequestBody requestBody = RequestBody.create(
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(client),
                    MediaType.parse("application/json"));
            HttpUrl urlClientsExport = httpUrlBase.newBuilder().addPathSegment("admin").addPathSegment("realms")
                    .addPathSegment(realmExport).addPathSegment("clients").build();
            Request postClient = new Request.Builder().url(urlClientsExport).header("Authorization", "Bearer " + token)
                    .post(requestBody).build();
        }

        return RepeatStatus.FINISHED;
    }

    private String getBearer() throws IOException {
        HttpUrl tokenUrl = HttpUrl.parse(url).newBuilder().addPathSegment("realms").addPathSegment("master")
                .addPathSegment("protocol").addPathSegment("openid-connect").addPathSegment("token").build();
        RequestBody requestBody = new FormBody.Builder().add("username", adminUsername).add("password", adminPassword)
                .add("grant_type", "password").add("client_id", "admin-cli").build();
        Request r = new Request.Builder().url(tokenUrl).post(requestBody).build();
        String response = httpClient.newCall(r).execute().body().string();
        return objectMapper.readTree(response).get("access_token").asText();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getRealmImport() {
        return realmImport;
    }

    public void setRealmImport(String realmImport) {
        this.realmImport = realmImport;
    }

    public String getRealmExport() {
        return realmExport;
    }

    public void setRealmExport(String realmExport) {
        this.realmExport = realmExport;
    }

    public List<String> getClientsToExclude() {
        return clientsToExclude;
    }

    public void setClientsToExclude(List<String> clientsToExclude) {
        this.clientsToExclude = clientsToExclude;
    }

}