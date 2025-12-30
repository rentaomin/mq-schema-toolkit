<?xml version="1.0" encoding="UTF-8"?>
<beans:beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="${namespace}">
  <${converterTag} id="${converterId}" codeGen="true">
    <message forType="${messageType}">
<#list fields as field>
      <@renderField field=field indent="      " />
</#list>
    </message>
  </${converterTag}>
</beans:beans>

<#macro renderField field indent>
<#if field.children?size == 0>
${indent}<field<#if field.name??> name="${field.name}"</#if> type="${field.type}"<#if field.forType??> forType="${field.forType}"</#if><#list field.attributes?keys as key> ${key}="${field.attributes[key]}"</#list> />
<#else>
${indent}<field<#if field.name??> name="${field.name}"</#if> type="${field.type}"<#if field.forType??> forType="${field.forType}"</#if><#list field.attributes?keys as key> ${key}="${field.attributes[key]}"</#list>>
<#list field.children as child>
${indent}  <@renderField field=child indent="${indent}  " />
</#list>
${indent}</field>
</#if>
</#macro>
