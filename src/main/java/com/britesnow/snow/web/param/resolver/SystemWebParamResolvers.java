package com.britesnow.snow.web.param.resolver;

import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import com.britesnow.snow.util.AnnotationMap;
import com.britesnow.snow.util.ObjectUtil;
import com.britesnow.snow.web.HttpMethod;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.WebRequestType;
import com.britesnow.snow.web.param.annotation.PathVar;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebPath;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.param.resolver.annotation.WebParamResolver;
import com.google.common.base.Strings;

@Singleton
public class SystemWebParamResolvers {

    // --------- Primary Type @WebParam Resolvers --------- //
    //TODO: probably can reduce all the primary type resolver to one generic (as the resolveWebPath)
    @WebParamResolver(annotatedWith=WebParam.class)
    public Long resolveLong(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        String id = rc.getParam(webParam.value());
        return ObjectUtil.getValue(id, Long.class, null);
    }

    @WebParamResolver(annotatedWith=WebParam.class)
    public Integer resolveInteger(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        String id = rc.getParam(webParam.value());
        return ObjectUtil.getValue(id, Integer.class, null);
    }

    @WebParamResolver(annotatedWith=WebParam.class)
    public Double resolveDouble(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        String id = rc.getParam(webParam.value());
        return ObjectUtil.getValue(id, Double.class, null);
    }

    @WebParamResolver(annotatedWith=WebParam.class)
    public String resolveString(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        return rc.getParam(webParam.value());
    }
    
    @WebParamResolver(annotatedWith=WebParam.class)
    public Boolean resolveBoolean(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        String val = rc.getParam(webParam.value());
        return ObjectUtil.getValue(val,Boolean.class,null);
    }
    
    @WebParamResolver(annotatedWith=WebParam.class)
    public Enum resolveEnum(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        String val = rc.getParam(webParam.value());
        return (Enum)ObjectUtil.getValue(val, paramType, null);
    }
    
    @WebParamResolver(annotatedWith=WebParam.class)
    public Map resolveMap(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        return rc.getParamMap(webParam.value());
    }
    
    @WebParamResolver(annotatedWith=WebParam.class)
    public FileItem resolveFileItem(AnnotationMap annotationMap, Class paramType, RequestContext rc) {
        WebParam webParam = annotationMap.get(WebParam.class);
        return rc.getParamAs(webParam.value(), FileItem.class);
    }    
    // --------- /@WebParam Resolvers --------- //
    

    @WebParamResolver(annotatedWith=WebUser.class)
    public Object resolveUser(AnnotationMap annotationMap, Class paramType, RequestContext rc){
        if (rc.getAuthToken() != null) {
            // TODO: probly need to check that the paramType match the getUser
            return rc.getAuthToken().getUser();
        }else{
            return null;
        }
    }

    @WebParamResolver(annotatedWith=WebModel.class)
    public Map resolveWebModel(AnnotationMap annotationMap, Class paramType, RequestContext rc){
        return rc.getWebModel();
    }
    
    @WebParamResolver(annotatedWith=PathVar.class)
    public Object resolvePathVar(AnnotationMap annotationMap, Class paramType, RequestContext rc){
        PathVar pathVar = annotationMap.get(PathVar.class);
        String  varName = pathVar.value();
        if (!Strings.isNullOrEmpty(varName)){
            return rc.getPathVarAs(varName,paramType,null);
        }
        return null;
    }
    
    @WebParamResolver(annotatedWith=WebPath.class)
    public Object resolveWebpath(AnnotationMap annotationMap, Class paramType, RequestContext rc){
        WebPath webPath = annotationMap.get(WebPath.class);
        Object value;
        //if the index has been set, then, return the single Path and convert to the appropriate type.
        if (webPath.value() > -1) {
            value = rc.getResourcePathAt(webPath.value(), paramType, null);
        }
        //otherwise, return the full path
        else {
            value = rc.getResourcePath();
        }        
        return value;
    }
    
    @WebParamResolver
    public RequestContext resolveRequestContext(AnnotationMap annotationMap, Class paramType, RequestContext rc){
        return rc;
    }

    @WebParamResolver
    public HttpMethod resolveHttpMethod(AnnotationMap annotationMap, Class paramType, RequestContext rc){
        return rc.getMethod();
    }

    @WebParamResolver
    public WebRequestType resolveWebRequestType(AnnotationMap annotationMap, Class paramType, RequestContext rc){
        return rc.getWebRequestType();
    }
    // --------- Servlet Resolvers --------- //
    @WebParamResolver
    public HttpServletRequest resolveHttpServletRequest(AnnotationMap annotationMap, Class paramType,
                            RequestContext rc) {
        return rc.getReq();
    }

    @WebParamResolver
    public HttpServletResponse resolveHttpServletResponse(AnnotationMap annotationMap, Class paramType,
                            RequestContext rc) {
        return rc.getRes();
    }

    @WebParamResolver
    public ServletContext resolveServletContext(AnnotationMap annotationMap, Class paramType,
                            RequestContext rc) {
        return rc.getServletContext();
    }
    // --------- Servlet Resolvers --------- //

}
