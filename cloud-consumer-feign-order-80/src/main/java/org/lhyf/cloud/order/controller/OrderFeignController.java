package org.lhyf.cloud.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;
import org.lhyf.cloud.order.service.PaymentFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/****
 * @author YF
 * @date 2020-04-05 10:25
 * @desc OrderFeignController
 *
 **/
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderFeignController {
    @Autowired
    private PaymentFeignService paymentService;

    @PostMapping("/add")
    public RestResponseBo<Payment> add(@RequestBody Payment payment) {
        RestResponseBo result = paymentService.add(payment);
        return result;
    }

    @GetMapping("/get")
    public RestResponseBo<Payment> getPaymentById(@RequestParam(value = "id") Long id) {
        RestResponseBo response = paymentService.getPaymentById(id);
        return response;
    }
}