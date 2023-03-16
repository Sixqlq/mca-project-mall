package com.msb.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msb.common.constant.ProductConstant;
import com.msb.common.dto.MemberPrice;
import com.msb.common.dto.SkuHasStockDto;
import com.msb.common.dto.SkuReductionDTO;
import com.msb.common.dto.SpuBoundsDTO;
import com.msb.common.dto.es.SkuESModel;
import com.msb.common.utils.R;
import com.msb.mall.product.entity.*;
import com.msb.mall.product.feign.CouponFeignService;
import com.msb.mall.product.feign.SearchFeignService;
import com.msb.mall.product.feign.WareSkuFeignService;
import com.msb.mall.product.service.*;
import com.msb.mall.product.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
@Slf4j
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private WareSkuFeignService wareSkuFeignService;

    @Autowired
    private SearchFeignService searchFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存商品的发布信息
     * @param spuInfoVO
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuInfoVO spuInfoVO) {
        // 1. 保存spu的基本信息 pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuInfoVO, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.save(spuInfoEntity);

        // 2. 保存spu的详情信息 pms_spu_info_desc
        List<String> descripts = spuInfoVO.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",", descripts));
        spuInfoDescService.save(spuInfoDescEntity);

        // 3. 保存图集信息 pms_spu_images
        List<String> images = spuInfoVO.getImages();
        // 将每一个图片信息封装成一个SpuImagesEntity实体对象
        List<SpuImagesEntity> imagesEntities = images.stream().map(item -> {
            SpuImagesEntity entity = new SpuImagesEntity();
            entity.setSpuId(spuInfoEntity.getId());
            entity.setImgUrl(item);
            return entity;
        }).collect(Collectors.toList());
        spuImagesService.saveBatch(imagesEntities);

        // 4. 保存规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = spuInfoVO.getBaseAttrs();
        List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setSpuId(spuInfoEntity.getId()); // 关联商品编号
            valueEntity.setAttrId(attr.getAttrId());
            valueEntity.setAttrValue(attr.getAttrValues());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(attrEntity.getAttrName());
            valueEntity.setQuickShow(attr.getShowDesc());
            return valueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(productAttrValueEntities);

        // 5. 保存当前的spu对应的所有的sku信息
        List<Skus> skus = spuInfoVO.getSkus();
        if(skus != null && skus.size() > 0){
            // 5.1 保存sku的基本信息 pms_sku_info
            skus.forEach((item)->{
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSaleCount(0L);
                List<Images> images1 = item.getImages();
                String defaultImage = "";
                for (Images images2 : images1) {
                    if(images2.getDefaultImg() == 1){
                        // 表示是默认图片
                        defaultImage = images2.getImgUrl();
                    }
                }
                skuInfoEntity.setSkuDefaultImg(defaultImage);
                skuInfoService.save(skuInfoEntity);
                // 5.2 保存sku的图片信息 pms_sku_image
                List<SkuImagesEntity> skuImagesEntities = images1.stream().map(img -> {
                    SkuImagesEntity entity = new SkuImagesEntity();
                    entity.setSkuId(skuInfoEntity.getSkuId());
                    entity.setImgUrl(img.getImgUrl());
                    entity.setDefaultImg(img.getDefaultImg());
                    return entity;
                }).filter(img -> {
                    return img.getDefaultImg() == 1; // 为空的图片不需要保存
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(skuImagesEntities);
                // 5.3 保存满减信息，折扣，会员价 mall-sms: sms_sku_ladder sms_full_reduction sms_member_price
                SkuReductionDTO dto = new SkuReductionDTO(); // TODO memberPrice is null
                BeanUtils.copyProperties(item, dto);
                dto.setSkuId(skuInfoEntity.getSkuId());
                if(item.getMemberPrice() != null && item.getMemberPrice().size() > 0){
                    List<MemberPrice> list = item.getMemberPrice().stream().map(memberPrice -> {
                        MemberPrice mdto = new MemberPrice();
                        BeanUtils.copyProperties(memberPrice, mdto);
                        return mdto;
                    }).collect(Collectors.toList());
                    dto.setMemberPrice(list);
                }
                R r = couponFeignService.saveFullReductionInfo(dto);
                if(r.getCode() != 0){
                    log.error("调用coupon服务处理满减、折扣、会员价操作失败");
                }
                // 5.4 sku的销售属性信息 pms_sku_sale_attr_value
                List<Attr> attrs = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attrs.stream().map(sale -> {
                    SkuSaleAttrValueEntity entity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(sale, entity);
                    entity.setSkuId(skuInfoEntity.getSkuId());
                    return entity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

            });

        }


        // 6. 保存spu的积分信息: mall-sms: sms_spu_bounds
        Bounds bounds = spuInfoVO.getBounds();
        SpuBoundsDTO spuBoundsDTO = new SpuBoundsDTO();
        BeanUtils.copyProperties(bounds, spuBoundsDTO);
        spuBoundsDTO.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundsDTO);
        if(r.getCode() != 0){
            log.error("调用coupon服务存储积分信息操作失败");
        }

    }

    /**
     * spu信息检索
     * 分页查询
     * 分类 品牌 状态 关键字查询
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        // 设置队友的检索条件
        // 1. 关键字查询
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            // 1. 添加关键字查询
            wrapper.and((w)->{
                w.eq("id", key)
                        .or().like("spu_name", key)
                        .or().like("spu_description", key);
            });
        }
        // status
        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status", status);
        }

        // catalog_id
        String catalogId = (String) params.get("catalogId");
        if(!StringUtils.isEmpty(catalogId) && !"0".equalsIgnoreCase(catalogId)){
            wrapper.eq("catalog_id", catalogId);
        }
        // brandId
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
        // 根据查询到的分页信息， 再查询出对应的类别名称和品牌名称
        List<SpuInfoVO> voList = page.getRecords().stream().map(spuInfoEntity -> {
            Long catalogId1 = spuInfoEntity.getCatalogId();
            CategoryEntity categoryEntity = categoryService.getById(catalogId1);
            Long brandId1 = spuInfoEntity.getBrandId();
            BrandEntity brandEntity = brandService.getById(brandId1);
            SpuInfoVO vo = new SpuInfoVO();
            BeanUtils.copyProperties(spuInfoEntity, vo);
            vo.setCatalogName(categoryEntity.getName());
            vo.setBrandName(brandEntity.getName());
            return vo;
        }).collect(Collectors.toList());
        IPage<SpuInfoVO> spuInfoVOIPage = new Page<SpuInfoVO>();
        spuInfoVOIPage.setRecords(voList);
        spuInfoVOIPage.setPages(page.getPages());
        spuInfoVOIPage.setCurrent(page.getCurrent());
        return new PageUtils(spuInfoVOIPage);

    }

    /**
     * 实现商品上架 -》 商品相关数据存储到ES中
     * 1. 根据spuId查询出相关信息封装到对应的对象
     * 2. 将封装的数据存储到ES中，调用mall-search的远程接口
     * 3. 更新spuId对应的状态-》上架
     * @param spuId
     */
    @Override
    public void up(Long spuId) {
        // 1. 根据spuId查询出相关信息封装到对应的对象
        List<SkuESModel> skuEs = new ArrayList<>();
        // 根据spuId找到相关的sku信息
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        // 对应的规格和参数 根据spuId查询规格参数信息
        List<SkuESModel.Attrs> attrsModel = getAttrsModel(spuId);
        // 需要根据所有的spuId获取对应的库存信息--》远程调用
        List<Long> skuIds = skus.stream().map(sku -> {
            return sku.getSkuId();
        }).collect(Collectors.toList());
        Map<Long, Boolean> skusHasStockMap = getSkusHasStock(skuIds);

        // 2. 远程调用mall-search的远程接口，将封装的数据存储到ES中
        List<SkuESModel> skuESModels = skus.stream().map(item -> {
            SkuESModel model = new SkuESModel();
            // 先实现属性复制
            BeanUtils.copyProperties(item, model);
            model.setSubTitle(item.getSkuTitle());
            model.setSkuPrice(item.getPrice());
            model.setSkuImg(item.getSkuDefaultImg());

            // hasStock 是否有库存 --》 库存系统查询 一次远程调用获取所有的skuId对应的库存信息
            if(skusHasStockMap == null){
                model.setHasStock(false);
            }else {
                model.setHasStock(skusHasStockMap.get(item.getSkuId()));
            }
            // hotScore 热度分 --》 默认给0
            model.setHotScore(0l);
            // 品牌和类型的名称
            BrandEntity brand = brandService.getById(item.getBrandId());
            CategoryEntity category = categoryService.getById(item.getCatalogId());
            model.setBrandName(brand.getName());
            model.setBrandImg(brand.getLogo());
            model.setCatalogName(category.getName());
            // 需要存储的规格参数数据
            model.setAttrs(attrsModel);
            return model;
        }).collect(Collectors.toList());
        // 将SkuESModel中的数据存储到ES中
        R r = searchFeignService.productStartUp(skuESModels);
        log.info("----->ES操作完成：{}", r.getCode());
        // 3. 更新SpuId对应的状态
        if(r.getCode() == 0){
            // 远程调用成功 更新商品的状态为上架
            baseMapper.updateSpuStatusUp(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else {
            // 远程调用失败
        }
    }

    @Override
    public List<OrderItemSpuInfoVO> getOrderItemSpuInfoBySpuId(Long[] spuIds) {
        List<OrderItemSpuInfoVO> list = new ArrayList<>();
        // 根据spuId查询出相关信息
        for (Long spuId : spuIds) {
            OrderItemSpuInfoVO vo = new OrderItemSpuInfoVO();
            SpuInfoEntity spuInfoEntity = this.getById(spuId);
            vo.setId(spuId);
            vo.setSpuName(spuInfoEntity.getSpuName());
            vo.setBrandId(spuInfoEntity.getBrandId());
            vo.setCatalogId(spuInfoEntity.getCatalogId());
            // 根据品牌编号查询品牌信息
            BrandEntity brand = brandService.getById(spuInfoEntity.getBrandId());
            vo.setBrandName(brand.getName());
            // 根据类别编号查询品牌信息
            CategoryEntity category = categoryService.getById(spuInfoEntity.getCatalogId());
            vo.setCatalogName(category.getName());
            // 获取spu图片
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);
            vo.setImg(spuInfoDesc.getDecript());
            list.add(vo);
        }
        return list;
    }

    /**
     * 根据spuId获取对应的规格参数
     * @param spuId
     * @return
     */
    private List<SkuESModel.Attrs> getAttrsModel(Long spuId) {
        // 1. product_attr_value  存储了对应的spu相关的所有的规格参数
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrsForSpuId(spuId);
        // 2. attr search_type 决定了该属性是否支持检索
        List<Object> attrIds = baseAttrs.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        // 查询出所有的可以检索的对应的规格参数编号
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        // baseAttrs 中根据可以检索的数据过滤
        List<SkuESModel.Attrs> attrsModel = baseAttrs.stream().filter(item -> {
            return searchAttrIds.contains(item.getAttrId());
        }).map(item -> {
            SkuESModel.Attrs attr = new SkuESModel.Attrs();
//            attr.setAttrId(item.getAttrId());
//            attr.setAttrName(item.getAttrName());
//            attr.setAttrValue(item.getAttrValue());
            BeanUtils.copyProperties(item, attr);
            return attr;
        }).collect(Collectors.toList());
        return attrsModel;
    }

    /**
     * 根据skuIds获取对应的库存状态
     * @param skuIds
     * @return
     */
    private Map<Long, Boolean> getSkusHasStock(List<Long> skuIds){
        List<SkuHasStockDto> skusHasStock = null;
        if(skuIds == null || skuIds.size() == 0) {
            return null;
        }
        try {
            // 调用远程接口获取对应的库存信息
            skusHasStock = wareSkuFeignService.getSkusHasStock(skuIds);
            Map<Long, Boolean> map = skusHasStock.stream().
                    collect(Collectors.toMap(SkuHasStockDto :: getSkuId, SkuHasStockDto::getHasStock));
            return map;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}