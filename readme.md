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

### 服务名称修改

修改前

![](img\182004289411681.png)

修改后

![](img\1672040464105109.png)

引用actuator jar

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
```

修改 yml

```yaml
eureka:
  client:
    # 将自己注册进Eureka, 默认是true
    register-with-eureka: true
    # 是否从Eureka抓取已有的注册信息,默认为true,单节点无所谓,集群必须设置为true才能配合ribbon使用负载均衡
    fetch-registry: true
    service-url:
      # 单机注册
#      defaultZone: http://localhost:7001/eureka
      # 集群注册
      defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka

  instance:
    instance-id: payment8001 # 修改服务名称
```

### 访问信息有IP提示

```yaml
eureka:
  client:
    # 将自己注册进Eureka, 默认是true
    register-with-eureka: true
    # 是否从Eureka抓取已有的注册信息,默认为true,单节点无所谓,集群必须设置为true才能配合ribbon使用负载均衡
    fetch-registry: true
    service-url:
      # 单机注册
#      defaultZone: http://localhost:7001/eureka
      # 集群注册
      defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka

  instance:
    instance-id: payment8001 # 修改服务名称
    prefer-ip-address: true # 访问路径可以显示IP地址
```



## 服务发现

使用 DiscoveryClient 获取注册中心所有的服务列表

```java
@EnableDiscoveryClient // 开启服务发现
@EnableEurekaClient
@MapperScan(basePackages = "org.lhyf.cloud.payment.mapper")
@SpringBootApplication
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class);
    }
}
```

```java
    
    @Autowired
    private DiscoveryClient discoveryClient;

	@GetMapping("/discovery")
    public DiscoveryClient getDiscoveryClient() {

        List<String> services = discoveryClient.getServices();
        log.info("注册中心已经注册了的服务清单");
        for (String service : services) {
            log.info("service : {}", service);
        }

        // 获取某个服务名的实例
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PROVIDER-PAYMENT");
        log.info("CLOUD-PROVIDER-PAYMENT 服务的实例清单");
        for (ServiceInstance instance : instances) {
            log.info(instance.getServiceId() + "\t" + instance.getInstanceId() + "\t" + 
                     instance.getHost() + "\t" + instance.getPort() + "\t" + instance.getUri());
        }

        return this.discoveryClient;
    }
```

# Ribbon

**提供客户端的软件负载均衡算法(如简单轮询,随机连接)和服务调用.** 提供了一套完善的配置项,如连接超时,重试等. 

**Ribbon 本地负载均衡, 在调用微服务接口的时候, 会在注册中心上获取注册信息服务列表之后缓存到JVM本地, 从而在本地实现RPC远程调用技术.**

| 负载算法实现类                                     | 说明                                                         |
| -------------------------------------------------- | ------------------------------------------------------------ |
| com.netflix.loadbalancer.RoundRobinRule            | 轮询                                                         |
| com.netflix.loadbalancer.RandomRule                | 随机                                                         |
| com.netflix.loadbalancer.RetryRule                 | 先按照轮询的策略获取服务,如果获取服务失败则在指定时间内会进行重试,获取可用的服务 |
| com.netflix.loadbalancer.WeightedResponseTimeRule  | 对轮询的扩展,响应速度越快的实例选择权重越大, 越容易被选择    |
| com.netflix.loadbalancer.BestAvailableRule         | 会先过滤掉由于多次访问故障而处于断路器跳闸状态的服务, 然后选择一个并发量最小的服务 |
| com.netflix.loadbalancer.AvailabilityFilteringRule | 先过滤掉故障实例,再选择并发较小的实例                        |
| com.netflix.loadbalancer.ZoneAvoidanceRule         | 默认规则, 复合判断Server所在区域的性能和Server的可能性选择服务器 |

## 自定义负载算法

```java
/****
 * @author YF
 * @date 2020-04-04 19:00
 * @desc MyLoadBalanceRule
 * 自定义一个随机的负载均衡算法
 **/
public class MyLoadBalanceRule extends AbstractLoadBalancerRule {

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
    }

    @Override
    public Server choose(Object key) {
        ILoadBalancer loadBalancer = getLoadBalancer();
        if (loadBalancer == null) {
            return null;
        }
        Server server = null;
        while (server == null) {
            // 获取所有的服务列表, 包括可访问的和不可访问的
            List<Server> allServers = loadBalancer.getAllServers();
            // 获取所有可以访问到的服务列表
            List<Server> reachableServers = loadBalancer.getReachableServers();

            int serverCount = allServers.size();
            if (serverCount == 0) {
                return null;
            }
            int index = getServerIndex(serverCount);
            server = reachableServers.get(index);

            if (server == null) {
                Thread.yield();
                continue;
            }
            if (server.isAlive()) {
                return server;
            }

            server = null;
            Thread.yield();
        }
        return server;
    }


    /**
     * 随机产生一个服务器下标
     *
     * @param bound
     * @return
     */
    private int getServerIndex(int bound) {
        return new Random().nextInt(bound);
    }
}

```

```java
@EnableEurekaClient
@SpringBootApplication
// 对CLOUD-PROVIDER-PAYMENT 服务的访问,使用指定的负载均衡算法
@RibbonClient(name = "CLOUD-PROVIDER-PAYMENT",configuration = MyLoadBalanceRule.class)
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class);
    }
}
```



# Feign

feign 是一个声明式Web Service客户端. 使用Feign能让编写Web Service 客户端更加简单.

它的使用**方法是定义一个服务接口,然后在上面添加注解**. feign也支持可拔插式的编码器和解码器. Spring Cloud 对Feign进行了封装,使其支持了

Spring MVC 标准建注解和HttpMessageConverters. Feign 可以与Eureka和Ribbon 组合使用以支持负载均衡

Feign 集成了Ribbon

