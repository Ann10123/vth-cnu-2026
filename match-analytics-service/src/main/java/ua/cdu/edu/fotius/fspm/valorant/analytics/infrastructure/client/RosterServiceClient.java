package ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client;

import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client("${roster.service.url:`http://localhost:8080`}")
public interface RosterServiceClient {
    @Post("/api/v1/rosters/{id}/archive")
    void archiveRoster(String id);
}
