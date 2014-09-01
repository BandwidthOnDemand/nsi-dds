package net.es.nsi.pce.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class SpringContext {

    private static SpringContext instance;

    public static SpringContext getInstance() {
        if (instance == null) {
            instance = new SpringContext();
        }
        return instance;
    }

    private SpringContext() {
    }
    
    private ApplicationContext context;

    public ApplicationContext getContext() {
        return context;
    }

    public ApplicationContext initContext(String filename) {
        context = new FileSystemXmlApplicationContext(filename);
        return context;
    }
    
    public Object getBean(String beanId) {
        return context.getBean(beanId);
    }
    
    public boolean containsBean(String beanId) {
        return context.containsBean(beanId);
    }
    
    public <T> T getBean(String beanId, Class<T> requiredType) {
        return context.getBean(beanId, requiredType);
    }
}
