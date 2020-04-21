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

![](https://raw.githubusercontent.com/lhyf/cloud-demo/master/img/1672040464105109.png)

修改后

![](https://raw.githubusercontent.com/lhyf/cloud-demo/master/img/182004289411681.png)

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

## Ribbon 基础使用

**getForEntity**

```java
// 方式一
// 使用 "didi" 参数替换url中的{1} 占位符
    restTemplate.getForEntity("http://HELLO-SERVICE/hello1?name={1}", String.class, "didi")

// 方式二
    Map<String, String> params = new HashMap<>();
    params.put("name", "dada");
    restTemplate.getForEntity("http://HELLO-SERVICE/hello1?name={name}", String.class, params)
// 方式三
   UriComponents uriComponents = UriComponentsBuilder.fromUriString(
        "http://HELLO-SERVICE/hello1?name={name}")
        .build()
        .expand("dodo")
        .encode();
    URI uri = uriComponents.toUri();
    restTemplate.getForEntity(uri, String.class);
```



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

## 使用步骤

引入maven 依赖

```xml
<!--feign-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

主启动类开启feign client 使用
```java
@EnableFeignClients
@SpringBootApplication
public class FeignOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeignOrderApplication.class);
    }
}
```

编写调用接口,并使用@FeignClient 修饰

```java
@Component
@FeignClient(value = "CLOUD-PROVIDER-PAYMENT")
@RequestMapping("/payment")
public interface PaymentFeignService {
    @PostMapping(value = "/add")
    RestResponseBo add(@RequestBody Payment payment);

    @GetMapping("/get")
    RestResponseBo getPaymentById(@RequestParam(value = "id") Long id);
}

```

服务端(服务名:CLOUD-PROVIDER-PAYMENT)

```java
@Slf4j
@RestController
@RequestMapping(value = "/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Value("${server.port}")
    private String port;

    @PostMapping(value = "/add")
    public RestResponseBo add(@RequestBody Payment payment) {
        int i = paymentService.create(payment);
        log.info("插入payment:{}", i);
        if (i > 0) {
            return RestResponseBo.ok(payment);
        }
        return RestResponseBo.fail();
    }

    @GetMapping("/get")
    public RestResponseBo getPaymentById(@RequestParam(value = "id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        payment.setSerial(payment.getSerial() + " : " + port);
        if (payment == null) {
            return RestResponseBo.fail("对应记录不存在");
        }
        return RestResponseBo.ok(payment);
    }
}
```

客户端使用

```java
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderFeignController {
    @Autowired
    private PaymentFeignService paymentService;

    @PostMapping("/add")
    public RestResponseBo<Payment> add(@RequestBody Payment payment) {
        RestResponseBo result = paymentService.add(payment);
        return result;
    }

    @GetMapping("/get")
    public RestResponseBo<Payment> getPaymentById(@RequestParam(value = "id") Long id) {
        RestResponseBo response = paymentService.getPaymentById(id);
        return response;
    }
}
```

## @FeignClient 注解参数说明



## Feign超时控制

OpenFeign 默认等待1秒钟, 超过后报错

```yaml

#全局配置
ribbon: #设置feign 客户端超时时间(feign 底层使用的是ribbon)
  ConnectTimeout: 5000 #请求连接的超时时间 默认的时间为 1 秒
  ReadTimeout: 5000 #请求处理的超时时间

# 针对于 cloud-provider-payment 服务的配置
cloud-provider-payment:
  ribbon:
    OkToRetryOnAllOperations: true #对当前服务所有操作请求都进行重试
    MaxAutoRetries: 2 #对当前服务的重试次数
    MaxAutoRetriesNextServer: 0 # 切换实例的重试次数
    ConnectTimeout: 1200 #请求连接的超时时间
    ReadTimeout: 1300 #请求处理的超时时间
```

## Feign日志打印功能

feign提供了日志打印功能, 我们可以通过配置来调整日志级别, 从而了解Feign 中Http 请求细节,

### 日志级别

- NONE: 默认的, 不显示任何日志
- BASIC: 仅记录请求方法,URL,响应状态码及执行时间
- HEADERS: 除BASIC中定义的信息外,还有请求和响应的头信息
- FULL: 除了HEADERS中定义的信息外,还有请求和响应的正文及元数据

### 配置步骤

```java
// 添加feign日志配置类
@Configuration
public class FeignConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
```

```yaml
# 为对应的接口开启debug级别日志
logging:
  level:
    org.lhyf.cloud.order.service.PaymentFeignService: debug
```

```
---> GET http://CLOUD-PROVIDER-PAYMENT/payment/get?id=31 HTTP/1.1
---> END HTTP (0-byte body)
<--- HTTP/1.1 200 (3ms)
connection: keep-alive
content-type: application/json
date: Sun, 05 Apr 2020 03:28:42 GMT
keep-alive: timeout=60
transfer-encoding: chunked
{"payload":{"id":31,"serial":"111 : 8001"},"success":true,"msg":null,"code":-1,"timestamp":1586057322}
<--- END HTTP (111-byte body)
```

# 服务熔断与降级

## Hystrix

Hystrix是一个用于处理分布式系统的延迟和容错的开源库, 在分布式系统里, 许多依赖不可避免的会调用失败, 比如超时, 异常等, Hystrix能保证在一个依赖出问题的情况下, 不会导致整体服务失败, 避免级联故障, 以提高分布式系统的弹性. **向调用方返回一个符合预期的, 可处理的备选响应(Fallback), 而不是长时间的等待或者抛出调用方无法处理的异常, 这样就保证了服务调用方的线程不会被长时间, 不必要地占用,** 从而避免故障在分布式系统中的蔓延, 乃至雪崩.

### 服务降级

**服务降级，**当服务器压力剧增的情况下，根据当前业务情况及流量对一些服务和页面有策略的降级，以此释放服务器资源以保证核心任务的正常运行。降级：是利用有限资源，保障系统核心功能高可用、有损的架构方法。有限资源；核心高可用；有损；架构方法。

**自动降级方式有哪些？**
超时降级 —— 主要配置好超时时间和超时重试次数和机制，并使用异步机制探测恢复情况

失败次数降级 —— 主要是一些不稳定的API，当失败调用次数达到一定阀值自动降级，同样要使用异步机制探测回复情况

故障降级 —— 如要调用的远程服务挂掉了（网络故障、DNS故障、HTTP服务返回错误的状态码和RPC服务抛出异常），则可以直接降级

限流降级 —— 当触发了限流超额时，可以使用暂时屏蔽的方式来进行短暂的屏蔽
**服务端服务降级**

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>
```

```java
    @Service
    public class PaymentServiceImpl implements PaymentService {
        // 在service层 为 getTimeout 方法提供兜底方法,超过2秒或者运行异常,则使用timeoutFallback响应
        @HystrixCommand(fallbackMethod = "timeoutFallback", commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000")
        })
        @Override
        public RestResponseBo getTimeout() {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return RestResponseBo.ok();
        }

        @Override
        public RestResponseBo timeoutFallback() {
            return RestResponseBo.fail("请求失败,这是服务端的默认响应");
        }
    }
```

```java
	// 主启动类开启断路器
    @EnableCircuitBreaker
    @EnableDiscoveryClient
    @EnableEurekaClient
    @MapperScan(basePackages = "org.lhyf.cloud.payment.mapper")
    @SpringBootApplication
    public class PaymentApplication1 {
        public static void main(String[] args) {
            SpringApplication.run(PaymentApplication1.class);
        }
    }
```

**消费端服务降级**

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>
```

```yaml
feign:
  hystrix:
    enabled: true # 开启全局服务降级
```

```java
@EnableCircuitBreaker // 启用hystrix
@EnableFeignClients
@SpringBootApplication
public class HystrixOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(HystrixOrderApplication.class);
    }
}
```

兜底方法的参数签名和原方法必须一致, **如果服务端与客户端都配置了降级,当服务端发生降级时,客户端也会触发降级(不管客户端设置的超时时间是多少,服务端响应降级,则客户端不会再等到自己配置的超时时间,而是直接响应), 同时响应客户端自己的默认响应**
```java
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderFeignController {
   
    // 为单独的方法配置降级,如果服务端 1.5秒无数据返回,则将返回默认的响应
    @HystrixCommand(fallbackMethod = "timeoutFallback", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1500")
    })
    @GetMapping("/timeout/{num}/{name}")
    public RestResponseBo getTimeOut(@PathVariable("num") Integer num, @PathVariable("name") String name) {
        int i = 10 / num;
        System.out.println(name);
        RestResponseBo response = paymentService.getTimeOut();
        return response;
    }

    public RestResponseBo timeoutFallback(Integer num, String name) {
        return RestResponseBo.fail("请求失败,这是客户端的默认响应,入参: " + num + ":" + name);
    }
}
```

**客户端配置全局的默认响应**

```java
@Slf4j
@RestController
@RequestMapping("/order")
// 指定全局的默认响应
@DefaultProperties(defaultFallback = "globalFallback")
public class OrderFeignController {
    
    // 需要降级的方法,需要使用 @HystrixCommand 修饰
    @HystrixCommand
    @GetMapping("/timeout/{num}/{name}")
    public RestResponseBo getTimeOut(@PathVariable("num") Integer num, @PathVariable("name") String name) {
        int i = 10 / num;
        System.out.println(name);
        RestResponseBo response = paymentService.getTimeOut();
        return response;
    }

    public RestResponseBo globalFallback() {
        return RestResponseBo.fail("请求失败,这是客户端全局的默认响应");
    }
}
```

**为所有的服务方法提供降级** 注意:在为PaymentFeignService提供实现类PaymentFeignServiceFallbackImpl 后不能再使用@RequestMapping

```java
// feign 接口方法
@Component
@FeignClient(value = "CLOUD-PROVIDER-PAYMENT", fallback = PaymentFeignServiceFallbackImpl.class)
//@RequestMapping("/payment")
public interface PaymentFeignService {
    @PostMapping(value = "/payment/add")
    RestResponseBo add(@RequestBody Payment payment);

    @GetMapping("/payment/get")
    RestResponseBo getPaymentById(@RequestParam(value = "id") Long id);

    @GetMapping("/payment/timeout")
    RestResponseBo getTimeOut();
}
```

```java
@Component
public class PaymentFeignServiceFallbackImpl implements PaymentFeignService {
    @Override
    public RestResponseBo add(Payment payment) {
        return RestResponseBo.fail("服务调用失败");
    }

    @Override
    public RestResponseBo getPaymentById(Long id) {
        return RestResponseBo.fail("服务调用失败");
    }

    @Override
    public RestResponseBo getTimeOut() {
        return RestResponseBo.fail("服务调用失败");
    }
}
```

```java
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderFeignController {
    // 当服务端不可用或超时时, PaymentFeignService 里的每个方法都有默认的返回值
    @Autowired
    private PaymentFeignService paymentService;

    @PostMapping("/add")
    public RestResponseBo<Payment> add(@RequestBody Payment payment) {
        RestResponseBo result = paymentService.add(payment);
        return result;
    }

    @GetMapping("/get")
    public RestResponseBo<Payment> getPaymentById(@RequestParam(value = "id") Long id) {
        RestResponseBo response = paymentService.getPaymentById(id);
        return response;
    }


    @GetMapping("/timeout/{num}/{name}")
    public RestResponseBo getTimeOut(@PathVariable("num") Integer num, @PathVariable("name") String name) {
        int i = 10 / num;
        System.out.println(name);
        RestResponseBo response = paymentService.getTimeOut();
        return response;
    }
}
```



### 服务熔断

**服务熔断**则是对于目标服务的请求和调用大量超时或失败，这时应该熔断该服务的所有调用，并且对于后续调用应直接返回，从而快速释放资源，确保在目标服务不可用的这段时间内，所有对它的调用都是立即返回，不会阻塞的。**当检测到该节点微服务调用响应正常后,恢复调用链路.**

Hystrix会监控服务间调用的状况, **当失败的调用到了一定阈值, 缺省是5秒内20次调用失败,就会启动熔断机制**, 熔断机制的注解是@HystrixCommand

```java
// 开启熔断
@EnableCircuitBreaker
@EnableDiscoveryClient
@EnableEurekaClient
@MapperScan(basePackages = "org.lhyf.cloud.payment.mapper")
@SpringBootApplication
public class PaymentApplication1 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication1.class);
    }
}
```

```java
@Slf4j
@RestController
@RequestMapping(value = "/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/get")
    public RestResponseBo getPaymentById(@RequestParam(value = "id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        payment.setSerial(payment.getSerial() + " : " + port);
        if (payment == null) {
            return RestResponseBo.fail("对应记录不存在");
        }
        return RestResponseBo.ok(payment);
    }
    
    @GetMapping("/break/{num}")
    public RestResponseBo paymentCircuitBreaker(@PathVariable("num") int num) {
        return paymentService.paymentCircuitBreaker(num);
    }
}
```

```java
@Service
public class PaymentServiceImpl implements PaymentService {

    @Override
    public Payment getPaymentById(Long id) {
        return paymentMapper.getPaymentById(id);
    }

    
    /**********服务熔断测试**********/
    /**
     * Hystrix在运行过程中会向每个commandKey对应的熔断器报告 成功、失败、超时和拒绝的状态，熔断器维护计算统计的数据，
     * 根据这些统计的信息来确定熔断器是否打开。如果打开，后续的请求都会被截断。
     * 然后会隔一段时间默认是5s，尝试半开，放入一部分流量请求进来，相当于对依赖服务进行一次健康检查，如果恢复，熔断器关闭，随后完全恢复调用。
     * @return
     */
    @HystrixCommand(fallbackMethod = "fallbackCircuitBreaker",commandProperties = {
            // 此属性确定断路器是否用于跟踪健康状况，以及在断路器跳闸时是否用于短路请求。
            @HystrixProperty(name = "circuitBreaker.enabled",value = "true"),
            // 熔断器强制打开，始终保持打开状态。默认值FLASE。
            @HystrixProperty(name = "circuitBreaker.forceOpen",value = "false"),
            // 熔断器强制关闭，始终保持关闭状态。默认值FLASE。
            @HystrixProperty(name = "circuitBreaker.forceClosed",value = "false"),
            // 此属性设置将跳闸电路的滚动窗口中的最小请求数。
            // 例如，如果这个值是20，那么在滚动窗口(比如一个10秒的窗口)中只接收到19个请求，即使所有19个请求都失败了，电路也不会跳闸打开。
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold",value = "10"),
            // 半开试探休眠时间，默认值5000ms。当熔断器开启一段时间之后比如5000ms，
            // 会尝试放过去一部分流量进行试探，确定依赖服务是否恢复。
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds",value = "10000"),
            // 设定错误百分比，默认值50%，例如一段时间（10s）内有100个请求，其中有55个超时或者异常返回了，
            // 那么这段时间内的错误百分比是55%，大于了默认值50%，这种情况下触发熔断器-打开。
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage",value = "60")
    })
    @Override
    public RestResponseBo paymentCircuitBreaker(int num) {
        if(num < 0){
            throw new IllegalArgumentException("参数不能小于0");
        }
        return RestResponseBo.ok("服务端的正常返回");
    }

    @Override
    public RestResponseBo fallbackCircuitBreaker(int num) {
        return RestResponseBo.fail("服务端熔断的默认返回");
    }
}

```

现象: 多次访问 http://127.0.0.1:8001/payment/break/-1 造成服务熔断后,再次访问  http://127.0.0.1:8001/payment/break/0,依然提示熔断,此时访问 getPaymentById()方法,正常返回. 等待10秒(circuitBreaker.sleepWindowInMilliseconds) 后会尝试放行访问,如果访问成功,则取消熔断,若访问依然失败,则继续熔断

- 熔断打开: 请求不在进行调用当前服务, 内部设置时钟一般为MTTR(平均故障处理时间), 当打开时长达到设置时钟则进入半熔断状态

- 熔断关闭: 熔断关闭不会对服务进行熔断
- 熔断半开: 部分请求根据规则调用当前服务, 如果请求成功且符合规则则认为当前服务恢复正常, 关闭熔断

![Hystrix 工作流程图](https://raw.githubusercontent.com/wiki/Netflix/Hystrix/images/hystrix-command-flow-chart.png)



### 服务限流

只允许系统能够承受的访问量进来，超出的会被丢弃。

### 实时监控





# Gateway

**能干嘛**: 反向代理, 鉴权, 流量控制, 熔断, 日志监控...

## 三大核心概念

### Route(路由)

这是网关的基本构建块。它由一个 ID，一个目标 URI，一组断言和一组过滤器定义。如果断言为真，则路由匹配

### Predicate(断言)

这是一个 Java 8 的 Predicate。输入类型是一个 ServerWebExchange。我们可以使用它来匹配来自 HTTP 请求的任何内容，例如 headers 或参数。如果请求与断言相匹配则进行路由

### Filter(过滤)

这是org.springframework.cloud.gateway.filter.GatewayFilter的实例，我们可以使用它修改请求和响应。

## 工作流程
1. 客户端向 Spring Cloud Gateway 发出请求, 然后在Gateway Handler Mapping 中找到与请求相匹配的路由,将其发送到Gateway Web Handler.
2. Handler 再通过指定的过滤器来将请求发送到我们实际的服务执行业务逻辑, 然后返回. 过滤器之间用虚线分开是因为过滤器可能会在发送代理请求之前(pre)或之后(post)执行业务逻辑.
3. Filter 在pre 类型的过滤器可以做参数校验,权限校验,流量监控,日志输出,协议转换等,在 post 类型的过滤器中可以做响应内容, 响应头的修改,日志输出, 流量监控等重要的作用.

![Gateway 工作流程图](https://cloud.spring.io/spring-cloud-static/spring-cloud-gateway/2.2.2.RELEASE/reference/html/images/spring_cloud_gateway_diagram.png)

## 路由配置

使用yml配置路由

```yaml
spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      routes:
        - id: payment #路由id, 没有固定规则但要求唯一, 建议配服务名
          uri: http://127.0.0.1:8001 #匹配后提供服务的路由地址
          predicates:
            - Path=/payment/get/** #断言,路径匹配的进行路由

        - id: payment #路由id, 没有固定规则但要求唯一, 建议配服务名
          uri: http://127.0.0.1:8001 #匹配后提供服务的路由地址
          predicates:
          - Path=/payment/break/** #断言,路径匹配的进行路由

```

使用配置类配置

```yaml
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        RouteLocator locator = builder.routes()
                .route("route1", r -> r.path("/lhyf")
                        .uri("http://www.lhyf.org")
        ).build();

        return locator;
    }
```

使用注册中心实现动态路由

```yaml
spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true #开启从注册中心动态创建路由的功能,利用微服务名进行路由
      routes:
        - id: payment #路由id, 没有固定规则但要求唯一, 建议配服务名
          uri: lb://cloud-provider-payment #匹配后提供服务的路由地址
          predicates:
            - Path=/payment/get/** #断言,路径匹配的进行路由

        - id: payment #路由id, 没有固定规则但要求唯一, 建议配服务名
          uri: lb://cloud-provider-payment #匹配后提供服务的路由地址
          predicates:
          - Path=/payment/break/** #断言,路径匹配的进行路由

```

## Predicate(断言)

**After Route Predicate Factory**
**The Before Route Predicate Factory**

带有一个参数,一个 ZonedDateTime 类型 `datetime` , 这个断言匹配 发生在指定的时间之后(前)的请求.

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: after_route
        uri: https://example.org
        predicates:
        - After=2017-01-20T17:42:47.789-07:00[America/Denver]
```

**The Between Route Predicate Factory**

接收两个日期参数，判断请求日期是否在指定时间段内

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: between_route
        uri: https://example.org
        predicates:
        - Between=2017-01-20T17:42:47.789-07:00[America/Denver], 2017-01-21T17:42:47.789-07:00[America/Denver]
```

**The Cookie Route Predicate Factory**
cookie断言带有两个参数, `cookie name` 和 `regexp`(Java 的正则表达式), 此断言匹配含有指定cookie名,且cookie值匹配正则表达式的请求

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: cookie_route
        uri: https://example.org
        predicates:
        - Cookie=chocolate, ch.p # 这个路由匹配请求含有 cookie名为 chocolate ,它的值匹配 ch.p 正则表达式的请求
```

**The Header Route Predicate Factory**

含有两个参数,  header `name` 和 `regexp`

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: header_route
        uri: https://example.org
        predicates:
        - Header=X-Request-Id, \d+ # 匹配 header named 是 X-Request-Id ,它的值匹配 \d+ 的请求
```

**The Host Route Predicate Factory**

含有一个参数: 主机名的列表`patterns`。该模式是带有`.`分隔符的Ant样式的模式

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: host_route
        uri: https://example.org
        predicates:
        - Host=**.somehost.org,**.anotherhost.org
```

**The Method Route Predicate Factory**

需要`methods`的参数，它是一个或多个参数：HTTP请求方式来匹配

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: method_route
        uri: https://example.org
        predicates:
        - Method=GET,POST
```

**The Path Route Predicate Factory**
拥有两个参数, Path Route Predicate Factory使用的是path列表作为参数，使用Spring的`PathMatcher`匹配path，可以设置可选变量。

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: path_route
        uri: https://example.org
        predicates:
        - Path=/red/{segment},/blue/{segment}
```

**The Query Route Predicate Factory**
两个参数, 一个要求的 'param' 和一个可选的正则表达式
```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: query_route
        uri: https://example.org
        predicates:
        - Query=green  # 如果请求中包含有 gree的请求参数,则将被匹配
        
        - Query=red, gree. # 如果请求中包含有red 的请求参数,它的值匹配 gree. 的正则表达式,则将被匹配
```

**The RemoteAddr Route Predicate Factory**

需要一个是 CIDR表示法(IPv4 or IPv6)的字符串list(最小的size=1) 的source, 例如 192.168.0.1/16

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: remoteaddr_route
        uri: https://example.org
        predicates:
        - RemoteAddr=192.168.1.1/24 # 如果请求的远程地址为，则此路由匹配192.168.1.10
```

**The Weight Route Predicate Factory**
含有两个参数 `group` 和 `weight`(一个 int ) , 权重是按组计算的
```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: weight_high
        uri: https://weighthigh.org
        predicates:
        - Weight=group1, 8
      - id: weight_low
        uri: https://weightlow.org
        predicates:
        - Weight=group1, 2 # 这条路由会将大约80％的流量转发到weighthigh.org，将大约20％的流量转发到weightlow.org。
```

## Filter(过滤器)

指的是Spring框架中GatewayFilter的实例, 使用过滤器, 可以在请求路由前后对请求进行修改

**GatewayFilter Factories (过滤器工厂)** (30多种)

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: add_request_header_route
        uri: https://example.org
        predicates:
        - Path=/red/{segment}
        filters:
        - AddRequestHeader=X-Request-Red, Blue-{segment} #此清单将X-Request-Red, Blue-{segment} 请求头添加到所有匹配请求的下游请求头中。
```

**Global Filters (全局过滤器)**





**自定义全局过滤器**

```java
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String name = request.getQueryParams().getFirst("name");
        if (StringUtils.isEmpty(name)) {
            log.error("用户名不能为空");
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }
        return chain.filter(exchange);
    }


    @Override
    public int getOrder() {
        return 0;
    }
}

```



# 配置中心

## Spring Cloud Config 

为微服务架构中的微服务提供了集中化的外部配置支持, 配置服务器为各个不同微服务应用的所有环境提供了一个中心化的外部配置



# 消息总线

## Spring Cloud Bus 

Spring cloud bus 是用来将分布式系统的节点与轻量级消息系统连接起来的框架, 它整合了Java的时间处理机制和消息中间件的功能, Bus 支持两种消息代理: RabbitMQ 和 Kafka, 能管理和传播分布式系统间的消息, 就像一个分布式执行器, 可用于广播状态更改, 事件推送等,也可以当做微服务间的通信通道.



**什么是总线**

在微服务架构的系统中, 通常会使用**轻量级的消息代理**来构建一个公用的消息主题,并放系统中所有微服务实例都连接上来. 由于**该主题中产生的消息会被所有实例监听和消费, 所以称他为消息总线**, 在总线上的各个实例,都可以方便地广播一些需要其他连在该主题上的实例都知道的消息.

**基本原理**

ConfigClient 实例都监听MQ中同一个topic(默认是SpringCloudBus), 当一个服务刷新数据的时候, 它会把这个消息放入Topic中, 这样其它监听同一Topic的服务就能得到通知, 然后去更新自身的配置.

## 使用

```xml
  <!--添加消息总线RabbitMQ 支持-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-bus-amqp</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
```

**服务端**

```yaml
server:
  port: 3344
spring:
  application:
    name: cloud-config-center #注册进Eureka的服务名
  #RabbitMQ相关配置
  rabbitmq:
    host: 192.168.31.201
    port: 5672
    username: guest
    password: guest

eureka:
  client:
    # 将自己注册进Eureka, 默认是true
    register-with-eureka: true
    # 是否从Eureka抓取已有的注册信息,默认为true,单节点无所谓,集群必须设置为true才能配合ribbon使用负载均衡
    fetch-registry: false
    service-url:
      # 单机注册
      #      defaultZone: http://localhost:7001/eureka
      # 集群注册
      defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
  instance:
    instance-id: config-center3344 # 修改服务名称
    prefer-ip-address: true # 访问路径可以显示IP地址



#RabbitMQ 相关配置, 暴露bus刷新配置的端点
management:
  endpoints: #暴露bus 刷新配置的端点
    web:
      exposure:
        include: 'bus-refresh' #用于刷新配置的链接

```
**客户端**

```yaml
server:
  port: 3355
spring:
  application:
    name: cloud-config-client #注册进Eureka的服务名
  #RabbitMQ相关配置
  rabbitmq:
    host: 192.168.31.201
    port: 5672
    username: guest
    password: guest

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
    instance-id: config-client3355 # 修改服务名称
    prefer-ip-address: true # 访问路径可以显示IP地址


#RabbitMQ 相关配置, 暴露bus刷新配置的端点
management:
  endpoints: #暴露bus 刷新配置的端点
    web:
      exposure:
        include: '*'

```
















