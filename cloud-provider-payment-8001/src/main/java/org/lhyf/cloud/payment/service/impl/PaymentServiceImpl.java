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


    /**********服务熔断测试**********/
    /**
     * Hystrix在运行过程中会向每个commandKey对应的熔断器报告 成功、失败、超时和拒绝的状态，熔断器维护计算统计的数据，
     * 根据这些统计的信息来确定熔断器是否打开。如果打开，后续的请求都会被截断。
     * 然后会隔一段时间默认是5s，尝试半开，放入一部分流量请求进来，相当于对依赖服务进行一次健康检查，如果恢复，熔断器关闭，随后完全恢复调用。
     * @return
     */
    @HystrixCommand(fallbackMethod = "fallbackCircuitBreaker",commandProperties = {
            // 此属性确定断路器是否用于跟踪健康状况，以及在断路器跳闸时是否用于短路请求。
            @HystrixProperty(name = "circuitBreaker.enabled",value = "true"),
            // 熔断器强制打开，始终保持打开状态。默认值FLASE。
            @HystrixProperty(name = "circuitBreaker.forceOpen",value = "false"),
            // 熔断器强制关闭，始终保持关闭状态。默认值FLASE。
            @HystrixProperty(name = "circuitBreaker.forceClosed",value = "false"),
            // 此属性设置将跳闸电路的滚动窗口中的最小请求数。
            // 例如，如果这个值是20，那么在滚动窗口(比如一个10秒的窗口)中只接收到19个请求，即使所有19个请求都失败了，电路也不会跳闸打开。
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold",value = "10"),
            // 半开试探休眠时间，默认值5000ms。当熔断器开启一段时间之后比如5000ms，
            // 会尝试放过去一部分流量进行试探，确定依赖服务是否恢复。
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds",value = "10000"),
            // 设定错误百分比，默认值50%，例如一段时间（10s）内有100个请求，其中有55个超时或者异常返回了，
            // 那么这段时间内的错误百分比是55%，大于了默认值50%，这种情况下触发熔断器-打开。
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage",value = "60")
    })
    @Override
    public RestResponseBo paymentCircuitBreaker(int num) {
        if(num < 0){
            throw new IllegalArgumentException("参数不能小于0");
        }
        return RestResponseBo.ok("服务端的正常返回");
    }

    @Override
    public RestResponseBo fallbackCircuitBreaker(int num) {
        return RestResponseBo.fail("服务端熔断的默认返回");
    }
}
