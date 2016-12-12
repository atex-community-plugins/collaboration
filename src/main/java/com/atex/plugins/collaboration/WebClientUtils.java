package com.atex.plugins.collaboration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

/**
 * Simple web client.
 *
 * @author mnova
 */
public class WebClientUtils {

    private static final Logger LOGGER = Logger.getLogger(WebClientUtils.class.getName());

    private String baseUrl;

    public WebClientUtils(final String baseUrl) {
        this.baseUrl = checkNotNull(baseUrl);
    }

    public Response publish(final MessagePayload message) {

        LOGGER.log(Level.FINE, "publish " + checkNotNull(message));

        final long startTime = System.nanoTime();
        try {
            final Client client = createClient();
            WebResource target = client
                    .resource(baseUrl);

            final ClientResponse response = target
                    .type(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, message);
            return clientResponseToResponse(response);
        } finally {
            final long endTime = System.nanoTime();
            final long ms = TimeUnit.SECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
            LOGGER.fine("publish request terminated in " + ms + " ms");
        }
    }

    public boolean isSuccess(final Response response) {
        return (response != null) && (response.getStatus() == 200);
    }

    Client createClient() {
        final ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        return Client.create(clientConfig);
    }

    private Response clientResponseToResponse(final ClientResponse r) {
        // copy the status code
        final ResponseBuilder rb = Response.status(r.getStatus());
        // copy all the headers
        for (Entry<String, List<String>> entry : r.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                rb.header(entry.getKey(), value);
            }
        }
        // copy the entity
        rb.entity(r.getEntityInputStream());
        // return the response
        return rb.build();
    }

}
