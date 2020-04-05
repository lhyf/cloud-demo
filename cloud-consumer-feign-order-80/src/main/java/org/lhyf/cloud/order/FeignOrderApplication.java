package org.lhyf.cloud.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/****
 * @author YF
 * @date 2020-04-05 09:55
 * @desc FeignOrderApplication
 *
 **/
@EnableFeignClients
@SpringBootApplication
public class FeignOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeignOrderApplication.class);
    }
}
