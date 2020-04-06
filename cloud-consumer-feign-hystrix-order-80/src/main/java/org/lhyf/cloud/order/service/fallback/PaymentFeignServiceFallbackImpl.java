package org.lhyf.cloud.order.service.fallback;

import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;
import org.lhyf.cloud.order.service.PaymentFeignService;
import org.springframework.stereotype.Component;

/****
 * @author YF
 * @date 2020-04-05 22:57
 * @desc PaymentFeignServiceFallbackImpl
 *
 **/
@Component
public class PaymentFeignServiceFallbackImpl implements PaymentFeignService {
    @Override
    public RestResponseBo add(Payment payment) {
        return RestResponseBo.fail("服务调用失败");
    }

    @Override
    public RestResponseBo getPaymentById(Long id) {
        return RestResponseBo.fail("服务调用失败");
    }

    @Override
    public RestResponseBo getTimeOut() {
        return RestResponseBo.fail("服务调用失败");
    }
}
