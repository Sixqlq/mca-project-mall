package com.msb.mall.coupon.service.impl;

import com.msb.mall.coupon.entity.SeckillSkuRelationEntity;
import com.msb.mall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.coupon.dao.SeckillSessionDao;
import com.msb.mall.coupon.entity.SeckillSessionEntity;
import com.msb.mall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查询未来3天秒杀活动
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getLates3DaysSession() {
        // 计算未来3天时间
        List<SeckillSessionEntity> list = this.list(new QueryWrapper<SeckillSessionEntity>().between("start_time", startTime(), endTime()));
        List<SeckillSessionEntity> newList = list.stream().map(session -> {
            // 根据对应的sessionId活动编号查询出对应的活动商品信息
            List<SeckillSkuRelationEntity> relationEntities = seckillSkuRelationService.list(new QueryWrapper<SeckillSkuRelationEntity>()
                    .eq("promotion_session_id", session.getId()));
            session.setRelationEntities(relationEntities);
            return session;
        }).collect(Collectors.toList());
        return newList;
    }

    private String startTime(){
        LocalDate now  = LocalDate.now();
        LocalDate startDay = now.plusDays(0);
        LocalTime min = LocalTime.MIN;
        String startTime = LocalDateTime.of(startDay, min).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return startTime;
    }

    private String endTime(){
        LocalDate now  = LocalDate.now();
        LocalDate endDay = now.plusDays(2);
        LocalTime max = LocalTime.MAX;
        String endTime = LocalDateTime.of(endDay, max).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return endTime;
    }

}