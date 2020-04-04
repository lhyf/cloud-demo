package org.lhyf.cloud.payment.controller;

import lombok.extern.slf4j.Slf4j;
import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;
import org.lhyf.cloud.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/****
 * @author YF
 * @date 2020-03-22 13:15
 * @desc PaymentController
 *
 **/
@Slf4j
@RestController
@RequestMapping(value = "/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping(value = "/add")
    public RestResponseBo add(@RequestBody Payment payment) {
        int i = paymentService.create(payment);
        log.info("插入payment:{}", i);
        if (i > 0) {
            return RestResponseBo.ok(payment);
        }
        return RestResponseBo.fail();
    }

    @GetMapping("/get")
    public RestResponseBo getPaymentById(@RequestParam(value = "id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        if (payment == null) {
            return RestResponseBo.fail("对应记录不存在");
        }
        return RestResponseBo.ok(payment);
    }

}
