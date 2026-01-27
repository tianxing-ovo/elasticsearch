# Elasticsearch 酒店搜索系统

基于 Spring Boot 3.4.1 和 Elasticsearch 9.2.4 的酒店搜索系统，提供全文搜索、多条件筛选、地理位置排序等功能。

## 技术栈

- **Spring Boot** 3.4.1 - 基础框架
- **Spring Data Elasticsearch** 5.4.1 - Elasticsearch 集成
- **Elasticsearch** 9.2.4 - 搜索引擎
- **MyBatis-Plus** 3.5.9 - ORM 框架
- **MySQL** 8.x - 关系型数据库
- **Java** 17 - JDK 版本

## 项目结构

```
src/
├── main/
│   ├── java/com/ltx/
│   │   ├── Application.java              # 启动类
│   │   ├── constant/
│   │   │   └── Constant.java             # 常量定义
│   │   ├── controller/
│   │   │   └── HotelController.java      # 酒店控制器
│   │   ├── entity/
│   │   │   ├── Hotel.java                # 酒店实体（MySQL）
│   │   │   ├── HotelDoc.java             # 酒店文档（Elasticsearch）
│   │   │   ├── PageResult.java           # 分页结果
│   │   │   └── SearchRequestBody.java    # 搜索请求体
│   │   ├── mapper/
│   │   │   └── HotelMapper.java          # MyBatis Mapper
│   │   ├── repository/
│   │   │   └── HotelDocRepository.java   # Elasticsearch Repository
│   │   └── service/
│   │       ├── HotelService.java         # 服务接口
│   │       └── impl/
│   │           └── HotelServiceImpl.java # 服务实现（含距离计算）
│   └── resources/
│       ├── application.properties        # 配置文件
│       ├── logback.xml                   # 日志配置
│       ├── json/                         # JSON 配置
│       ├── mapper/                       # MyBatis XML
│       ├── sql/                          # SQL 脚本
│       └── static/                       # 静态资源
│           ├── index.html                # 前端页面
│           ├── css/                      # 样式文件
│           ├── js/                       # JavaScript
│           └── img/                      # 图片资源
└── test/java/com/ltx/
    └── HotelTest.java                    # 单元测试类
```

## 核心功能

### 查询类型

- **matchAllQuery()** - 查询所有文档
- **matchQuery(name, text)** - 全文匹配查询：对查询文本进行分词后匹配
- **multiMatchQuery(text, fieldNames)** - 多字段匹配查询：在多个字段中搜索
- **termQuery(name, value)** - 精确匹配查询：不分词精确匹配
- **rangeQuery(name, from, to)** - 范围查询：匹配指定范围内的文档
- **boolQuery(must, should, mustNot, filter)** - 布尔查询：组合多个查询条件
- **functionScoreQuery()** - 函数评分查询：自定义文档评分
- **geoDistanceQuery(name, lat, lon, distance)** - 地理距离查询：匹配指定距离范围内的文档

### 搜索功能

- **关键字搜索** - 支持酒店名称、地址、商圈等多字段搜索
- **条件筛选** - 按城市、品牌、星级、价格范围筛选
- **距离计算** - 使用 Haversine 公式计算用户到酒店的距离
- **距离排序** - 根据用户位置按距离排序
- **广告置顶** - 使用 function_score 提升广告酒店排名
- **聚合统计** - 动态获取筛选项的可选值

### 前端功能

- **自动定位** - 使用高德地图 API 获取用户位置
- **位置缓存** - 将位置信息缓存到 localStorage（1小时有效）
- **手动定位** - 支持用户手动触发重新定位
- **地图预览** - 在地图上显示酒店位置标记

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.x
- Elasticsearch 8.x

### 2. 数据库配置

创建数据库 `elasticsearch`，导入酒店数据表 `tb_hotel`。

### 3. 修改配置

编辑 `application.properties`：

```properties
# MySQL 配置
spring.datasource.url=jdbc:mysql://localhost:3306/elasticsearch
spring.datasource.username=root
spring.datasource.password=123

# Elasticsearch 配置
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=elastic
spring.elasticsearch.password=123456
```

### 4. 启动项目

```bash
mvn spring-boot:run
```

访问 http://localhost:8089 即可使用。

## API 接口

### 搜索酒店

```http
POST /hotel/list
Content-Type: application/json

{
  "key": "外滩",
  "pageNumber": 1,
  "pageSize": 10,
  "sortBy": "default",
  "sortOrder": "asc",
  "city": "上海",
  "brand": "",
  "starName": "",
  "minPrice": 100,
  "maxPrice": 500,
  "location": "31.2, 121.5"
}
```

**参数说明：**

| 参数         | 类型      | 必填 | 说明                                |
|------------|---------|----|-----------------------------------|
| key        | String  | 否  | 搜索关键字                             |
| pageNumber | Integer | 否  | 页码，默认 1                           |
| pageSize   | Integer | 否  | 每页数量，默认 10                        |
| sortBy     | String  | 否  | 排序方式：default/score/price/distance |
| sortOrder  | String  | 否  | 排序顺序：asc/desc                     |
| city       | String  | 否  | 城市筛选                              |
| brand      | String  | 否  | 品牌筛选                              |
| starName   | String  | 否  | 星级筛选                              |
| minPrice   | Integer | 否  | 最低价格                              |
| maxPrice   | Integer | 否  | 最高价格                              |
| location   | String  | 否  | 用户位置（纬度,经度），用于计算距离                |

### 获取筛选项

```http
POST /hotel/filters
Content-Type: application/json

{
  "key": "",
  "city": "上海"
}
```

## 索引映射

酒店文档索引 `hotel` 的映射结构：

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "name": { "type": "text", "analyzer": "ik_max_word", "copy_to": "all" },
      "address": { "type": "text", "analyzer": "ik_max_word", "copy_to": "all" },
      "price": { "type": "integer" },
      "score": { "type": "integer" },
      "brand": { "type": "keyword", "copy_to": "all" },
      "city": { "type": "keyword" },
      "starName": { "type": "keyword" },
      "business": { "type": "keyword", "copy_to": "all" },
      "location": { "type": "geo_point" },
      "pic": { "type": "keyword", "index": false },
      "all": { "type": "text", "analyzer": "ik_max_word" },
      "isAd": { "type": "boolean" }
    }
  }
}
```

## 许可证

MIT License
