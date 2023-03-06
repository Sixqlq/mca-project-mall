package com.msb.mall.product.web;

import com.msb.mall.product.entity.CategoryEntity;
import com.msb.mall.product.service.CategoryService;
import com.msb.mall.product.vo.Catalog2VO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 访问商城首页
 */
@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 修改页面跳转信息，查询一级分类数据
     * @param model
     * @return
     */
    @GetMapping({"/", "/index.html", "/home", "/home.html"})
    public String index(Model model){
        // 查询出所有的一级分类的信息
        List<CategoryEntity> list = categoryService.getLevel1Category();
        model.addAttribute("category", list);
        // thymeleaf.prefix: classPath:/templates/
        // thymeleaf.suffix: .html
        return "index";
    }

    // index/catalog.json

    /**
     * 查询二级和三级分类数据
     * @return
     */
    @ResponseBody
    @RequestMapping("/index/catalog.json")
    public Map<String, List<Catalog2VO>> getCatalog2JSON(){
        Map<String, List<Catalog2VO>> map = categoryService.getCatalog2Json();
        return map;
    }

    /**
     * 测试商城首页
     * @return
     */
    @ResponseBody
    @RequestMapping("/hello")
    public String hello(){
        return "hello";
    }

}
