package net.backlogic.persistence.springboot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.backlogic.persistence.client.handler.ReturnType;
import net.backlogic.persistence.client.handler.ServiceHandler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

// TODO: To support reactive service
public class WebfluxServiceHandler  implements ServiceHandler {

    private final WebClient webClient;

    public WebfluxServiceHandler(String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(
                            new ObjectMapper()
                                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                                    .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                            MediaType.APPLICATION_JSON
                    ));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(
                            new ObjectMapper()
                                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
                            MediaType.APPLICATION_JSON
                    ));
                })
                .build();
    }

    @Override
    public Object invoke(String serviceUrl, Object serviceInput, ReturnType returnType, Class<?> elementType) {
        switch (returnType) {
            case LIST:
                return postForList(serviceUrl, serviceInput, elementType);
            default:
                return postForObject(serviceUrl, serviceInput, elementType);
        }
    }


    public Object postForObject(String url, Object input, Class<?> elementType) {
        // post
        WebClient.ResponseSpec resp = this.webClient.post()
                .uri(url)
                .bodyValue(input)
                .retrieve();

        if (resp == null) {
            return null;
        }

        Object object = resp
                .bodyToMono(elementType)
                .block();
        return object;
    }

    public List<Object> postForList(String url, Object input, Class<?> elementType) {
        WebClient.ResponseSpec resp = this.webClient.post()
                .uri(url)
                .bodyValue(input)
                .retrieve();

        if (resp == null) {
            return new LinkedList<>();
        }

        List<Object> list = (List<Object>) resp
                .bodyToFlux(elementType)
                .collectList()
                .block();

        return list;
    }

}
