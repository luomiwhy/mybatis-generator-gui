package com.zzg.mybatis.generator.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.*;
import org.mybatis.generator.internal.util.JavaBeansUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;


public class LogicalDeletePlugin extends PluginAdapter {
    protected static final Logger logger = LoggerFactory.getLogger(LogicalDeletePlugin.class);

    public static final String PRO_LOGICAL_DELETE_COLUMN = "logicalDeleteColumn";
    public static final String PRO_LOGICAL_DELETE_VALUE = "logicalDeleteValue";
    public static final String PRO_LOGICAL_NOT_DELETE_VALUE = "logicalNotDeleteValue";
    private IntrospectedColumn logicalDeleteColumn;
    private String logicalDeleteValue;
    private String logicalNotDeleteValue;

    public static final String PRO_LOGICAL_DELETE_ENUM_CLASS_NAME="logicalDeleteEnumClassName";
    public String logicalDeleteEnumClassName;

    /**
     * 逻辑删除查询方法
     */
    public static final String METHOD_LOGICAL_DELETED = "andSetLogicalDeleted";
    /**
     * 增强selectByPrimaryKey是参数名称
     */
    public static final String PARAMETER_LOGICAL_DELETED = METHOD_LOGICAL_DELETED;
    /**
     * selectByPrimaryKey 的逻辑删除增强
     */
    public static final String METHOD_SELECT_BY_PRIMARY_KEY_WITH_LOGICAL_DELETE = "selectByPrimaryKeyWithLogicalDelete";

    public static final String METHOD_LOGICAL_DELETE_BY_EXAMPLE = "logicalDeleteByExample";
    public static final String METHOD_LOGICAL_DELETE_BY_PRIMARY_KEY = "logicalDeleteByPrimaryKey";


    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        super.initialized(introspectedTable);

        // 1. 获取配置的逻辑删除列
        Properties properties = getProperties();
        String logicalDeleteColumn = properties.getProperty(PRO_LOGICAL_DELETE_COLUMN);
        this.logicalDeleteColumn = introspectedTable.getColumn(logicalDeleteColumn);
        // 判断如果表单独配置了逻辑删除列，但是却没有找到对应列进行提示
        if (introspectedTable.getTableConfigurationProperty(PRO_LOGICAL_DELETE_COLUMN) != null && this.logicalDeleteColumn == null) {
            logger.error("(逻辑删除插件):" + introspectedTable.getFullyQualifiedTable() + "没有找到您配置的逻辑删除列(" + introspectedTable.getTableConfigurationProperty(PRO_LOGICAL_DELETE_COLUMN) + ")！");
        }

        // 3.判断逻辑删除值是否配置了
        this.logicalDeleteValue = properties.getProperty(PRO_LOGICAL_DELETE_VALUE);
        this.logicalNotDeleteValue = properties.getProperty(PRO_LOGICAL_NOT_DELETE_VALUE);
        if (this.logicalDeleteValue == null || this.logicalNotDeleteValue == null) {
            this.logicalDeleteColumn = null;
            logger.error("(逻辑删除插件):" + introspectedTable.getFullyQualifiedTable() + "没有找到您配置的逻辑删除值，请全局配置logicalDeleteValue和logicalUnDeleteValue值！");
        }

        //4. 通常情况下，逻辑删除枚举和列定义是固定的，所以删除原有代码。替换为固定枚举类
        this.logicalDeleteEnumClassName = properties.getProperty(PRO_LOGICAL_DELETE_ENUM_CLASS_NAME);
        if (this.logicalDeleteEnumClassName == null) {
            logger.error("(逻辑删除插件):没有找到您配置的逻辑删除值对应的枚举类(0位置放未删除，1位置放已删除)");
        }

        // 5. 防止增强的selectByPrimaryKey中逻辑删除键冲突
        if (this.logicalDeleteColumn != null) {
            Field logicalDeleteField = JavaBeansUtil.getJavaBeansField(this.logicalDeleteColumn, context, introspectedTable);
            if (logicalDeleteField.getName().equals(PARAMETER_LOGICAL_DELETED)) {
                this.logicalDeleteColumn = null;
                logger.error("(逻辑删除插件):" + introspectedTable.getFullyQualifiedTable() + "配置的逻辑删除列和插件保留关键字(" + PARAMETER_LOGICAL_DELETED + ")冲突！");
            }
        }
    }

    /**
     * 逻辑删除ByExample
     */
    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        Method limitMethod1 = new Method(METHOD_LOGICAL_DELETE_BY_EXAMPLE);
        limitMethod1.setVisibility(JavaVisibility.DEFAULT);
        limitMethod1.setReturnType(FullyQualifiedJavaType.getIntInstance());
        limitMethod1.addParameter(new Parameter(new FullyQualifiedJavaType(introspectedTable.getExampleType()), "example"));
        interfaze.addMethod(limitMethod1);
        logger.debug("(逻辑删除插件):" + interfaze.getType().getShortName() + "增加方法logicalDeleteByExample。");
        return super.clientDeleteByExampleMethodGenerated(method, interfaze, introspectedTable);
    }

    /**
     * 逻辑删除ByExample
     */
    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        if (introspectedTable.hasPrimaryKeyColumns()) {
            Method mLogicalDeleteByPrimaryKey = new Method(METHOD_LOGICAL_DELETE_BY_PRIMARY_KEY);
            mLogicalDeleteByPrimaryKey.setVisibility(JavaVisibility.DEFAULT);
            mLogicalDeleteByPrimaryKey.setReturnType(FullyQualifiedJavaType.getIntInstance());

            if (introspectedTable.getRules().generatePrimaryKeyClass()) {
                // 暂不处理有key生产类的
//                FullyQualifiedJavaType type1 = new FullyQualifiedJavaType(introspectedTable.getPrimaryKeyType());
//                mLogicalDeleteByPrimaryKey.addParameter(new Parameter(type1, "key"));
            } else {
                // no primary key class - fields are in the base class
                // if more than one PK field, then we need to annotate the
                // parameters
                // for MyBatis
                List<IntrospectedColumn> introspectedColumns = introspectedTable.getPrimaryKeyColumns();
                boolean annotate = introspectedColumns.size() > 1;
                StringBuilder sb = new StringBuilder();
                for (IntrospectedColumn introspectedColumn : introspectedColumns) {
                    FullyQualifiedJavaType type1 = introspectedColumn.getFullyQualifiedJavaType();
                    Parameter parameter = new Parameter(type1, introspectedColumn.getJavaProperty());
                    if (annotate) {
                        sb.setLength(0);
                        sb.append("@Param(\"");
                        sb.append(introspectedColumn.getJavaProperty());
                        sb.append("\")");
                        parameter.addAnnotation(sb.toString());
                    }
                    mLogicalDeleteByPrimaryKey.addParameter(parameter);
                }
            }
            interfaze.addMethod(mLogicalDeleteByPrimaryKey);
            logger.debug("(逻辑删除插件):" + interfaze.getType().getShortName() + "增加方法logicalDeleteByPrimaryKey。");
        }
        return super.clientDeleteByPrimaryKeyMethodGenerated(method, interfaze, introspectedTable);
    }

    /**
     * 增强为 selectByPrimaryKeyWithLogicalDelete
     */
    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        Method selectLogicDeleteMethod = new Method(METHOD_SELECT_BY_PRIMARY_KEY_WITH_LOGICAL_DELETE);
        selectLogicDeleteMethod.setVisibility(JavaVisibility.DEFAULT);
        selectLogicDeleteMethod.setReturnType(FullyQualifiedJavaType.getIntInstance());
        selectLogicDeleteMethod.addParameter(new Parameter(new FullyQualifiedJavaType(introspectedTable.getExampleType()), "example", "@Param(\"example\")"));
        selectLogicDeleteMethod.addParameter(new Parameter(FullyQualifiedJavaType.getBooleanPrimitiveInstance(), PARAMETER_LOGICAL_DELETED, "@Param(\""+PARAMETER_LOGICAL_DELETED+"\")"));
        interfaze.addMethod(selectLogicDeleteMethod);
        logger.debug("(逻辑删除插件):" + interfaze.getType().getShortName() + "增加方法logicalDeleteByExample。");
        return super.clientSelectByExampleWithoutBLOBsMethodGenerated(method, interfaze, introspectedTable);
    }

    /**
     * 修改 selectByExample 只查未逻辑删除的记录
     */
    @Override
    public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        int insertIndex = 5;
        for (int i = 0; i < element.getElements().size(); i++) {
            Element e = element.getElements().get(i);
            if (e instanceof XmlElement) {
                XmlElement xmlElement = (XmlElement) e;
                if (xmlElement.getAttributes().get(0).getValue().startsWith("orderByClause")) {
                    insertIndex = i;
                    break;
                }
            }
        }
        TextElement e = new TextElement(" and "+this.logicalDeleteColumn.getActualColumnName()+"="+this.logicalNotDeleteValue+" ");
        element.addElement(insertIndex, e);
        return super.sqlMapSelectByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
    }

    /**
     * 修改 selectByPrimaryKey 只查未逻辑删除的记录
     */
    @Override
    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        TextElement e = new TextElement(" and "+this.logicalDeleteColumn.getActualColumnName()+"="+this.logicalNotDeleteValue+" ");
        element.addElement(e);
        return super.sqlMapSelectByPrimaryKeyElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        // 1. 逻辑删除ByExample
        XmlElement logicalDeleteByExample = new XmlElement("update");
        logicalDeleteByExample.addAttribute(new Attribute("id", METHOD_LOGICAL_DELETE_BY_EXAMPLE));
        logicalDeleteByExample.addAttribute(new Attribute("parameterType", introspectedTable.getExampleType()));

        StringBuilder updatePrefix = new StringBuilder();
        updatePrefix.append("update ");
        updatePrefix.append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime());
        updatePrefix.append(" set ");
        // 更新逻辑删除字段
        updatePrefix.append(this.logicalDeleteColumn.getActualColumnName());
        updatePrefix.append(" = ");
        updatePrefix.append(this.logicalDeleteValue);
        logicalDeleteByExample.addElement(new TextElement(updatePrefix.toString()));
        logicalDeleteByExample.addElement(CommonUtils.getExampleIncludeElement(introspectedTable));

        document.getRootElement().addElement(logicalDeleteByExample);
        logger.debug("itfsw(逻辑删除插件):" + introspectedTable.getMyBatis3XmlMapperFileName() + "增加方法logicalDeleteByExample的实现。");


        // 2. 判断是否有主键，生成主键删除方法
        if (introspectedTable.hasPrimaryKeyColumns()) {
            XmlElement logicalDeleteByPrimaryKey = new XmlElement("update");
            logicalDeleteByPrimaryKey.addAttribute(new Attribute("id", METHOD_LOGICAL_DELETE_BY_PRIMARY_KEY));

            String parameterClass;
            if (introspectedTable.getRules().generatePrimaryKeyClass()) {
                // 暂不处理有key生产类的
                parameterClass = introspectedTable.getPrimaryKeyType();
            } else {
                // PK fields are in the base class. If more than on PK
                // field, then they are coming in a map.
                if (introspectedTable.getPrimaryKeyColumns().size() > 1) {
                    parameterClass = "map";
                } else {
                    parameterClass = introspectedTable.getPrimaryKeyColumns().get(0).getFullyQualifiedJavaType().toString();
                }
            }
            logicalDeleteByPrimaryKey.addAttribute(new Attribute("parameterType", parameterClass));
            logicalDeleteByPrimaryKey.addElement(new TextElement(updatePrefix.toString()));
            CommonUtils.generateWhereByPrimaryKeyTo(logicalDeleteByPrimaryKey, introspectedTable.getPrimaryKeyColumns());
            document.getRootElement().addElement(logicalDeleteByPrimaryKey);
            logger.debug("(逻辑删除插件):" + introspectedTable.getMyBatis3XmlMapperFileName() + "增加方法logicalDeleteByPrimaryKey的实现。");
        }

        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }
}
