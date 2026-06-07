package com.hmall.gateway.routers;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter routeDefinitionWriter;

    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";

    private final Set<String> routeIds = new HashSet<>();

    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        log.info("初始化动态路由监听");

        // 1. 项目启动先拉去配置,在添加配置监听器
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        // 2. 监听到配置变更, 更新路由
                        updateRouteConfig(configInfo);
                    }
                });
        // 3. 第一次读取到配置, 更新路由
        updateRouteConfig(configInfo);
    }

    public void updateRouteConfig(String configInfo) {
        log.info("更新动态路由: {}", configInfo);
        // 1. 解析配置,转为routeDefinition
        List<RouteDefinition> routeDefinition = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 2. 先删除旧的路由
        routeIds.forEach(routeId -> {
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        });
        routeIds.clear();

        // 3. 更新路由
        routeDefinition.forEach(route -> {
            // 3.1 更新路由表
            routeDefinitionWriter.save(Mono.just(route)).subscribe();
            // 3.2 添加路由ID,方便下一次删除
            routeIds.add(route.getId());
        });
    }

}
