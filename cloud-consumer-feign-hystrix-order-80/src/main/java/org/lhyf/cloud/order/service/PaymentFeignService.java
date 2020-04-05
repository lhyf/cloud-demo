package org.lhyf.cloud.order.service;

import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/****
 * @author YF
 * @date 2020-04-05 10:11
 * @desc PaymentFeignService
 *
 **/
@Component
@FeignClient(value = "CLOUD-PROVIDER-PAYMENT")
@RequestMapping("/payment")
public interface PaymentFeignService {
    @PostMapping(value = "/add")
    RestResponseBo add(@RequestBody Payment payment);

    @GetMapping("/get")
    RestResponseBo getPaymentById(@RequestParam(value = "id") Long id);

    @GetMapping("/timeout")
    RestResponseBo getTimeOut();
}
