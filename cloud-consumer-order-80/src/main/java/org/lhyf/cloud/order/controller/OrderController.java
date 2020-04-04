package org.lhyf.cloud.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/****
 * @author YF
 * @date 2020-03-22 14:20
 * @desc OrderController
 *
 **/
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    private static final String PAYMENT_URL = "http://127.0.0.1:8001";

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/add")
    public RestResponseBo<Payment> add(@RequestBody Payment payment) {
        RestResponseBo result = restTemplate.postForObject(PAYMENT_URL + "/payment/add", payment, RestResponseBo.class);
        return result;
    }

    @GetMapping("/get")
    public RestResponseBo<Payment> getPaymentById(@RequestParam(value = "id") Long id) {
        return restTemplate.getForObject(PAYMENT_URL + "/payment/get?id=" + id, RestResponseBo.class);
    }
}
