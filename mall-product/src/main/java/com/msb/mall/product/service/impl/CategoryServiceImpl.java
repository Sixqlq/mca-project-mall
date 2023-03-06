package com.msb.mall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.msb.mall.product.service.CategoryBrandRelationService;
import com.msb.mall.product.vo.Catalog2VO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.product.dao.CategoryDao;
import com.msb.mall.product.entity.CategoryEntity;
import com.msb.mall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查询所有的类别数据，然后将数据封装为树形结构，便于前端使用
     * @param params
     * @return
     */
    @Override
    public List<CategoryEntity> queryPageWithTree(Map<String, Object> params) {
        // 1. 查询所有的商品分类信息
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        // 2. 将商品分类信息拆解为树形结构【父子关系】
        // 第一步遍历出所有的大类 parent_cid = 0
        List<CategoryEntity> list = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map(categoryEntity -> {
                    // 根据大类找到所有的小类 递归的方式实现
                    categoryEntity.setChildren(getCategoryChildren(categoryEntity, categoryEntities));
                    return categoryEntity;
                }).sorted((entity1, entity2) -> {
                    return (entity1.getSort() == null ? 0 : entity1.getSort()) - (entity2.getSort() == null ? 0 : entity2.getSort());
                }).collect(Collectors.toList());
        // 第二步根据大类找到所有的小类
        return list;
    }

    /**
     * 查找该大类下的所有小类
     * @param categoryEntity 某个大类
     * @param categoryEntities 所有的类别数据
     * @return
     */
    private List<CategoryEntity> getCategoryChildren(CategoryEntity categoryEntity, List<CategoryEntity> categoryEntities) {
        List<CategoryEntity> collect = categoryEntities.stream().filter(entity -> {
            // 根据大类找到直属他的小类
            return entity.getParentCid() == categoryEntity.getCatId();
        }).map(entity -> {
            // 根据小类递归找到对应的小小类
            entity.setChildren(getCategoryChildren(entity, categoryEntities));
            return entity;
        }).sorted((entity1, entity2) -> {
            return (entity1.getSort() == null ? 0 : entity1.getSort()) - (entity2.getSort() == null ? 0 : entity2.getSort());
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 逻辑批量删除操作
     * @param ids
     */
    @Override
    public void removeCategoryByIds(List<Long> ids) {
        // TODO  1.检查类别数据是否在其他业务中使用
        // 2.批量逻辑删除操作
        baseMapper.deleteBatchIds(ids);

    }

    /**
     * 查询三级分类路径
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     *  @CacheEvict: 在更新数据的时候同步删除缓存中的数据
     *  @CacheEvict(value = "category", allEntries = true)表示删除category分区下所有的缓存数据
     * 更新类别名称
     * @param entity
     */
//    @CacheEvict(value = "category",key = "'getLevel1Category'")
//    @Caching(evict = {
//            @CacheEvict(value = "category",key = "'getLevel1Category'"),
//            @CacheEvict(value = "category",key = "'getCatalog2Json'")
//    })
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateDetail(CategoryEntity entity) {
        // 更新类别名称
        this.updateById(entity);
        if(!StringUtils.isEmpty(entity.getName())){
            // 同步更新级联的数据
            categoryBrandRelationService.updateCatelogName(entity.getCatId(), entity.getName());
            // TODO 同步更新其他的冗余数据
        }
    }

    /**
     * 查询出所有的商品大类（一级分类）
     * 在Cacheable注解中可以指定对应的缓存名称，起到一个分区的作用，一般按照业务来区分
     * @Cacheable({"category", "product"})代表当前的方法返回结果是需要缓存的
     * 调用该方法的时候，如果缓存中有数据，那么该方法就不会执行，如果缓存中没有数据，那么就执行该方法并且把查询结果缓存起来
     *
     * 缓存处理
     *      1. 存储在Redis中的缓存数据的key是默认生成的, 缓存名称::SimpleKey[]
     *      2. 默认缓存的数据过期时间是-1永久
     *      3. 缓存的数据，默认使用的是jdk的序列化机制
     *  改进
     *      1. 生成的缓存数据我们需要指定自定义的key: key属性指定，可以'字符串'定义或者通过SPEL表达式处理: #root.method.name
     *      2. 指定缓存数据的过期时间: spring.cache.redis.time-to-live 指定过期时间
     *      3. 把缓存的数据保存为JSON数据
     *   SpringCache原理
     *      CacheAutoConfiguration--》根据指定的spring.cache.type=redis会导入RedisCacheConfiguration
     * @return
     */
    @Cacheable(value = {"category"},key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLevel1Category() {
        long start = System.currentTimeMillis();
        List<CategoryEntity> list = baseMapper.queryLevel1Category();
        System.out.println("查询消耗时间:" + (System.currentTimeMillis() - start));
        return list;
    }

    /**
     * 跟进父编号获取对应的子菜单信息
     * @param list
     * @param parentCid
     * @return
     */
    private List<CategoryEntity> queryByParentCid(List<CategoryEntity> list, Long parentCid){
        List<CategoryEntity> collect = list.stream().filter(item -> {
            return item.getParentCid().equals(parentCid);
        }).collect(Collectors.toList());
        return collect;
    }

    // 本地缓存
    private Map<String, Map<String, List<Catalog2VO>>> cache = new HashMap<>();

    /**
     * 查询所有分类数据，使用Spring Cache缓存
     * @return
     */
    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String, List<Catalog2VO>> getCatalog2Json() {
        // 获取所有的分类数据
        List<CategoryEntity> list = baseMapper.selectList(new QueryWrapper<CategoryEntity>());
        // 获取一级分类的数据
        List<CategoryEntity> level1Category = this.queryByParentCid(list, 0l);
        // 把一级分类的数据转化为map容器 key就是一级分类的编号 value就是一级分类对应的二级分类的数据
        Map<String, List<Catalog2VO>> map = level1Category.stream().collect(Collectors.toMap(
                key -> key.getCatId().toString(),
                value -> {
                    // 根据一级分类的编号，查询出所有的二级分类的数据
                    List<CategoryEntity> l2Catalogs = this.queryByParentCid(list, value.getCatId());
                    // 需要把查询出来的二级分类的数据填充到对应的Catalog2VO中
                    List<Catalog2VO> catalog2VOS = null;
                    if (l2Catalogs != null) {
                        catalog2VOS = l2Catalogs.stream().map(l2 -> {
                            Catalog2VO catalog2VO = new Catalog2VO(l2.getParentCid().toString(), null, l2.getCatId().toString(), l2.getName());
                            // 根据二级分类的数据找到对应的三级分类的数据
                            List<CategoryEntity> l3Catalogs = this.queryByParentCid(list, l2.getCatId());
                            if (l3Catalogs != null) {
                                List<Catalog2VO.Catalog3VO> catalog3VOs = l3Catalogs.stream().map(l3 -> {
                                    Catalog2VO.Catalog3VO catalog3VO = new Catalog2VO.Catalog3VO(l3.getParentCid().toString(), l3.getCatId().toString(), l3.getName());
                                    return catalog3VO;
                                }).collect(Collectors.toList());
                                // 三级分类关联二级分类
                                catalog2VO.setCatalog3List(catalog3VOs);
                            }
                            return catalog2VO;
                        }).collect(Collectors.toList());
                    }
                    return catalog2VOS;
                }));
        return map;
    }


    /**
     * 查询出所有的二级和三级分类的数据
     * @return
     */
    public Map<String, List<Catalog2VO>> getCatalog2JsonRedis() {
        String key  = "catalogJson";
        // 从redis中获取分类的信息
        String catalogJson = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isEmpty(catalogJson)){
            System.out.println("缓存没有命中...");
            // 缓存中没有数据，需要从数据库中查询
            Map<String, List<Catalog2VO>> catalog2JsonForDB = getCatalog2JsonDBWithRedisson();
            if(catalog2JsonForDB == null){
                // 数据库也不存在数据 防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "1", 5, TimeUnit.SECONDS);
            }else {
                // 从数据库中查询到的数据，需要在缓存中也存取一份
                // 防止缓存雪崩
                String json = JSON.toJSONString(catalog2JsonForDB);
                stringRedisTemplate.opsForValue().set(key, json, 10, TimeUnit.MINUTES);
            }
            return catalog2JsonForDB;
        }
        System.out.println("缓存命中了...");
        // 表示缓存命中了数据，那么从缓存中获取信息，然后返回
        // TypeReference 类构造方法被protected 关键字修饰，不能直接引用，所以需要创建子类去继承TypeReference类
        Map<String, List<Catalog2VO>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2VO>>>(){});
        return stringListMap;
    }

    /**
     * Redisson实现分布式锁查询二级和三级分类数据
     * @return
     */
    public Map<String, List<Catalog2VO>> getCatalog2JsonDBWithRedisson() {
        String keys = "catalogJson";
        // 获取分布式锁对象 加锁的时候，锁的名称一定要注意粒度
        // 商品信息 product-lock  product-1001-lock
        RLock lock = redissonClient.getLock("catalogJson-lock");
        Map<String, List<Catalog2VO>> data = null;
        try {
            lock.lock();
            // 加锁成功
            data = getDataForDB(keys);
        } finally {
            lock.unlock();
        }
        return data;
    }

    /**
     * Redis实现分布式锁查询二级和三级分类数据
     * @return
     */
    public Map<String, List<Catalog2VO>> getCatalog2JsonDBWithRedisLock() {
        String keys = "catalogJson";
        // 设置uuid在释放锁时通过比较uuid确定释放的是自己的锁
        String uuid = UUID.randomUUID().toString();
        // 加锁, 执行插入操作的时候设置过期时间，保证setNx和设置过期时间为原子性操作，防止出现死锁
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if(lock){
            System.out.println("获取锁成功...");
            Map<String, List<Catalog2VO>> data = null;
            try{
                // 加锁成功
                data = getDataForDB(keys);
            }finally {
                String scripts = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end ";
                // 通过redis的lua脚本实现 查询和删除操作的原子性
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(scripts, Long.class), Arrays.asList("lock"), uuid);
            }
//            String val = stringRedisTemplate.opsForValue().get("lock");
//            if(uuid.equals(val)){
//                // 说明这把锁是自己的
//                // 从数据库中获取数据成功后释放锁
//                stringRedisTemplate.delete("lock");
//            }
            return data;
        }else{
            // 加锁失败
            // 休眠+重试
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("获取锁失败...");
            return getCatalog2JsonDBWithRedisLock();
        }
    }

    /**
     * 从数据库中查询二级和三级分类数据
     * @param keys
     * @return
     */
    private Map<String, List<Catalog2VO>> getDataForDB(String keys) {
        // 先去缓存中查询有没有数据，如果有就返回，否则查询数据库
        // 从Redis中获取分类的信息
        String catalogJson = stringRedisTemplate.opsForValue().get(keys);
        if(!StringUtils.isEmpty(catalogJson)){
            // 说明缓存命中, 从缓存中获取信息直接返回
            Map<String, List<Catalog2VO>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2VO>>>(){});
            return stringListMap;
        }
        System.out.println("------> 缓存没有命中，开始查询数据库");
        // TODO: 2023/3/5 多次查询数据库需要优化
        // 获取所有的分类数据
        List<CategoryEntity> list = baseMapper.selectList(new QueryWrapper<CategoryEntity>());
        // 获取一级分类的数据
        List<CategoryEntity> level1Category = this.queryByParentCid(list, 0l);
        // 把一级分类的数据转化为map容器 key就是一级分类的编号 value就是一级分类对应的二级分类的数据
        Map<String, List<Catalog2VO>> map = level1Category.stream().collect(Collectors.toMap(
                key -> key.getCatId().toString(),
                value -> {
                    // 根据一级分类的编号，查询出所有的二级分类的数据
                    List<CategoryEntity> l2Catalogs = this.queryByParentCid(list, value.getCatId());
                    // 需要把查询出来的二级分类的数据填充到对应的Catalog2VO中
                    List<Catalog2VO> catalog2VOS = null;
                    if (l2Catalogs != null) {
                        catalog2VOS = l2Catalogs.stream().map(l2 -> {
                            Catalog2VO catalog2VO = new Catalog2VO(l2.getParentCid().toString(), null, l2.getCatId().toString(), l2.getName());
                            // 根据二级分类的数据找到对应的三级分类的数据
                            List<CategoryEntity> l3Catalogs = this.queryByParentCid(list, l2.getCatId());
                            if (l3Catalogs != null) {
                                List<Catalog2VO.Catalog3VO> catalog3VOs = l3Catalogs.stream().map(l3 -> {
                                    Catalog2VO.Catalog3VO catalog3VO = new Catalog2VO.Catalog3VO(l3.getParentCid().toString(), l3.getCatId().toString(), l3.getName());
                                    return catalog3VO;
                                }).collect(Collectors.toList());
                                // 三级分类关联二级分类
                                catalog2VO.setCatalog3List(catalog3VOs);
                            }
                            return catalog2VO;
                        }).collect(Collectors.toList());
                    }
                    return catalog2VOS;
                }));
        // TODO: 2023/3/5 与getCatalog2Json方法代码重复需要优化
        if(map == null){
            // 数据库也不存在数据 防止缓存穿透
            stringRedisTemplate.opsForValue().set(keys, "1", 5, TimeUnit.SECONDS);
        }else {
            // 从数据库中查询到的数据，需要在缓存中也存取一份
            // 防止缓存雪崩
            String json = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set(keys, json, 10, TimeUnit.MINUTES);
        }
        return map;
    }


    /**
     * 从数据库查询出所有的二级和三级分类的数据并封装为Map<String, List<Catalog2VO>>对象（加本地锁synchronized防止缓存击穿）
     * ps: 全量查询如果是菜单其实数据量不会太大，如果数据量很多，肯定是建议多级查询，如果数据量很大，有要全量查询，就把数据缓存在Redis中，放内存中肯定不合适
     * 在SpringBoot中默认情况是单例
     * @return
     */
    public Map<String, List<Catalog2VO>> getCatalog2JsonForDB() {
        String keys = "catalogJson";
        // 加锁防止缓存击穿
        synchronized (this){
            // 先去缓存中查询有没有数据，如果有就返回，否则查询数据库
            // 从Redis中获取分类的信息
            String catalogJson = stringRedisTemplate.opsForValue().get(keys);
            if(!StringUtils.isEmpty(catalogJson)){
                // 说明缓存命中, 从缓存中获取信息直接返回
                Map<String, List<Catalog2VO>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2VO>>>(){});
                return stringListMap;
            }
            System.out.println("------> 缓存没有命中，开始查询数据库");
            // TODO: 2023/3/5 多次查询数据库需要优化
            // 获取所有的分类数据
            List<CategoryEntity> list = baseMapper.selectList(new QueryWrapper<CategoryEntity>());
            // 获取一级分类的数据
            List<CategoryEntity> level1Category = this.queryByParentCid(list, 0l);
            // 把一级分类的数据转化为map容器 key就是一级分类的编号 value就是一级分类对应的二级分类的数据
            Map<String, List<Catalog2VO>> map = level1Category.stream().collect(Collectors.toMap(
                    key -> key.getCatId().toString(),
                    value -> {
                        // 根据一级分类的编号，查询出所有的二级分类的数据
                        List<CategoryEntity> l2Catalogs = this.queryByParentCid(list, value.getCatId());
                        // 需要把查询出来的二级分类的数据填充到对应的Catalog2VO中
                        List<Catalog2VO> catalog2VOS = null;
                        if (l2Catalogs != null) {
                            catalog2VOS = l2Catalogs.stream().map(l2 -> {
                                Catalog2VO catalog2VO = new Catalog2VO(l2.getParentCid().toString(), null, l2.getCatId().toString(), l2.getName());
                                // 根据二级分类的数据找到对应的三级分类的数据
                                List<CategoryEntity> l3Catalogs = this.queryByParentCid(list, l2.getCatId());
                                if (l3Catalogs != null) {
                                    List<Catalog2VO.Catalog3VO> catalog3VOs = l3Catalogs.stream().map(l3 -> {
                                        Catalog2VO.Catalog3VO catalog3VO = new Catalog2VO.Catalog3VO(l3.getParentCid().toString(), l3.getCatId().toString(), l3.getName());
                                        return catalog3VO;
                                    }).collect(Collectors.toList());
                                    // 三级分类关联二级分类
                                    catalog2VO.setCatalog3List(catalog3VOs);
                                }
                                return catalog2VO;
                            }).collect(Collectors.toList());
                        }
                        return catalog2VOS;
                    }));
            // TODO: 2023/3/5 与getCatalog2Json方法代码重复需要优化
            if(map == null){
                // 数据库也不存在数据 防止缓存穿透
                stringRedisTemplate.opsForValue().set(keys, "1", 5, TimeUnit.SECONDS);
            }else {
                // 从数据库中查询到的数据，需要在缓存中也存取一份
                // 防止缓存雪崩
                String json = JSON.toJSONString(map);
                stringRedisTemplate.opsForValue().set(keys, json, 10, TimeUnit.MINUTES);
            }
            return map;
        }
    }

    /**
     * 225,22,2
     * @param catelogId
     * @param paths
     * @return
     */
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity entity = this.getById(catelogId);
        if(entity.getParentCid() != 0){
            findParentPath(entity.getParentCid(), paths);
        }
        return paths;
    }
}