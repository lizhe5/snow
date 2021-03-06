package com.britesnow.snow.web.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.britesnow.snow.web.HttpMethod;
import com.britesnow.snow.web.binding.WebClasses;
import com.britesnow.snow.web.exception.WebExceptionCatcherRef;
import com.britesnow.snow.web.exception.annotation.WebExceptionCatcher;
import com.britesnow.snow.web.handler.annotation.FreemarkerMethodHandler;
import com.britesnow.snow.web.handler.annotation.WebActionHandler;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.handler.annotation.WebResourceHandler;
import com.britesnow.snow.web.handler.annotation.FreemarkerDirectiveHandler;
import com.britesnow.snow.web.hook.HookRegistry;
import com.britesnow.snow.web.hook.annotation.WebApplicationHook;
import com.britesnow.snow.web.hook.annotation.WebRequestHook;
import com.britesnow.snow.web.param.resolver.WebParamResolverRegistry;
import com.britesnow.snow.web.renderer.freemarker.FreemarkerDirectiveProxy;
import com.britesnow.snow.web.renderer.freemarker.FreemarkerMethodProxy;
import com.britesnow.snow.web.rest.DefaultWebSerializers;
import com.britesnow.snow.web.rest.RestRegistry;
import com.britesnow.snow.web.rest.SerializerRegistry;
import com.britesnow.snow.web.rest.annotation.WebDelete;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.britesnow.snow.web.rest.annotation.WebPut;
import com.britesnow.snow.web.rest.annotation.WebSerializer;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class WebObjectRegistry {

    private String[]                                                leafPaths;

    private Map<String, WebModelHandlerRef>                         webModelHandlerByStartsWithMap = new HashMap<String, WebModelHandlerRef>();
    private List<WebModelHandlerRef>                                webModelHandlerRefList         = new ArrayList<WebModelHandlerRef>();
    private Map<String, WebActionHandlerRef>                        webActionHandlerDic            = new HashMap<String, WebActionHandlerRef>();
    private List<WebResourceHandlerRef>                             webResourceHandlerList         = new ArrayList<WebResourceHandlerRef>();
    private List<FreemarkerDirectiveProxy>                          freemarkerDirectiveProxyList   = new ArrayList<FreemarkerDirectiveProxy>();
    private List<FreemarkerMethodProxy>                             freemarkerMethodProxyList      = new ArrayList<FreemarkerMethodProxy>();
    private Map<Class<? extends Throwable>, WebExceptionCatcherRef> webExceptionCatcherMap         = new HashMap<Class<? extends Throwable>, WebExceptionCatcherRef>();

    @Inject
    private WebParamResolverRegistry                                webParamResolverRegistry;

    @Inject
    private HookRegistry                                            hookRegistry;
    
    @Inject
    private RestRegistry                                            restRegistry;
    
    @Inject
    private SerializerRegistry                                      serializerRegistry;

    @Inject
    private ParamDefBuilder                                         paramDefBuilder;

    @Inject(optional = true)
    @Nullable
    @WebClasses
    private Class[]                                                webClasses;
    
    @Inject
    private Injector injector;

    /**
     * Must be called before calling registerWebHandlers.<br />
     * Must be called before at application init time (not thread safe). <br />
     */
    public void init() {
        webParamResolverRegistry.init();

        WebObjectValidationExceptions exs = new WebObjectValidationExceptions();
        
        // register the default
        registerWebObjectMethods(DefaultWebSerializers.class);

        if (webClasses != null) {
            for (Class webClass : webClasses) {
                try {
                    validateWebObject(webClass);
                    registerWebObjectMethods(webClass);
                } catch (WebObjectValidationException ex) {
                    exs.addWebException(ex);
                }
            }
        }
        if (exs.hasExceptions()) {
            throw exs;
        }
    }

    /**
     * Validate that the
     * 
     * @param webObject
     * @since 2.0.0
     */
    private void validateWebObject(Class webClass) throws WebObjectValidationException {
        Class cls = webClass;

        Object an = cls.getAnnotation(Singleton.class);
        if (an == null) {
            an = cls.getAnnotation(javax.inject.Singleton.class);
        }
        if (an == null) {
            WebObjectValidationException ex = new WebObjectValidationException(cls, WebObjectValidationException.ERROR.NO_SINGLETON);
            throw ex;
        }
    }

    /**
     * Get the leafPaths (probably
     * 
     * @return
     */
    public String[] getLeafPaths() {
        return leafPaths;
    }

    public WebActionHandlerRef getWebActionHandlerRef(String actionName) {
        return webActionHandlerDic.get(actionName);
    }

    public List<WebModelHandlerRef> getWebModelHandlerRefList(String[] resourcePaths){
        List<WebModelHandlerRef> refs = new ArrayList<WebModelHandlerRef>();
        
        // add the root
        WebModelHandlerRef rootWmr = getWebModeHandlerlRef("/");
        if (rootWmr != null) {
            refs.add(rootWmr);
        }
        
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < resourcePaths.length; i++) {
            String path = pathBuilder.append('/').append(resourcePaths[i]).toString();
            WebModelHandlerRef webModelRef = getWebModeHandlerlRef(path);
            if (webModelRef != null){
                refs.add(webModelRef);
            }
        }        
        
        return refs;
    }
    
    public WebModelHandlerRef getWebModeHandlerlRef(String path) {
        return webModelHandlerByStartsWithMap.get(path);
    }

    public List<WebModelHandlerRef> getMatchWebModelHandlerRef(String path) {
        List<WebModelHandlerRef> matchWebModelRefs = new ArrayList<WebModelHandlerRef>();

        for (WebModelHandlerRef webModelRef : webModelHandlerRefList) {
            boolean match = webModelRef.matchesPath(path);
            if (match) {
                matchWebModelRefs.add(webModelRef);
            }
        }

        return matchWebModelRefs;
    }

    public WebResourceHandlerRef getWebResourceHandlerRef(String path) {
        for (WebResourceHandlerRef webFileRef : webResourceHandlerList) {
            boolean match = webFileRef.matchesPath(path);
            if (match) {
                return webFileRef;
            }
        }
        return null;
    }

    /**
     * Do not call. Internal to Snow.
     * 
     * (used by the FreemarkerTemplateRenderer)
     * 
     * @return
     */
    public List<FreemarkerDirectiveProxy> getFreemarkerDirectiveProxyList() {
        return freemarkerDirectiveProxyList;
    }

    /**
     * Do not call. Internal to Snow.
     * 
     * @param exceptionClass
     * @return
     */
    public List<FreemarkerMethodProxy> getFreemarkerMethodProxyList() {
        return freemarkerMethodProxyList;
    }

    public WebExceptionCatcherRef getWebExceptionCatcherRef(Class<? extends Throwable> exceptionClass) {
        WebExceptionCatcherRef ref = null;

        // if there is a direct match, return it.
        ref = webExceptionCatcherMap.get(exceptionClass);
        if (ref != null) {
            return ref;
        }

        Class cls = exceptionClass.getSuperclass();

        while (cls != Object.class) {
            ref = webExceptionCatcherMap.get(cls);
            if (ref != null) {
                return ref;
            }
            cls = cls.getSuperclass();
        }

        return null;
    }

    // --------- Private Registration Methods (call at init() time) --------- //
    private final void registerWebObjectMethods(Class webClass) {

        //Class c = getNonGuiceEnhancedClass(targetObject);
        Class c = webClass;
        
        Method methods[] = c.getMethods();
        List<String> additionalLeafPaths = new ArrayList<String>();

        for (Method m : methods) {
            
            // --------- Register Rest Methods --------- //
            WebGet webGet = m.getAnnotation(WebGet.class);
            if (webGet != null){
                restRegistry.registerWebRest(c, m, paramDefBuilder.buildParamDefs(m, true), HttpMethod.GET, webGet.value());
            }
            
            WebPost webPost = m.getAnnotation(WebPost.class);
            if (webPost != null){
                restRegistry.registerWebRest(c, m, paramDefBuilder.buildParamDefs(m, true), HttpMethod.POST, webPost.value());
            }
            
            WebPut webPut = m.getAnnotation(WebPut.class);
            if (webPut != null){
                restRegistry.registerWebRest(c, m, paramDefBuilder.buildParamDefs(m, true), HttpMethod.PUT, webPut.value());
            }
            
            WebDelete webDelete = m.getAnnotation(WebDelete.class);
            if (webDelete != null){
                restRegistry.registerWebRest(c, m, paramDefBuilder.buildParamDefs(m, true), HttpMethod.DELETE, webDelete.value());
            }            
            // --------- /Register Rest Methods --------- //
            
            // --------- Register Web Serializer --------- //
            WebSerializer webSerializer = m.getAnnotation(WebSerializer.class);
            if (webSerializer != null){
                serializerRegistry.registerWebSerializer(webClass, m, webSerializer);
            }
            // --------- /Register Web Serializer --------- //
            
            // --------- Register WebActionHandler --------- //
            WebActionHandler webActionHandlerAnnotation = m.getAnnotation(WebActionHandler.class);
            // if it is an action method, then, add the WebAction Object and
            // Method to the action Dic
            if (webActionHandlerAnnotation != null) {
                registerWebAction(c, m, webActionHandlerAnnotation);
            }
            // --------- /Register WebActionHandler --------- //

            // --------- Register WebModelHandler --------- //
            WebModelHandler webModelHandlerAnnotation = m.getAnnotation(WebModelHandler.class);
            if (webModelHandlerAnnotation != null) {
                registerWebModel(c, m, webModelHandlerAnnotation);
            }
            // --------- Register WebModelHandler --------- //

            // --------- Register WebResourceHandler --------- //
            WebResourceHandler webResourceHandlerAnnotation = m.getAnnotation(WebResourceHandler.class);
            if (webResourceHandlerAnnotation != null) {
                registerWebResourceHandler(c, m, webResourceHandlerAnnotation);
            }
            // --------- /Register WebResourceHandler --------- //

            // --------- Register WebRequestHook --------- //
            WebRequestHook webRequestHook = m.getAnnotation(WebRequestHook.class);
            if (webRequestHook != null) {
                hookRegistry.addReqHook(c, m, webRequestHook);
            }
            // --------- Register WebRequestHook --------- //

            // --------- Register WebRequestHook --------- //
            WebApplicationHook webApplicationHook = m.getAnnotation(WebApplicationHook.class);
            if (webApplicationHook != null) {
                hookRegistry.addAppHook(c, m, webApplicationHook);
            }
            // --------- Register WebRequestHook --------- //

            // --------- Freemarker handlers --------- //
            FreemarkerDirectiveHandler webTemplateDirective = m.getAnnotation(FreemarkerDirectiveHandler.class);
            if (webTemplateDirective != null) {
                registerFreemarkerDirective(c, m, webTemplateDirective);
            }

            FreemarkerMethodHandler freemarkerMethodHandler = m.getAnnotation(FreemarkerMethodHandler.class);
            if (freemarkerMethodHandler != null) {
                registerFreemarkerMethod(c, m, freemarkerMethodHandler);
            }
            // --------- /Freemarker handlers --------- //

            // --------- Register WebException --------- //
            WebExceptionCatcher webExceptionHandler = m.getAnnotation(WebExceptionCatcher.class);
            if (webExceptionHandler != null) {
                registerWebExceptionCatcher(c, m, webExceptionHandler);
            }
            // --------- /Register WebException --------- //

        }

        // if we have any declared leaf paths, add them into the array. they come after
        // any injected leaf path values.
        if (additionalLeafPaths.size() > 0) {
            if (leafPaths != null) {
                additionalLeafPaths.addAll(0, Arrays.asList(leafPaths));
            }

            leafPaths = additionalLeafPaths.toArray(new String[additionalLeafPaths.size()]);
        }
    }

    private final void registerWebModel(Class webClass, Method m, WebModelHandler webModel) {
        //WebParamResolverRef webParamResolverRefs[] = buildWebParamResolverRefs(m);
        WebModelHandlerRef webModelRef = new WebModelHandlerRef(webClass, m, paramDefBuilder.buildParamDefs(m,true), webModel);
        webModelHandlerRefList.add(webModelRef);

        String startWithArray[] = webModel.startsWith();
        for (String startsWith : startWithArray) {
            webModelHandlerByStartsWithMap.put(startsWith, webModelRef);
        }
    }

    private final void registerWebAction(Class webClass, Method m, WebActionHandler webAction) {

        String actionName = webAction.name();
        // if the action does have an empty name, then, take the name of the
        // method
        if (actionName.length() == 0) {
            actionName = m.getName();
        }
        // try to get the actionObjectList from the actionDic
        WebActionHandlerRef actionRef = webActionHandlerDic.get(actionName);
        // if the WebActionRef already exist, throw an exception
        if (actionRef != null) {
            // AlertHandler.systemSevere(Alert.ACTION_NAME_ALREADY_EXIST,
            // actionName);
            throw new RuntimeException("Action Name Already Exist: " + actionName);
        }
        // if not found, create an empty list
        // add this object and method to the list
        webActionHandlerDic.put(actionName, new WebActionHandlerRef(webClass, m, paramDefBuilder.buildParamDefs(m,true), webAction));
    }

    private final void registerWebResourceHandler(Class webClass, Method m, WebResourceHandler webResourceHandler) {
        WebResourceHandlerRef webFileRef = new WebResourceHandlerRef(webClass, m, paramDefBuilder.buildParamDefs(m,true), webResourceHandler);
        webResourceHandlerList.add(webFileRef);
    }

    private final void registerFreemarkerDirective(Class webClass, Method m,
                            FreemarkerDirectiveHandler freemarkerDirectiveHandler) {
        String templateMethodName = freemarkerDirectiveHandler.name();
        // if the action does have an empty name, then, take the name of the method
        if (templateMethodName.length() == 0) {
            templateMethodName = m.getName();
        }

        FreemakerDirectiveHandlerRef directiveRef = new FreemakerDirectiveHandlerRef(webClass, m, paramDefBuilder.buildParamDefs(m,true), freemarkerDirectiveHandler);
        FreemarkerDirectiveProxy directiveProxy = new FreemarkerDirectiveProxy(templateMethodName, directiveRef);
        injector.injectMembers(directiveProxy);
        freemarkerDirectiveProxyList.add(directiveProxy);
    }

    private final void registerFreemarkerMethod(Class webClass, Method m,
                            FreemarkerMethodHandler freemarkerMethodHandler) {
        String name = freemarkerMethodHandler.name();
        if (name.length() == 0) {
            name = m.getName();
        }
        FreemarkerMethodHandlerRef ref = new FreemarkerMethodHandlerRef(webClass, m, paramDefBuilder.buildParamDefs(m,true), freemarkerMethodHandler);
        FreemarkerMethodProxy proxy = new FreemarkerMethodProxy(name, ref);
        injector.injectMembers(proxy);
        freemarkerMethodProxyList.add(proxy);

    }

    private final void registerWebExceptionCatcher(Class cls, Method m, WebExceptionCatcher webExceptionHandler) {
        WebExceptionCatcherRef webExcpetionCatcherRef = new WebExceptionCatcherRef(cls, m, webExceptionHandler);
        webExceptionCatcherMap.put(webExcpetionCatcherRef.getThrowableClass(), webExcpetionCatcherRef);
    }

    // --------- /Private Registration Methods (call at init() time) --------- //

    public static Class getNonGuiceEnhancedClass(Object obj) {
        String className = obj.getClass().getName();
        if (className.indexOf("$$EnhancerByGuice$$") > -1) {
            return obj.getClass().getSuperclass();
        } else {
            return obj.getClass();
        }
    }

}
