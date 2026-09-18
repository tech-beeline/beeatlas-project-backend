/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.projectbackend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.projectbackend.dto.UserProfileShortDTO;
import ru.beeline.projectbackend.exception.AuthServiceException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserClient {

    private final RestTemplate restTemplate;
    private final String userServerUrl;

    public UserClient(@Value("${integration.auth-server-url:}") String userServerUrl, RestTemplate restTemplate) {
        this.userServerUrl = userServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<UserProfileShortDTO> findUserProfiles(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Integer>> entity = new HttpEntity<>(userIds, headers);

            ResponseEntity<List<UserProfileShortDTO>> response = restTemplate.exchange(
                    userServerUrl + "/api/v1/user/list",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching user profiles: {}", e.getMessage());
            throw new AuthServiceException("Сервис авторизации недоступен");
        }
    }
}
