package org.lhyf.cloud.payment.service;


import org.lhyf.cloud.entity.Payment;

/****
 * @author YF
 * @date 2020-03-22 13:11
 * @desc PaymentService
 *
 **/
public interface PaymentService {
    int create(Payment payment);

    Payment getPaymentById(Long id);

}
