package org.lhyf.cloud.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/****
 * @author YF
 * @date 2020-03-22 12:45
 * @desc PaymentApplication
 *
 **/
@EnableEurekaClient
@MapperScan(basePackages = "org.lhyf.cloud.payment.mapper")
@SpringBootApplication
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class);
    }
}
