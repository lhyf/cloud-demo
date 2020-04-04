package org.lhyf.cloud.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/****
 * @author YF
 * @date 2020-03-22 14:23
 * @desc ContextConfig
 *
 **/
@Configuration
public class ContextConfig {

    /**
     * 使用Eureka 服务发现,需要使用 @LoadBalanced 标注
     * @return
     */
    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
