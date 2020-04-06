package org.lhyf.cloud.payment.service.impl;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;
import org.lhyf.cloud.payment.mapper.PaymentMapper;
import org.lhyf.cloud.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/****
 * @author YF
 * @date 2020-03-22 13:12
 * @desc PaymentServiceImpl
 *
 **/
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;


    @Override
    public int create(Payment payment) {
        return paymentMapper.create(payment);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentMapper.getPaymentById(id);
    }

    @HystrixCommand(fallbackMethod = "timeoutFallback", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "600")
    })
    @Override
    public RestResponseBo getTimeout() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return RestResponseBo.ok();
    }


    @Override
    public RestResponseBo timeoutFallback() {
        return RestResponseBo.fail("请求失败,这是服务端的默认响应");
    }
}
