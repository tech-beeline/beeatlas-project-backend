/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.projectbackend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.beeline.projectbackend.dto.product.DiscoveredInterfaceResultDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredInterfaceUpsertDTO;
import ru.beeline.projectbackend.dto.product.DiscoveredOperationByIdsDTO;
import ru.beeline.projectbackend.exception.ExternalServiceException;

import java.util.List;

@Slf4j
@Component
public class ProductClient {

    private static final String PRODUCT_SERVICE_UNAVAILABLE = "Сервис продуктов недоступен";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductClient(@Qualifier("productRestTemplate") RestTemplate restTemplate,
                         @Value("${integration.product-server-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public List<DiscoveredInterfaceResultDTO> upsertUseCaseOperations(
            Integer useCaseId, List<DiscoveredInterfaceUpsertDTO> interfaces) {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/api/v1/use-case/operations")
                .queryParam("use-case-id", useCaseId)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("PUT {} — интерфейсов: {}", url, interfaces.size());
        try {
            List<DiscoveredInterfaceResultDTO> result = restTemplate.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(interfaces, headers),
                    new ParameterizedTypeReference<List<DiscoveredInterfaceResultDTO>>() {
                    }).getBody();
            if (result == null) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY,
                        "Сервис продуктов вернул пустой ответ на публикацию операций use case");
            }
            return result;
        } catch (RestClientResponseException e) {
            log.error("Ошибка вызова {}: статус {}, тело {}", url, e.getStatusCode(), e.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            throw new ExternalServiceException(
                    status == null || status.is5xxServerError() ? HttpStatus.BAD_GATEWAY : status,
                    "Ошибка сервиса продуктов при публикации операций use case: " + e.getResponseBodyAsString());
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка вызова {}: {}", url, e.getMessage());
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Сервис продуктов недоступен: " + e.getMessage());
        }
    }

    public List<DiscoveredOperationByIdsDTO> getDiscoveredOperationsByIds(List<Integer> ids) {
        String url = baseUrl + "/api/v1/discovered-operation/by-ids";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("POST {} — методов: {}", url, ids.size());
        try {
            List<DiscoveredOperationByIdsDTO> result = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(ids, headers),
                    new ParameterizedTypeReference<List<DiscoveredOperationByIdsDTO>>() {
                    }).getBody();
            if (result == null) {
                log.error("Пустой ответ {}", url);
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, PRODUCT_SERVICE_UNAVAILABLE);
            }
            return result;
        } catch (RestClientResponseException e) {
            log.error("Ошибка вызова {}: статус {}, тело {}", url, e.getStatusCode(), e.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == null || !status.is4xxClientError()) {
                throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, PRODUCT_SERVICE_UNAVAILABLE);
            }
            throw new ExternalServiceException(status, extractErrorMessage(e.getResponseBodyAsString()));
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка вызова {}: {}", url, e.getMessage());
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, PRODUCT_SERVICE_UNAVAILABLE);
        }
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return PRODUCT_SERVICE_UNAVAILABLE;
        }
        try {
            JsonNode errorMessage = OBJECT_MAPPER.readTree(body).get("errorMessage");
            if (errorMessage != null && errorMessage.isTextual()) {
                return errorMessage.asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }
}
