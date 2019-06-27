package com.zzg.mybatis.generator.plugins;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

import java.util.List;

public class FixCountPlugin extends PluginAdapter {
    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean sqlMapCountByExampleElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        //将原有第二句更改为 如果列名不为空，则使用列名   否则为 *
        TextElement e = (TextElement) element.getElements().get(0);
        element.getElements().remove(0);
        String ori = e.getContent();
        ori = ori.replace("*", "1");
        element.addElement(0, new TextElement(ori));
        return super.sqlMapCountByExampleElementGenerated(element, introspectedTable);
    }
}
