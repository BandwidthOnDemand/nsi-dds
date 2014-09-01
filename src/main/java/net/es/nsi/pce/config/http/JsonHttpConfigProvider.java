package net.es.nsi.pce.config.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.es.nsi.pce.config.FileBasedConfigProvider;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import net.es.nsi.pce.spring.SpringContext;

public class JsonHttpConfigProvider  extends FileBasedConfigProvider implements HttpConfigProvider {


    private HashMap<String, HttpConfig> configs = new HashMap<String, HttpConfig>();

    public static JsonHttpConfigProvider getInstance() {
        SpringContext sc = SpringContext.getInstance();
        JsonHttpConfigProvider provider = (JsonHttpConfigProvider) sc.getContext().getBean("httpConfigProvider");
        return provider;
    }

    @Override
    public void loadConfig() throws Exception {

        File configFile = new File(this.getFilename());
        String json = FileUtils.readFileToString(configFile);
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, HttpConfig>>() {}.getType();

        configs = gson.fromJson(json, type);
    }

    @Override
    public HttpConfig getConfig(String id) {
        return configs.get(id);
    }

}
