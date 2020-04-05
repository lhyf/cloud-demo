package org.lhyf.cloud.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/****
 * @author YF
 * @date 2020-03-22 12:45
 * @desc PaymentApplication
 *
 **/
@EnableCircuitBreaker
@EnableDiscoveryClient
@EnableEurekaClient
@MapperScan(basePackages = "org.lhyf.cloud.payment.mapper")
@SpringBootApplication
public class PaymentApplication2 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication2.class);
    }
}
