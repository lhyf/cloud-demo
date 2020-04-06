package org.lhyf.cloud.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

/****
 * @author YF
 * @date 2020-04-05 13:34
 * @desc HystrixOrderApplication
 *
 **/
//@EnableHystrix
@EnableCircuitBreaker
@EnableFeignClients
@SpringBootApplication
public class HystrixOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(HystrixOrderApplication.class);
    }
}
