package com.expenseplatform.expense.client;

import com.expenseplatform.expense.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${auth-service.url}", configuration = FeignConfig.class)
public interface AuthServiceClient {

    @GetMapping("/auth/users/{id}")
    UserDetailsResponse getUserById(@PathVariable("id") Long id, @RequestHeader("Authorization") String authHeader);
}
