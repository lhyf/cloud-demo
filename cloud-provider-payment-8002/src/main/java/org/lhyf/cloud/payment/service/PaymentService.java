package org.lhyf.cloud.payment.service;


import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.entity.RestResponseBo;

/****
 * @author YF
 * @date 2020-03-22 13:11
 * @desc PaymentService
 *
 **/
public interface PaymentService {
    int create(Payment payment);

    Payment getPaymentById(Long id);

    RestResponseBo getTimeout();

    RestResponseBo timeoutFallback();

}
