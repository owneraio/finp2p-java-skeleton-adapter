package io.ownera.ledger.adapter.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.graphql.models.AssetDetails;
import io.ownera.ledger.adapter.graphql.models.AssetResponse;
import io.ownera.ledger.adapter.graphql.models.UserDetails;
import io.ownera.ledger.adapter.graphql.models.UserResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GraphqlClient {
    private final String url;

    public GraphqlClient(String url) {
        this.url = url;
    }

    public UserDetails getUserDetails(String publicKey) throws IOException, ItemNotFoundException {
        String query = "query ($publicKey: String!, $includeCert: Boolean!) {\\n" +
                "    users(filter: { key: \\\"finIds\\\", operator: CONTAINS, value: $publicKey }) {\\n" +
                "        nodes {\\n" +
                "            id\\n" +
                "            certificates @include(if: $includeCert){\\n" +
                "                nodes {\\n" +
                "                    type\\n" +
                "                }\\n" +
                "            }\\n" +
                "        }\\n" +
                "    }\\n" +
                "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("publicKey", publicKey);
        variables.put("includeCert", true);

        UserResponse response = callGraphQLService(url, query, variables, UserResponse.class);
        if (response == null || response.getData() == null
                || response.getData().getUsers() == null
                || response.getData().getUsers().getNodes().isEmpty()) {
            throw new ItemNotFoundException("User not found");
        }

        return response.getData().getUsers().getNodes().get(0);

    }

    public AssetDetails getAssetDetails(String assetId) throws IOException, ItemNotFoundException {
        String query = "query getAssets($assetId: String!){\\n" +
                "    assets(filter: { key: \\\"id\\\", operator: EQ, value: $assetId}) {\\n" +
                "        nodes {\\n" +
                "            id\\n" +
                "            config\\n" +
                "            regulationVerifiers {\\n" +
                "                id\\n" +
                "                name\\n" +
                "                provider\\n" +
                "            }\\n" +
                "        }\\n" +
                "    }\\n" +
                "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("assetId", assetId);

        AssetResponse response = callGraphQLService(url, query, variables, AssetResponse.class);
        if (response == null || response.getData() == null
                || response.getData().getAssets() == null
                || response.getData().getAssets().getNodes().isEmpty()) {
            throw new ItemNotFoundException("Asset not found");
        }

        return response.getData().getAssets().getNodes().get(0);

    }


    public static <T> T callGraphQLService(String url, String query, Map<String, Object> variables, Class<T> responseType) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/json");

            ObjectMapper objectMapper = new ObjectMapper();
            String variablesStr = objectMapper.writeValueAsString(variables);

            StringEntity entity = new StringEntity(String.format("{\"query\":\"%s\",\"variables\": %s}", query, variablesStr));
            httpPost.setEntity(entity);

            HttpResponse httpResponse = httpClient.execute(httpPost);

            return objectMapper.readValue(httpResponse.getEntity().getContent(), responseType);
        }
    }

}

