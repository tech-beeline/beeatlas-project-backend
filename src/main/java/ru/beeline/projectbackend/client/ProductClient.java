/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.projectbackend.client;

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
import ru.beeline.projectbackend.exception.ExternalServiceException;

import java.util.List;

@Slf4j
@Component
public class ProductClient {

    private static final String SOURCE_PROJECT_TASK = "ProjectTask";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductClient(@Qualifier("productRestTemplate") RestTemplate restTemplate,
                         @Value("${integration.product-server-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public List<DiscoveredInterfaceResultDTO> upsertProjectDiscoveredInterfaces(
            Integer projectId, List<DiscoveredInterfaceUpsertDTO> interfaces) {

        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/api/v1/discovered-interface/project/" + projectId)
                .queryParam("source", SOURCE_PROJECT_TASK)
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
                        "Сервис продуктов вернул пустой ответ на публикацию интерфейсов use case");
            }
            return result;
        } catch (RestClientResponseException e) {
            log.error("Ошибка вызова {}: статус {}, тело {}", url, e.getStatusCode(), e.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            throw new ExternalServiceException(
                    status == null || status.is5xxServerError() ? HttpStatus.BAD_GATEWAY : status,
                    "Ошибка сервиса продуктов при публикации интерфейсов use case: " + e.getResponseBodyAsString());
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка вызова {}: {}", url, e.getMessage());
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Сервис продуктов недоступен: " + e.getMessage());
        }
    }
}
