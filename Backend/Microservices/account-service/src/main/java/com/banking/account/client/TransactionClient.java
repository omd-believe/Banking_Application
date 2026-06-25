package com.banking.account.client;

import com.banking.account.dto.TransactionRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service")
public interface TransactionClient {

    @PostMapping("/transactions")
    void recordTransaction(@RequestBody TransactionRequestDto requestDto);
}