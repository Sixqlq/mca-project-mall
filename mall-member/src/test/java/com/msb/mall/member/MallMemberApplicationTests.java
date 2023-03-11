package com.msb.mall.member;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class MallMemberApplicationTests {

	/**
	 * 测试加密方法
	 */
	@Test
	void contextLoads() {
		String s = DigestUtils.md2Hex("123456");
		// d4541250b586296fcce5dea4463ae17f
		// d4541250b586296fcce5dea4463ae17f
		System.out.println("s = " + s);
		// 加盐处理
		// $1$lkcSfhKz$YIh787MPndl6UiBsvNMGP0
		// $1$LmNF8qy7$yINxBoOzsWflC/5ZS6QUV/
		String s1 = Md5Crypt.md5Crypt("123456".getBytes());
		System.out.println("s1 = " + s1);

		//$1$666$O89F8Qw2bIQHvHSYQbN2e.
		//$1$666$O89F8Qw2bIQHvHSYQbN2e.
		String s2 = Md5Crypt.md5Crypt("123456".getBytes(), "$1$abc@123");
		System.out.println("s2 = " + s2);

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String encode1 = encoder.encode("123456");
		String encode2 = encoder.encode("123456");
		String encode3 = encoder.encode("123456");
		System.out.println("encode1 = " + encode1);
		System.out.println("encode2 = " + encode2);
		System.out.println("encode3 = " + encode3);

		// BCryptPasswordEncoder加密每次的密文都不一样，安全性比较高，根据encoder.matches方法比较密码明文和加密后的密文是否相等
		System.out.println("encoder.matches(\"123456\", encode1) = " + encoder.matches("123456", encode1));
		System.out.println("encoder.matches(\"123456\", encode2) = " + encoder.matches("123456", encode2));
		System.out.println("encoder.matches(\"123456\", encode3) = " + encoder.matches("123456", encode3));
	}

}
