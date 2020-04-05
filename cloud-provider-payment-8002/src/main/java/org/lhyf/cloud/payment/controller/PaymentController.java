package org.lhyf.cloud.payment.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import lombok.extern.slf4j.Slf4j;
import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;
import org.lhyf.cloud.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private DiscoveryClient discoveryClient;

    @Value("${server.port}")
    private String port;

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
        payment.setSerial(payment.getSerial() + " : " + port);
        if (payment == null) {
            return RestResponseBo.fail("对应记录不存在");
        }
        return RestResponseBo.ok(payment);
    }


    @GetMapping("/timeout")
    public RestResponseBo getTimeOut() {
        return paymentService.getTimeout();
    }


    @GetMapping("/discovery")
    public DiscoveryClient getDiscoveryClient() {

        List<String> services = discoveryClient.getServices();
        log.info("注册中心已经注册了的服务清单");
        for (String service : services) {
            log.info("service : {}", service);
        }

        // 获取某个服务名的实例
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PROVIDER-PAYMENT");
        log.info("CLOUD-PROVIDER-PAYMENT 服务的实例清单");
        for (ServiceInstance instance : instances) {
            log.info(instance.getServiceId() + "\t" + instance.getInstanceId() + "\t" + instance.getHost() + "\t" + instance.getPort() + "\t" + instance.getUri());
        }

        return this.discoveryClient;
    }

}
