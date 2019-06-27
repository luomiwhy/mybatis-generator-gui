package com.zzg.mybatis.generator.plugins;

/**
 * Created by zouzhigang on 2016/6/14.
 */

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.internal.util.StringUtility;

import java.util.List;

public class MySQLLimitPlugin extends PluginAdapter {
    /**
     * 分页开始页码
     */
    public final static String PRO_START_PAGE = "startPage";
    private final static int DEFAULT_START_PAGE = 0;
    private int startPage = DEFAULT_START_PAGE;

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        super.initialized(introspectedTable);

        // 获取配置
        String startPage = this.getProperties().getProperty(PRO_START_PAGE);
        if (StringUtility.stringHasValue(startPage)) {
            this.startPage = Integer.valueOf(startPage);
        }
    }

    /**
     * 为每个Example类添加limit和offset属性已经set、get方法
     */
    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {

        PrimitiveTypeWrapper integerWrapper = FullyQualifiedJavaType.getIntInstance().getPrimitiveTypeWrapper();
        PrimitiveTypeWrapper longWrapper = new FullyQualifiedJavaType("long").getPrimitiveTypeWrapper();

        Field limit = new Field();
        limit.setName("limit");
        limit.setVisibility(JavaVisibility.PRIVATE);
        limit.setType(integerWrapper);
        topLevelClass.addField(limit);

        Method setLimit = new Method();
        setLimit.setVisibility(JavaVisibility.PUBLIC);
        setLimit.setName("setLimit");
        setLimit.addParameter(new Parameter(integerWrapper, "limit"));
        setLimit.addBodyLine("this.limit = limit;");
        topLevelClass.addMethod(setLimit);

        Method getLimit = new Method();
        getLimit.setVisibility(JavaVisibility.PUBLIC);
        getLimit.setReturnType(integerWrapper);
        getLimit.setName("getLimit");
        getLimit.addBodyLine("return limit;");
        topLevelClass.addMethod(getLimit);

        Field offset = new Field();
        offset.setName("offset");
        offset.setVisibility(JavaVisibility.PRIVATE);
        offset.setType(longWrapper);
        topLevelClass.addField(offset);

        Method setOffset = new Method();
        setOffset.setVisibility(JavaVisibility.PUBLIC);
        setOffset.setName("setOffset");
        setOffset.addParameter(new Parameter(longWrapper, "offset"));
        setOffset.addBodyLine("this.offset = offset;");
        topLevelClass.addMethod(setOffset);

        Method getOffset = new Method();
        getOffset.setVisibility(JavaVisibility.PUBLIC);
        getOffset.setReturnType(longWrapper);
        getOffset.setName("getOffset");
        getOffset.addBodyLine("return offset;");
        topLevelClass.addMethod(getOffset);

        Method limitMethod1 = new Method("limit");
        limitMethod1.setVisibility(JavaVisibility.PUBLIC);
        limitMethod1.setReturnType(topLevelClass.getType());
        limitMethod1.addParameter(new Parameter(integerWrapper, "limit"));
        limitMethod1.addBodyLine("this.limit = limit;");
        limitMethod1.addBodyLine("return this;");
        topLevelClass.addMethod(limitMethod1);

        Method limitMethod2 = new Method("limit");
        limitMethod2.setVisibility(JavaVisibility.PUBLIC);
        limitMethod2.setReturnType(topLevelClass.getType());
        limitMethod2.addParameter(new Parameter(longWrapper, "offset"));
        limitMethod2.addParameter(new Parameter(integerWrapper, "limit"));
        limitMethod2.addBodyLine("this.offset = offset;");
        limitMethod2.addBodyLine("this.limit = limit;");
        limitMethod2.addBodyLine("return this;");
        topLevelClass.addMethod(limitMethod2);

        Method pageMethod = new Method("page");
        pageMethod.setVisibility(JavaVisibility.PUBLIC);
        pageMethod.setReturnType(topLevelClass.getType());
        pageMethod.addParameter(new Parameter(integerWrapper, "page"));
        pageMethod.addParameter(new Parameter(integerWrapper, "pageSize"));
        pageMethod.addBodyLine("this.offset = (long) (" + (this.startPage == 0 ? "page" : "(page - " + this.startPage + ")") + " * pageSize);");
        pageMethod.addBodyLine("this.limit = pageSize;");
        pageMethod.addBodyLine("return this;");
        topLevelClass.addMethod(pageMethod);

        // !!! clear 方法增加 offset 和 limit 的清理
        List<Method> methodList = topLevelClass.getMethods();
        for (Method method : methodList) {
            if (method.getName().equals("clear")) {
                method.addBodyLine("limit = null;");
                method.addBodyLine("offset = null;");
            }
        }

        return true;
    }

    /**
     * 为Mapper.xml的selectByExample添加limit
     */
    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element,
                                                                     IntrospectedTable introspectedTable) {

        XmlElement ifLimitNotNullElement = new XmlElement("if");
        ifLimitNotNullElement.addAttribute(new Attribute("test", "limit != null"));

        XmlElement ifOffsetNotNullElement = new XmlElement("if");
        ifOffsetNotNullElement.addAttribute(new Attribute("test", "offset != null"));
        ifOffsetNotNullElement.addElement(new TextElement("limit ${offset}, ${limit}"));
        ifLimitNotNullElement.addElement(ifOffsetNotNullElement);

        XmlElement ifOffsetNullElement = new XmlElement("if");
        ifOffsetNullElement.addAttribute(new Attribute("test", "offset == null"));
        ifOffsetNullElement.addElement(new TextElement("limit ${limit}"));
        ifLimitNotNullElement.addElement(ifOffsetNullElement);

        element.addElement(ifLimitNotNullElement);

        return true;
    }

    /**
     * 为Mapper.xml的selectByExampleWithBLOBs添加limit
     */
    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        XmlElement ifLimitNotNullElement = new XmlElement("if");
        ifLimitNotNullElement.addAttribute(new Attribute("test", "limit != null"));

        XmlElement ifOffsetNotNullElement = new XmlElement("if");
        ifOffsetNotNullElement.addAttribute(new Attribute("test", "offset != null"));
        ifOffsetNotNullElement.addElement(new TextElement("limit ${offset}, ${limit}"));
        ifLimitNotNullElement.addElement(ifOffsetNotNullElement);

        XmlElement ifOffsetNullElement = new XmlElement("if");
        ifOffsetNullElement.addAttribute(new Attribute("test", "offset == null"));
        ifOffsetNullElement.addElement(new TextElement("limit ${limit}"));
        ifLimitNotNullElement.addElement(ifOffsetNullElement);

        element.addElement(ifLimitNotNullElement);

        return true;
    }
}
