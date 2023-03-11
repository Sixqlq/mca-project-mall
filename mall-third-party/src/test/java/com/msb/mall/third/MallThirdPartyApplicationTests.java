package com.msb.mall.third;

import com.aliyun.oss.OSSClient;
import com.msb.mall.third.utils.HttpUtils;
import com.msb.mall.third.utils.SMSComponent;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class MallThirdPartyApplicationTests {
	@Autowired(required = false)
	private OSSClient ossClient;

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
		InputStream inputStream = new FileInputStream("D:\\MavenProject\\project_images\\project-mall\\5.jpg");
		// 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
		ossClient.putObject("qlq-mall", "5.jpg", inputStream);

		// 关闭OSSClient。
		ossClient.shutdown();
		System.out.println("上传图片成功...");
	}

	@Autowired
	private SMSComponent component;

	/**
	 * 测试短信组件类
	 */
	@Test
	public void testSendSMS2(){
		component.sendSMSCode("15251891599", "123456");
	}

	/**
	 * 测试阿里云短信服务API
	 */
	@Test
	public void testSendSMS(){
		String host = "https://dfsns.market.alicloudapi.com";
		String path = "/data/send_sms";
		String method = "POST";
		String appcode = "8bbf0cda1bfa4430a3c1776134aa3806";
		Map<String, String> headers = new HashMap<String, String>();
		//最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
		headers.put("Authorization", "APPCODE " + appcode);
		//根据API的要求，定义相对应的Content-Type
		headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		Map<String, String> querys = new HashMap<String, String>();
		Map<String, String> bodys = new HashMap<String, String>();
		bodys.put("content", "code:1234");
		bodys.put("phone_number", "15251891599");
		bodys.put("template_id", "CST_ptdie100");

		try {
			/**
			 * 重要提示如下:
			 * HttpUtils请从
			 * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
			 * 下载
			 *
			 * 相应的依赖请参照
			 * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
			 */
			HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
			System.out.println(response.toString());
			//获取response的body
			//System.out.println(EntityUtils.toString(response.getEntity()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
