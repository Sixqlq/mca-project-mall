package com.msb.mall.product;

import com.aliyun.oss.OSSClient;
import com.msb.mall.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;



@SpringBootTest(classes = MallProductApplication.class)
public class MallProductApplicationTests {
	@Autowired(required = false)
	private OSSClient ossClient;

	@Autowired
	private CategoryService categoryService;

	@Test
	public void testUploadFile() throws FileNotFoundException {
//		// yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
//		String endpoint = "oss-cn-hangzhou.aliyuncs.com";
//		// 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
//		String accessKeyId = "LTAI5tAj7JgXViru9GhUhaCh";
//		String accessKeySecret = "3TcvJXYSy6IaSZOjgCwORQA4Z0SCxG";
//
//		// 创建OSSClient实例。
//		OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

		// 填写本地文件的完整路径。如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
		InputStream inputStream = new FileInputStream("D:\\MavenProject\\project_images\\project-mall\\2.jpg");
		// 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
		ossClient.putObject("qlq-mall", "2.jpg", inputStream);

		// 关闭OSSClient。
		ossClient.shutdown();
		System.out.println("上传图片成功...");
	}

	@Test
	public void test1(){
		Long[] catelogPath = categoryService.findCatelogPath(762l);
		for (Long along:catelogPath) {
			System.out.println(along);
		}
	}

}
