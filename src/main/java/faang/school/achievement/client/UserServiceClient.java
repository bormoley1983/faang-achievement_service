package faang.school.achievement.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://${user-service.host}:${user-service.port}")
public interface UserServiceClient {
    
    //TODO: replacement of foreign key in db beacuse of total separation of domains on microservice layer
    @GetMapping("/users/{id}/exists")
    boolean userExists(@PathVariable("id") long userId);
}
