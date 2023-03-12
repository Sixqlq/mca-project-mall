package com.msb.mall.member.vo;

import lombok.Data;

@Data
public class SocialUser {
    private String accessToken; // token信息
    private long remindIn;
    private long expiresIn; // 过期时间
    private String uid; // 用户识别编号
    private boolean isRealName;
}
