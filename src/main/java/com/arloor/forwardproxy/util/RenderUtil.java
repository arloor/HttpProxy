package com.arloor.forwardproxy.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

public class RenderUtil {
    private final static TemplateEngine textEngine = new TemplateEngine();
    private final static TemplateEngine htmlEngine = new TemplateEngine();

    static {
        StringTemplateResolver textResolver = new StringTemplateResolver();
        textResolver.setOrder(1);
        textResolver.setTemplateMode(TemplateMode.TEXT);
        // TODO Cacheable or Not ?
        textResolver.setCacheable(true);
        textEngine.setTemplateResolver(textResolver);

        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setOrder(1);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        // TODO Cacheable or Not ?
        templateResolver.setCacheable(true);
        htmlEngine.setTemplateResolver(templateResolver);
    }

    /**
     * 使用 Thymeleaf 渲染 Text模版
     * Text模版语法见：https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#textual-syntax
     *
     * @param template 模版
     * @param params   参数
     * @return 渲染后的Text
     */
    public static String text(String template, Map<String, Object> params) {

        Context context = new Context();
        context.setVariables(params);
        return textEngine.process(template, context);
    }

    /**
     * 使用 Thymeleaf 渲染 Html模版
     *
     * @param template Html模版
     * @param params   参数
     * @return 渲染后的html
     */
    public static String html(String template, Map<String, Object> params) {
        Context context = new Context();
        context.setVariables(params);
        return htmlEngine.process(template, context);
    }

    /**
     * 测试用，展示如何使用
     *
     * @param args
     */
    public static void main(String[] args) {
        // 渲染String
        String string_template = "这是[(${name.toString()})]"; // 直接name其实就行了，这里就是展示能调用java对象的方法
        String value = RenderUtil.text(string_template, ImmutableMap.of("name", "ARLOOR"));
        System.out.println(value);

        // 渲染List
        /**
         * [# th:each="item : ${items}"]
         *   - [(${item})]
         * [/]
         */
        String list_template = "[# th:each=\"item : ${items}\"]\n" +
                "  - [(${item})]\n" +
                "[/]";
        String value1 = RenderUtil.text(list_template, ImmutableMap.of("items", Lists.newArrayList("第一个", "第二个")));
        System.out.println(value1);

        // 渲染Map
        /**
         * [# th:each="key : ${map.keySet()}"]
         *   - [(${map.get(key)})]
         * [/]
         */
        String map_template = "[# th:each=\"key : ${map.keySet()}\"]\n" +
                " 这是 - [(${map.get(key)})]\n" +
                "[/]";
        String value2 = RenderUtil.text(map_template, ImmutableMap.of("map", ImmutableMap.of("a", "甲", "b", "乙")));
        System.out.println(value2);

        String html_template = "这是<span th:text=\"${name}\"></span>";
        System.out.println(RenderUtil.html(html_template, ImmutableMap.of("name", "ARLOOR")));

    }
}