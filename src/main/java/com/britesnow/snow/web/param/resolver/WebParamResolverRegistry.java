package com.britesnow.snow.web.param.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.resolver.annotation.WebParamResolver;
import com.google.inject.Inject;

@Singleton
public class WebParamResolverRegistry {

    private Map<Class, WebParamResolverRef> refByReturnType = new HashMap<Class, WebParamResolverRef>();
    // for now, just support one annotation (ignore the WebParam)
    private Map<Class, WebParamResolverRef> refByAnnotation = new HashMap<Class, WebParamResolverRef>();

    @Inject
    private SystemWebParamResolvers systemWebParamResolvers;
    
    
    /**
     * Must be called before calling registerResolvers. 
     * Must be called at init time, no thread safe
     */
    public void init(){
        registerResolvers(systemWebParamResolvers);
    }
    
    
    public void registerResolvers(Object resolversObject) {

        Class cls = resolversObject.getClass();

        for (Method method : cls.getMethods()) {
            WebParamResolver webParamResolver = method.getAnnotation(WebParamResolver.class);

            if (webParamResolver != null) {
                WebParamResolverRef ref = new WebParamResolverRef(webParamResolver, resolversObject, method);
                Class returnType = ref.getReturnType();
                Class[] annotatedWith = ref.getAnnotatedWith();

                if (annotatedWith.length == 0) {
                    refByReturnType.put(returnType, ref);
                }else{
                    // for now, support only one annotation
                    // TODO: need to add support for multi annotation
                    refByAnnotation.put(annotatedWith[0],ref);
                }
            }

        }

    }
    
    /**
     * This will return the WebParamResolverRef for webHandlerMethod param at the index paramIdx
     * 
     * @param webHandlerMethod
     * @param paramIdx
     * @return
     */
    public WebParamResolverRef getWebParamResolverRef(Method webHandlerMethod, int paramIdx) {
        WebParamResolverRef ref = null;

        
        Class paramType = webHandlerMethod.getParameterTypes()[paramIdx];

        Annotation[] paramAnnotations = webHandlerMethod.getParameterAnnotations()[paramIdx];
        Annotation paramAnnotation = getFirstAnnotationButWebParam(paramAnnotations);
        
        // if we have an annotation, it takes precedence
        
        // first try to get the annotation 
        // TODO: need to support multiple annotations
        if (paramAnnotation != null){
            ref = refByAnnotation.get(paramAnnotation.annotationType());
        }

        // TODO: probably need to mature this logic to make sure the system is the most predictable. 
        
        // if not found, then, got with the paramType
        if (ref == null){
            ref = refByReturnType.get(paramType);
        }
        
        // if still null, then, check the parent classes
        if (ref == null){
            Class parentClass = paramType.getSuperclass();
            while (ref == null && parentClass != Object.class){
                ref = refByReturnType.get(parentClass);
            }
        }
        
        // if still null, then, check with the interfaces
        if (ref == null){
            for (Class interfaceClass : paramType.getInterfaces()){
                ref = refByReturnType.get(interfaceClass);
                if (ref != null){
                    break;
                }
            }
        }
        
        return ref;
    }
    
    
    private static Annotation getFirstAnnotationButWebParam(Annotation[] paramAnnotations){
        for (Annotation a : paramAnnotations){
            if (a.annotationType() != WebParam.class){
                return a;
            }
        }
        return null;
    }
    
    
}