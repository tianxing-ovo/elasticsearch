# Elasticsearch 酒店搜索系统

一个基于 Spring Boot + Elasticsearch + RabbitMQ 的分布式酒店搜索与实时数据同步系统

## 技术栈

|           技术            |  版本   |         说明         |
|:-------------------------:|:-------:|:--------------------:|
|        Spring Boot        |  3.4.1  |       基础框架       |
| Spring Data Elasticsearch |  5.4.1  |  Elasticsearch 集成  |
|       Elasticsearch       |   8.x   |       搜索引擎       |
|         RabbitMQ          |   3.x   | 消息队列（数据同步） |
|       MyBatis-Plus        |  3.5.9  |       ORM 框架       |
|           MySQL           |   8.x   |     关系型数据库     |
|           Java            |   17+   |       JDK 版本       |
|          Lombok           | 1.18.36 |       简化代码       |

## 项目结构

```
src/
├── main/
│   ├── java/com/ltx/
│   │   ├── Application.java                 # 启动类
│   │   ├── config/
│   │   │   ├── MqConfig.java                # RabbitMQ 配置（交换机/队列绑定）
│   │   │   ├── MybatisPlusConfig.java       # MyBatis Plus 分页插件配置
│   │   │   └── WebMvcConfig.java            # Web MVC 配置（静态资源映射）
│   │   ├── constant/
│   │   │   ├── Constant.java                # 通用常量（ES 索引名等）
│   │   │   └── MqConstant.java              # MQ 相关常量（交换机/队列/RoutingKey）
│   │   ├── controller/
│   │   │   ├── AdminHotelController.java    # 后台管理控制器（增删改查）
│   │   │   └── HotelController.java         # 前台搜索控制器
│   │   ├── entity/
│   │   │   ├── Hotel.java                   # 酒店实体（MySQL 表映射）
│   │   │   ├── HotelDoc.java                # 酒店文档（Elasticsearch 索引映射）
│   │   │   ├── PageResult.java              # 分页结果封装
│   │   │   ├── Result.java                  # 统一响应结果
│   │   │   └── SearchRequestBody.java       # 搜索请求体
│   │   ├── listener/
│   │   │   └── HotelListener.java           # RabbitMQ 监听器（同步 ES 数据）
│   │   ├── mapper/
│   │   │   └── HotelMapper.java             # MyBatis Mapper 接口
│   │   ├── repository/
│   │   │   └── HotelDocRepository.java      # Elasticsearch Repository
│   │   └── service/
│   │       ├── AdminHotelService.java       # 后台管理服务接口
│   │       ├── HotelService.java            # 搜索服务接口
│   │       └── impl/
│   │           ├── AdminHotelServiceImpl.java  # 后台管理服务实现
│   │           └── HotelServiceImpl.java       # 搜索服务实现
│   └── resources/
│       ├── application.properties           # 应用配置文件
│       ├── logback.xml                      # 日志配置
│       ├── json/
│       │   └── settings.json                # ES 索引 Settings（分词器配置）
│       ├── sql/
│       │   └── tb_hotel.sql                 # 数据库初始化脚本
│       └── static/                          # 静态资源
│           ├── css/                         # 样式文件
│           ├── html/
│           │   ├── index.html               # 用户端搜索页面
│           │   └── admin.html               # 管理端后台页面
│           ├── img/                         # 图片资源
│           └── js/                          # JavaScript 文件
└── test/java/com/ltx/
    └── HotelTest.java                       # 单元测试类
```

## 核心功能

### 1. 搜索引擎 (用户端)

- **全文检索**：基于 IK 分词器，支持对酒店名称、商圈、品牌等多字段进行混合搜索
- **条件筛选**：支持按城市、星级、价格范围、品牌精确筛选
- **地理位置**：
    - **附近酒店**：根据用户经纬度计算距离并排序（使用 `geo_distance`）
    - **距离显示**：实时计算酒店与用户的直线距离
- **智能排序**：支持综合排序（含广告加权）、价格排序、距离排序
- **自动补全**：输入关键词时提供拼音和汉字的联想建议（使用 `completion` suggester）
- **广告置顶**：通过 `function_score` 对标记为广告的酒店进行加权算分，使其排名靠前

### 2. 后台管理 (管理端)

- **酒店 CRUD**：提供完整的增删改查接口
- **广告管理**：可设置酒店是否为广告推广位
- **分页查询**：支持后台分页浏览所有酒店

### 3. 数据同步 (核心架构)

采用 **RabbitMQ 异步解耦** 实现 MySQL 与 Elasticsearch 之间的数据同步：

```
                                   ┌─────────────┐
                           ┌──────▶│    MySQL    │◀─────────┐
                           │       └─────────────┘          │
                           │ 1.写入                     4.查询最新数据
                           │                                │
┌─────────────┐    ┌───────┴─────┐                   ┌──────┴──────┐       ┌─────────────┐
│  前端/API   │───▶│   Service   │                   │  Listener   │──────▶│Elasticsearch│
└─────────────┘    └───────┬─────┘                   └──────▲──────┘ 5.写入 └─────────────┘
                           │                                │
                           │ 2.发送消息(Hotel ID)           │ 3.消费消息
                           ▼                                │
                   ┌─────────────┐                          │
                   │  RabbitMQ   │──────────────────────────┘
                   │  Exchange   │
                   │     ↓       │
                   │   Queue     │
                   └─────────────┘
```

**同步流程**：

1. 前端调用 API → Service 将数据 **写入 MySQL**
2. 写入成功后，Service **发送 MQ 消息**（仅包含 Hotel ID）
3. RabbitMQ 将消息路由到对应队列， **Listener 消费消息**
4. Listener 根据 ID **查询 MySQL 获取最新数据**
5. Listener 将数据转换为 HotelDoc 并 **写入 Elasticsearch**

**RabbitMQ 交换机与队列配置**：

```
                              ┌────────────────────────┐
                              │    hotel.topic         │
                              │   (Topic Exchange)     │
                              └───────────┬────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
                    │RoutingKey:          │                     │RoutingKey:
                    │hotel.insert         │                     │hotel.delete
                    ▼                     │                     ▼
        ┌───────────────────────┐         │         ┌───────────────────────┐
        │  hotel.insert.queue   │         │         │  hotel.delete.queue   │
        │  (新增/修改队列)       │         │         │  (删除队列)            │
        └───────────┬───────────┘         │         └───────────┬───────────┘
                    │                     │                     │
                    ▼                     │                     ▼
        ┌───────────────────────┐         │         ┌───────────────────────┐
        │listenHotelInsertOrUpdate│       │         │  listenHotelDelete    │
        │     (消费者方法)        │         │        │    (消费者方法)        │
        └───────────────────────┘         │         └───────────────────────┘
                                          │
                                    ┌─────┴─────┐
                                    │ Listener  │
                                    └───────────┘
```

|    组件    |         名称         |               说明               |
|:----------:|:--------------------:|:--------------------------------:|
|  Exchange  |    `hotel.topic`     | Topic 类型交换机，支持通配符路由 |
|   Queue    | `hotel.insert.queue` |      监听酒店新增/修改消息       |
|   Queue    | `hotel.delete.queue` |         监听酒店删除消息         |
| RoutingKey |    `hotel.insert`    |      新增/修改操作的路由键       |
| RoutingKey |    `hotel.delete`    |         删除操作的路由键         |

## 快速开始

### 1. 环境准备

- **JDK 17+**
- **MySQL 8.x**
- **RabbitMQ 3.x** (默认端口 5672)
- **Elasticsearch 8.x** (或兼容版本)
    - 安装插件：`analysis-ik` (IK 中文分词器)
    - 安装插件：`analysis-pinyin` (拼音分词器)

### 2. 数据库配置

执行 `src/main/resources/sql/tb_hotel.sql` 脚本完成数据库初始化与测试数据导入

### 3. 修改配置

编辑 `src/main/resources/application.properties`：

```properties
# 服务端口
server.port=8089
# MySQL 配置
spring.datasource.url=jdbc:mysql://localhost:3306/elasticsearch
spring.datasource.username=root
spring.datasource.password=123
# Elasticsearch 配置
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=elastic
spring.elasticsearch.password=123456
# RabbitMQ 配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### 4. 启动项目

```bash
mvn spring-boot:run
```

访问地址：

- **用户端**：http://localhost:8089 （酒店搜索页面）
- **管理端**：http://localhost:8089/html/admin.html （后台管理页面）

## API 接口

### 搜索相关 (HotelController)

| 方法 |              URL               |                       说明                       |
|:----:|:------------------------------:|:------------------------------------------------:|
| POST |         `/hotel/list`          |       搜索酒店列表（支持分页、筛选、排序）       |
| POST |        `/hotel/filters`        | 获取当前搜索条件下的聚合筛选项（城市/星级/品牌） |
| GET  | `/hotel/suggestion?prefix=xxx` |                搜索关键词自动补全                |

### 管理相关 (AdminHotelController)

|  方法  |                  URL                  |             说明             |
|:------:|:-------------------------------------:|:----------------------------:|
|  GET   | `/admin/hotel/list?current=1&size=10` | 分页查询所有酒店（走数据库） |
|  GET   |          `/admin/hotel/{id}`          |         获取酒店详情         |
|  POST  |            `/admin/hotel`             |   新增酒店（自动同步 ES）    |
|  PUT   |            `/admin/hotel`             | 更新酒店信息（自动同步 ES）  |
| DELETE |          `/admin/hotel/{id}`          |   删除酒店（自动同步 ES）    |
|  PUT   |   `/admin/hotel/{id}/ad?isAd=true`    |      设置/取消广告状态       |

## 索引结构

酒店文档索引 `hotel` 核心字段说明：

|     字段     |      类型       |                      说明                      |
|:------------:|:---------------:|:----------------------------------------------:|
|     `id`     |     keyword     |                    酒店 ID                     |
|    `name`    | text (ik_smart) |                    酒店名称                    |
|  `location`  |    geo_point    |                   经纬度坐标                   |
|   `price`    |     integer     |                      价格                      |
|   `score`    |     integer     |                      评分                      |
|   `brand`    |     keyword     |                      品牌                      |
|    `city`    |     keyword     |                      城市                      |
|  `starName`  |     keyword     |                      星级                      |
|  `business`  |     keyword     |                      商圈                      |
|    `all`     |      text       | 组合搜索字段（copy_to: name, brand, business） |
| `suggestion` |   completion    |                  自动补全字段                  |
|    `isAd`    |     boolean     |                   是否为广告                   |

## 许可证

[MIT License](LICENSE)
