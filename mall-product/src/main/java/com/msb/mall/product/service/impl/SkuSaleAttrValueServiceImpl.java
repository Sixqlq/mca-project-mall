package com.msb.mall.product.service.impl;

import com.msb.mall.product.vo.ItemVO;
import com.msb.mall.product.vo.SkuItemSaleAttrVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.product.dao.SkuSaleAttrValueDao;
import com.msb.mall.product.entity.SkuSaleAttrValueEntity;
import com.msb.mall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 根据spuId获取所有的skuId，然后获取所有skuId对应的SkuItemSaleAttrVO
     * 按照attr_id attr_name 分组
     * 对attr_value需要去重，因为多个skuId对应的attr_value可能重复, 并且使用GROUP_CONCAT将多个attrValue拼接成一个字符串
     * @param spuId
     * @return
     */
    @Override
    public List<SkuItemSaleAttrVO> getSkuSaleAttrValueBySpuId(Long spuId) {
        return baseMapper.getSkuSaleAttrValueBySpuId(spuId);
    }
}