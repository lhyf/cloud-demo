package org.lhyf.cloud.order.controller;

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
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
@DefaultProperties(defaultFallback = "globalFallback")
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

//    @HystrixCommand(fallbackMethod = "timeoutFallback", commandProperties = {
//            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "600")
//    })
    @HystrixCommand
    @GetMapping("/timeout/{num}/{name}")
    public RestResponseBo getTimeOut(@PathVariable("num") Integer num, @PathVariable("name") String name) {
        int i = 10 / num;
        System.out.println(name);
        RestResponseBo response = paymentService.getTimeOut();
        return response;
    }

    public RestResponseBo timeoutFallback(Integer num, String name) {
        return RestResponseBo.fail("请求失败,这是客户端的默认响应,入参: " + num + ":" + name);
    }

    public RestResponseBo globalFallback() {
        return RestResponseBo.fail("请求失败,这是客户端全局的默认响应");
    }
}