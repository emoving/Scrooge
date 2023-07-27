package com.scrooge.scrooge.controller;

import com.scrooge.scrooge.domain.PaymentHistory;
import com.scrooge.scrooge.domain.User;
import com.scrooge.scrooge.dto.PaymentHistoryDto;
import com.scrooge.scrooge.dto.SuccessResp;
import com.scrooge.scrooge.repository.UserRepository;
import com.scrooge.scrooge.service.PaymentHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name="PaymentHistory", description = "소비내역 API")
@RestController
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;
    private final UserRepository userRepository;

    @Operation(summary = "POST PaymentHistory", description = "소비내역 등록")
    @PostMapping("/payment-history/{userId}")
    public ResponseEntity<?> addPaymentHistory(@RequestBody PaymentHistoryDto paymentHistoryDto, @PathVariable("userId") Long userId) {
        Optional<User> user = userRepository.findById(userId);

        //User user = userService.getUserByUserseq(userSeq); 이런 코드 필요,,
        //User user =

        SuccessResp successResp = new SuccessResp(1);
        PaymentHistory paymentHistory = paymentHistoryService.addPaymentHistory(userId, paymentHistoryDto);
        return new ResponseEntity<>(successResp, HttpStatus.OK);
    }


    // user별 소비내역 조회하는 함수
    @GetMapping("/payment-history/{userId}")
    public ResponseEntity<List<PaymentHistoryDto>> selectPaymentHistory(@PathVariable("userId") Long userId) {
        List<PaymentHistoryDto> paymentHistoryDtos = paymentHistoryService.getPaymentHistoryByUserId(userId);
        return ResponseEntity.ok(paymentHistoryDtos);
    }



}
