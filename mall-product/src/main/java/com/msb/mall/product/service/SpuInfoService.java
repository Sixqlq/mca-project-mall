package com.msb.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.product.entity.SpuInfoEntity;
import com.msb.mall.product.vo.SpuInfoVO;

import java.util.Map;

/**
 * spu信息
 *
 * @author qlq
 * @email 2390608028@qq.com
 * @date 2022-07-31 18:08:55
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuInfoVO spuInfo);

    PageUtils queryPageByCondition(Map<String, Object> params);
}

