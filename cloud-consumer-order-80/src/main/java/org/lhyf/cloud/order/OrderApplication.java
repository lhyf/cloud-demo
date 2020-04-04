package org.lhyf.cloud.order;

import org.lhyf.cloud.order.rule.MyLoadBalanceRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;

/****
 * @author YF
 * @date 2020-03-22 14:18
 * @desc OrderApplication
 *
 **/
@EnableEurekaClient
@SpringBootApplication
@RibbonClient(name = "CLOUD-PROVIDER-PAYMENT",configuration = MyLoadBalanceRule.class)
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class);
    }
}
