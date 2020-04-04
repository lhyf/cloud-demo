# Eureka

## 自我保护

**一旦进入保护模式,Eureka Server 将会尝试保护其服务注册表中的信息,不再删除服务注册表中的数据,也不就是不会注销任何微服务.**
某时刻,某一个微服务不可用了,Eureka不会立刻清理,依旧会对该微服务的信息进行保存,属于CAP里的AP分支

默认情况下,如果Eureka Server在一定时间内么有接收到某个微服务实例的心跳,Eureka Server将会注销该实例(默认90秒).
但是当网络分区故障发生(延时,卡顿,拥挤)时,微服务与Eureka Server之间无法进行正常通信,以上行为可能变得非常危险--因为微服务
本身其实是健康的,此时不应该注销这个微服务. Eureka通过 "自我保护" 来解决这个问题 -- 当Eureka Server节点在短时间内丢失
过多客户端时,那么这个节点就会进入自我保护模式.

## 消费者端使用Eureka作为服务发现

使用 @LoadBalanced 注解赋予RestTemplate负载均衡的能力
```java
    /**
     * 使用Eureka 服务发现,需要使用 @LoadBalanced 标注
     * @return
     */
    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
```

使用服务名调用,可以以轮询方式进行负载均衡
```java
    
    private static final String PAYMENT_URL = "http://CLOUD-PROVIDER-PAYMENT";

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/get")
    public RestResponseBo<Payment> getPaymentById(@RequestParam(value = "id") Long id) {
        return restTemplate.getForObject(PAYMENT_URL + "/payment/get?id=" + id, RestResponseBo.class);
    }
```

## actuator 微服务信息完善
