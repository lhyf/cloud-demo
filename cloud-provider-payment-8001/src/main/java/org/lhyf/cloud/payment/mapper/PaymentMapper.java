package org.lhyf.cloud.payment.mapper;


import org.lhyf.cloud.entity.Payment;

/****
 * @author YF
 * @date 2020-03-22 13:01
 * @desc PaymentMapper
 *
 **/
public interface PaymentMapper {
    int create(Payment payment);

    Payment getPaymentById(Long id);

}

