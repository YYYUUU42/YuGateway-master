# 1. 什么是网关，为什么需要自研网关

### 为什么会出现 Yu-Gateway 这个项目

相信大家基本上做过微服务项目的都会使用 SpringCloud GateWay 作为网关，尽管 Spring Cloud Gateway 作为 API 网关为我们带来了诸多便利，如快速开发、稳定运行以及丰富的功能集，其透明化的封装特性也带来了一些挑战。在遇到问题时，开发者往往需要深入框架内部，理解其工作原理与细节，以定位和解决问题，这无疑增加了项目的维护成本。此外，成熟框架为了适应广泛场景，可能包含了部分当前项目并不需要的功能，导致系统在某种程度上显得功能冗余。

面对上述情况，我们意识到针对特定项目需求进行个性化定制的重要性。出于学习与实践的目的，我们着手创建了 Yu-Gateway 项目。该项目旨在构建一个贴合项目实际需求、精简高效的API网关，不仅能够剔除不必要的功能以保持系统轻量化，还能提供深入理解与掌控网关运作机制的机会，从而提升技术实力与应对未来挑战的能力。



### 网关是什么

网关作为现代分布式架构中的关键组件，其作用远超乎想象的丰富多样，它犹如一座桥梁，联结客户端与后端服务，承担着至关重要的角色。对网关的核心职能进行归纳总结，以便更深入地理解其在整个系统中的价值。实际上，一个典型的客户端请求在经过网关时，会经历如下细致入微的处理步骤：

1. **请求识别**精准解析请求的元信息，包括头部、HTTP方法与路径，以此准确判断请求的服务目标与具体操作意图。
2. **安全验证**：在安全层面执行一系列严格检查，如解密加密传输的请求、核实API密钥的有效性、执行OAuth等标准认证流程，确保请求来源的合法性与权限合规。
3. **路由决策**：遵循预定义的路由规则或利用服务发现机制动态定位目标服务实例，确保请求被精确导向至正确的处理单元。
4. **请求调整**：根据业务需求或系统配置，网关可能对请求的头部、查询参数乃至请求体进行必要修改，以适应后端服务接口或优化通信效率。
5. **负载均衡调度**：运用各种负载均衡算法，在多个服务实例间智慧分配请求，确保系统整体负载均衡，避免单点过载与资源浪费。
6. **请求转发执行**：将经过处理的请求透明、高效地转发至选定的后端服务，启动实际业务逻辑的执行。
7. **响应处理与反馈**：接收后端服务返回的响应数据，可能对其进行格式转换、内容重组等操作，确保最终呈现给客户端的响应符合约定规范。
8. **日志记录留存**：详尽记录整个请求生命周期的详细信息，包括请求与响应的具体内容、处理时间、状态码等，为后续的运维审计、性能调优与故障排查提供宝贵数据支撑。

综上所述，网关在系统架构中的职责繁多且关键，它不仅是保障系统安全、稳定、高效运行的重要屏障，也是实现服务治理、监控分析与灵活扩展的核心枢纽。因此，在设计网关时，充分考虑上述各功能点及其相互影响至关重要，它们直接决定了网关的功能完备性与性能表现，成为衡量网关设计成败的关键指标。



### 为什么需要自研网关

在评估技术栈选择时，一种有效的策略是从正反两面审视成熟框架与自研解决方案的优势与局限。通过对成熟开源网关框架的优势进行剖析，我们可以揭示其潜在的不足，进而明确自研网关的必要性。以下是对成熟框架与自研网关各自特点的深度解读：

**成熟开源网关框架的优势与潜在局限**

1. **稳定性**

- **优势**：提供高度可靠的行为和预期的输出，降低系统故障风险。
- **潜在局限**：过度追求稳定性可能导致在特定场景下缺乏必要的灵活性，使得实现特定功能变得复杂或效率降低，无法满足快速变化的业务需求或应对新兴技术挑战。

2. **社区支持**

- **优势**：丰富的社区资源、文档和实践经验有助于快速解决问题，降低学习曲线。
- **潜在局限**：过度依赖社区支持可能在需要快速响应或定制化解决方案时受制于社区响应速度和专业知识的可获取性，特别是在处理独特业务场景或应对紧急问题时。

3. **快速开发**

- **优势**：现成的功能模块与简化配置有助于缩短项目周期，提高开发效率。
- **潜在局限**：通用化设计可能无法完美契合所有应用场景，可能导致性能非最优，或需额外工作进行定制化适配，增加隐形成本。

4. **集成能力**

- **优势**：与多种服务和组件具有良好兼容性，易于构建生态体系。
- **潜在局限**：过度依赖现有集成可能阻碍新技术的采纳，尤其是当新技术与现有网关架构不兼容或未得到充分优化时，可能限制系统的创新与发展。

5. **成本效益**

- **优势**：初期投入较少，可利用开源资源快速构建基础架构。
- **潜在局限**：随着时间推移，为适应特定需求进行学习、调整和扩展开源网关可能产生额外成本，甚至可能超过初期节省，尤其是在面临复杂定制、性能瓶颈或技术演进需求时。



从简单且更加片面的角度去看，自研网关的好处有如下几点：

1. **完全控制**: 自研网关意味着拥有对网关功能的全面控制，团队可以根据具体业务需求定制网关行为，不受外部项目的限制。这包括路由决策、安全性检查、流量控制和日志记录等。
2. **定制的性能优化**: 可以针对特定的用例和环境进行性能调优。例如，如果某个服务需要非常高的吞吐量或极低的延迟，团队可以优化代码和配置来满足这些特定的性能要求。
3. **特色功能**: 在内部网关中实现独特的业务逻辑和集成，例如，自定义认证/授权机制，特殊的转换逻辑，或者与内部系统的特殊集成。
4. **减少依赖**: 自研网关减少了对外部开源项目维护速度和方向的依赖，这在某些情况下可能避免了被迫跟随外部变化带来的风险。



更加高维的视角去看，为什么需要一个自研网关？

1. **成本考虑**: 成熟的网关产品在未来可能会引入不兼容的更改或弃用特定特性，导致企业不得不进行昂贵的迁移或重构。自研网关则完全控制版本和功能迭代，避免了这种风险。
2. **技术因素**: 对于使用非HTTP协议（如TCP、UDP直连或专有协议等）的老旧系统，市面上的网关产品往往只提供有限或无支持。自研网关可以确保与这些系统的兼容性，证据在于许多遗留系统的成功集成案例，例如金融行业中特有的交易协议。
3. **网络和安全性能**: 在对网络延迟极为敏感的交易系统中，即使是毫秒级的改进也具有重大意义。自研网关可以针对特定的网络环境和数据流模式进行优化，有的金融机构通过内部开发的网关减少了30%的网络延迟。
4. **组织和运营**: 自研网关的更新和迭代可以与企业的CI/CD流程紧密集成，这为企业带来了更高的敏捷性和响应市场变化的能力。在许多成功案例中，企业能够在几小时内部署网关更新以支持新的市场需求或应对安全威胁，而不是等待第三方发布更新。
5. **市场和竞争优势**: 通过自研网关提供的独特服务或功能，企业可以在市场上建立独特的卖点。例如，Amazon的API Gateway提供了与AWS服务的深度集成，这是它们的一个关键竞争优势。
6. **创新驱动**: 自研网关允许企业实现最新的技术研究成果，而无需等待这些功能在开源项目中可用。例如，谷歌的ESPv2 API网关就是在开源的Envoy基础上，集成了谷歌特有的安全、监控和控制策略。

其实，上面说的都太高大上了，从个人角度的层面来说的话，一个自研网关的作用难道不是，在自研项目的过程中，增加你对架构设计，语言特性以及你的知识面嘛。






# 2. 技术选型以及架构设计

### 市场调研

在设计一个项目之前，进行全面的市场调研与竞品分析是不可或缺的环节。这一步骤旨在深入了解当前市场上已有的同类产品，明确它们的优势与不足，然后提炼出设计这个项目的一个设计方向。

所以调查了市面上已有的且比较知名的网关项目，列举出了如下这张优缺点以及侧重点的表格。

| Gateway名称              | 优点                                                         | 缺点                                               | 设计侧重点                  |
| ------------------------ | ------------------------------------------------------------ | -------------------------------------------------- | --------------------------- |
| **Spring Cloud Gateway** | 基于 Spring 框架，拥有庞大的 Spring 生态系统，可以轻松集成其他组件 | 相对于其他选择，性能较慢。其生态和社区支持较弱     | 集成多种协议和插件，扩展性  |
| **Nginx**                | 高性能，配置灵活，轻量级，高稳定性                           | 模块化程度低，扩展性差，异步处理能力受限           | 高性能HTTP服务器和反向代理  |
| **Apache HTTP Server**   | 模块丰富，社区活跃，跨平台，文档齐全                         | 性能较差，配置复杂，更重量级                       | 多功能Web服务器，重视模块化 |
| **HAProxy**              | 高性能，支持TCP和HTTP代理，稳定且成熟                        | 配置不如Nginx直观，缺乏现代Web界面                 | 专注于高并发连接的负载均衡  |
| **Traefik**              | 自动化服务发现和配置，容器和微服务友好，易于部署             | 社区较新，历史较短                                 | 云原生环境中的动态配置      |
| **Kong**                 | 基于Nginx和OpenResty，提供丰富的插件，管理界面友好           | 高性能场景可能需优化配置，插件生态不如Apache/Nginx | 扩展性和插件生态系统        |



基于上面这些比较成熟且知名度较高的网关，提炼出了设计一个网关的侧重点：

1. **性能与可伸缩性**:

- - 关注高吞吐量和低延迟处理，以便能够处理大量并发连接和数据流。
  - 设计可在多个服务器、数据中心或地理区域之间伸缩的解决方案。

2. **安全性**:

- - 实现高级安全特性，如SSL/TLS终止、OAuth、JWT、API密钥验证和防止DDoS攻击等。
  - 确保所有通过网关的流量都符合最新的安全标准和法规要求。

3. **可观测性**:

- - 提供详细的监控和日志记录功能，使运维团队能够观测和诊断问题。
  - 集成与现有监控工具和警报系统的能力。

4. **路由能力**:

- - 开发动态路由和负载均衡策略，以支持微服务架构中服务发现的需求。
  - 支持基于URL、路径或头部的路由决策。

5. **扩展性**:

- - 构建插件架构，使新功能能够以模块化的方式添加。
  - 保持核心轻量级，允许通过插件或服务集成额外功能。

6. **多协议支持**:

- - 考虑支持多种网络协议，不仅限于HTTP/HTTPS，也包括WebSocket、RPC等。

7. **高可用性**:

- - 确保网关设计能够容忍单点故障和网络分区，提供故障转移和灾难恢复机制。





基于上面列举出来的这些点之后，就可以开始分析，应该如何从这些点入手将一个具体的问题拆解为几个细粒度的解决方法。

接下来一点一点的对上面的七个点进行分析，分析其具体的解决方法和思路。

于是就有了如下的答案：

1. **性能与可伸缩性**:

- - 使用 **Netty** 进行异步网络编程，Netty 是一个高性能的网络应用程序框架，可以处理大量的并发连接。
  - **缓存** 如 Caffeine 或 Redis 来减少数据库访问频率，提升性能。

2. **安全性**:

- - 集成 **JWT** 用于安全的API访问。
  - 利用 **TLS/SSL** 加密传输数据。

3. **可观测性**:

- - 集成 **Micrometer** 或 **Dropwizard Metrics** 来收集和导出性能指标。
  - 使用 **ELK Stack**（Elasticsearch, Logstash, Kibana）来收集和分析日志数据。
  - 利用 **Prometheus** 和 **Grafana** 进行监控和警报。

4. **路由能力**:

- - 利用 **Zuul** 或自定义的 **Servlet Filters** 进行动态路由。
  - 结合 **Consul** 或 **Eureka** 或 **Nacos** 进行服务发现和注册。

5. **扩展性**:

- - 设计插件架构，使得可以通过 **Java SPI (Service Provider Interface)** 加载新模块。

6. **多协议支持**:

- - **使用 gRPC/Dubbo 来支持RPC调用。**
  - 支持 **WebSocket** 用于双向通信，使用Java的 **JSR 356** 或 **Spring Framework** 的WebSocket API。

7. **高可用性:**

- - **使用 Nacos /  ZooKeeper / etcd 来管理网关的配置信息和服务元数据，以支持高可用部署。**



好的，那么其实基于上面的分析，就已经可以大致的得到设计一个网关所需要的一些技术上的方向了，接下来的就是确定这些技术，并且确定自己设计该网关时的一个架构图了。



### 技术选型

###### 性能与可伸缩性：

参考目前主流的网关的设计，有 SpringCloud Gateway 以及 Zuul，他们的底层都大量使用了异步编程的思想，并且也都有非常重要的网络通信上的设计。

由于我们的网关是自研的，也就是他自己本身就是一个单独的服务，因此我们并不需要使用到SpringBoot这种框架，我们可以直接使用原生Java框架来编写各种重要代码。且 Spring Cloud Gateway 底层也是大量使用到 Netty，所以网络通信上也还是使用 Netty 即可。

缓存以及高性能这方面，分布式缓存我们使用 Redis，本地缓存选择 Caffeine，因为 Redis 是市面上使用最广泛的缓存中间件，而 Caffeine 更是有着本地缓存之王的称号，所以选择这两个主要是看中了其成熟的特点。

然后为了提高其缓存区的性能，这里考虑使用 Disruptor 这个无锁队列，因为其无界队列的特性，可以将其作为缓存区队列提高性能。



###### 安全性上：

使用JWT，其优点在于简单的Token格式，便于跨语言和服务传递，适合于微服务和分布式系统的安全性设计。

当然缺点也在于我们需要精细的管理和保护我们的密钥。

这里我并不打算支持TLS/SSL，首先是我作为个人开发者，想要去支持TLS/SSL是比较复杂的，并且我还需要管理证书的生命周期，会影响项目开发的进度，因此我并不打算在我的网关中支持TLS/SSL。



###### 可观测性：

- **Micrometer** 和 **Dropwizard Metrics**:

- - 优点: 两者都是成熟的度量收集框架，提供了丰富的度量集合和报告功能。
  - 缺点: 可能需要适配特定的监控系统或标准。

- **ELK Stack**:

- - 优点: 提供了一个完整的日志分析解决方案，适用于大规模日志数据的收集、搜索和可视化。
  - 缺点: 组件较多，搭建和维护相对复杂。

- **Prometheus** 和 **Grafana**:

- - 优点: 高度适合于时序数据监控，Grafana 提供强大的数据可视化。
  - 缺点: 需要配置和维护 Prometheus 数据抓取和存储。

这里我选择使用最后一种，因为目前市面上这种用的比较多，并且Prometheus相对于其他的更加简单易用。



###### 路由能力/高可用：

同时，在上文也提到了，网关是需要用到注册中心的，因为我们的请求具体最后要转发到那个路由，是需要从注册中心中拉取服务信息的，目前注册中心有：**Zookeeper，Eureka，Nacos，Apollo，etcd，Consul**

他们各有优劣势，比如Zk保证的是CP而不是AP，我们知道，网关是应用的第一道门户，我们使用 Dubbo 的时候会使用 Zk ，但是对于网关，可用性大于一致性，因此 Zk 我们不选。

而 Eureka 都和 Spring Cloud 生态有比较紧密的联系，因此如果我们使用它，就会增加我们的网关和 Spring Cloud 的耦合，不太符合我们自研的初衷，所以也不选。

Etcd 虽然是通用的键值对分布式存储系统，可以很好的应用于分布式系统，但是依旧没有很好的优势，当然，他很轻量级。所以这里暂不考虑。

Consul 和 Etcd 差不多，所以这里也不考虑 Consul。

这里我选用 Nacos 作为注册中心，Nacos 首先支持 CP 和 AP 协议，并且提供了很好的控制台方便我对服务进行管理。同时，Nacos 的社区相对来说非常活跃，网络上的资料也更加的多，同时，我也看过 Nacos 的源码，编写优雅且相对易懂。同时我相信会使用 Nacos 的人肯定更多，因此在这里选择 Nacos 作为注册中心。

当然，上面的几种注册中心都可以使用，没有特别明显的优劣势，他们也都有各自合适的场合，具体场合具体分析，主要是要分析自己的团队更加了解或者适合哪一种注册中心。

而配置中心方面，有SpringCloud Config，Apollo，Nacos。

这里很明显，依旧选择Nacos，因为Nacos不仅仅是注册中心也是配置中心。因此选用Nacos我们可以减少引入不必要的第三方组件。



###### 多协议支持：

可以考虑的有 gRPC 和 Dubbo ，gRPC 支持多种语言，并且基于HTTP/2.0，Dubbo在 Alibaba 使用的比较多，并且比较适合 Java 的生态。同时 gRPC 的使用要求熟悉Protobuf，所以这里为了减少成本，考虑使用Dubbo。



所以，经过一套分析，我们就可以得出如下的主要技术栈：

###### 总结：

1. **开发语言：Java 19**
2. **网络通信框架：Netty 4.1.51**
3. **缓存：Redis、Caffeine 版本不限**
4. **注册中心与配置中心：Naccos 2.0.4**
5. **RPC协议：Dubbo 2.7.x**
6. **日志监控：Prometheus、Grafana 版本不限**
7. **安全鉴权：JWT 版本不限**



### 技术架构

![img](https://cdn.nlark.com/yuque/0/2024/png/27676268/1712322010176-a583157e-ec90-402f-b5fc-6f403206089a.png)

Common：维护公共代码，比如枚举

Client：客户端模块，方便我们其他模块接入网关

Register Center：注册中心模块

Config Center：配置中心模块

Container：包含核心功能

Context：请求上下文，规则

FilterChain：通过责任链模式，链式执行过滤器

FlowFilter：流控过滤器

LoadBalanceFilter：负载均衡过滤器

RouterFilter：路由过滤器

TimeoutFilter：超时过滤器

OtherFilter：其他过滤器

NettyHttpServer：接收外部请求并在内部进行流转

Processor：后台请求处理

Flusher：性能优化

MPMC：性能优化

SPI Loader：扩展加载器

Plugin Loader：插件加载器

Dynamic Loader：动态配置加载器

Config Loader：静态配置加载器



### 网关处理流程

![img](https://cdn.nlark.com/yuque/0/2024/png/27676268/1712322433460-72b386ed-4276-4b52-9996-bf8222ee4b74.png)


# 更多详细内容可以参看博客专栏
https://blog.csdn.net/m0_63208096/category_12631543.html?spm=1001.2014.3001.5482
