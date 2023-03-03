package com.msb.mall.mallsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msb.mall.mallsearch.config.MallElasticSearchConfiguration;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MallSearchApplicationTests {

	@Autowired
	private RestHighLevelClient client;

	/**
	 * 测试连接ES服务器
	 */
	@Test
	void contextLoads() {
		System.out.println("--->" + client);
	}


	/**
	 * 测试保存文档
	 */
	@Test
	void saveIndex() throws Exception {
		IndexRequest indexRequest = new IndexRequest("system");
		indexRequest.id("1");
//		indexRequest.source("name", "bobo", "age", "18", "gender", "男");
		User user = new User();
		user.setName("bobo");
		user.setAge(22);
		user.setGender("男");
		// 用Jackson中的对象转json数据
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(user);
		indexRequest.source(json, XContentType.JSON);
		// 执行操作
		IndexResponse index = client.index(indexRequest, MallElasticSearchConfiguration.COMMON_OPTIONS);
		// 提取有用的返回信息
		System.out.println(index);
	}

	@Data
	class User{
		private String name;
		private Integer age;
		private String gender;
	}

	/**
	 * 案例1.检索出所有的bank索引的所有文档
	 */
	@Test
	void searchIndexAll() throws Exception{
		// 1.创建一个 SearchRequest 对象
		// 2.如何执行检索操作
		// 3.获取检索后的响应对象，我们需要解析出我们关心的数据

	}

	/**
	 * 案例2.根据address全文检索
	 */
	@Test
	void searchIndexByAddress(){
		// 1.创建一个 SearchRequest 对象
		// 2.如何执行检索操作
		// 3.获取检索后的响应对象，我们需要解析出我们关心的数据
	}

	/**
	 * 案例3.嵌套的聚合操作：检索出bank下的年龄分布和每个年龄段的平均薪资
	 */
	@Test
	void searchIndexAggregation(){

	}

	/**
	 * 案例4.并行的聚合操作：查询出bank下年龄段的分布和总的平均薪资
	 */
	@Test
	void searchIndexAggregation1(){

	}

	/**
	 * 案例5.处理检索后的结果
	 */
	@Test
	void searchIndexResponse(){

	}
}
