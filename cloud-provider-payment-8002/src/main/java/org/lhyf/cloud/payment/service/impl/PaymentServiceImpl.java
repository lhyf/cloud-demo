package org.lhyf.cloud.payment.service.impl;

import org.lhyf.cloud.entity.Payment;
import org.lhyf.cloud.payment.mapper.PaymentMapper;
import org.lhyf.cloud.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
