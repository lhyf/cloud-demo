package org.lhyf.cloud.order.rule;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import java.util.List;
import java.util.Random;

/****
 * @author YF
 * @date 2020-04-04 19:00
 * @desc MyLoadBalanceRule
 *
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
