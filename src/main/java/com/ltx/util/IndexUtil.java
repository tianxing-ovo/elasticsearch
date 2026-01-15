package com.ltx.util;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 索引工具类
 *
 * @author tianxing
 */
@SuppressWarnings("unused")
@Component
public class IndexUtil {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * 创建索引
     *
     * @param clazz 索引类
     */
    public void createIndex(Class<?> clazz) {
        if (exists(clazz)) {
            System.out.println("索引已存在");
            return;
        }
        if (elasticsearchOperations.indexOps(clazz).createWithMapping()) {
            System.out.println("创建索引成功");
        }
    }

    /**
     * 删除索引
     *
     * @param clazz 索引类
     */
    public void deleteIndex(Class<?> clazz) {
        if (elasticsearchOperations.indexOps(clazz).delete()) {
            System.out.println("删除索引成功");
        }
    }

    /**
     * 判断索引是否存在
     *
     * @param clazz 索引类
     * @return 是否存在
     */
    public boolean exists(Class<?> clazz) {
        return elasticsearchOperations.indexOps(clazz).exists();
    }

    /**
     * 查询索引
     *
     * @param clazz 索引类
     */
    public void queryIndex(Class<?> clazz) {
        System.out.println(elasticsearchOperations.indexOps(clazz).getMapping());
    }
}
