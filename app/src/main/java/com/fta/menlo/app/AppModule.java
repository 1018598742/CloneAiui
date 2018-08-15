package com.fta.menlo.app;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.content.Context;

import com.fta.menlo.db.MessageDB;
import com.fta.menlo.db.MessageDao;
import com.fta.menlo.repository.AIUIRepository;
import com.fta.menlo.repository.SettingsRepo;
import com.fta.menlo.repository.Storage;
import com.fta.menlo.repository.TtsRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = ViewModelModule.class)
public class AppModule {

    @Provides
    @Singleton
    public Context providersContext(Application application) {
        return application;
    }

    @Named("AIUI cfg")
    @Provides
    @Singleton
    public JSONObject provideAIUICfg(Storage storage) {
        try {
            JSONObject config = new JSONObject(storage.readAssetFile("cfg/aiui_phone.cfg"));
            return config;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Named("msc cfg")
    @Provides
    @Singleton
    public String provideMSCCfg(Storage storage) {
        return storage.readAssetFile("cfg/msc.cfg");
    }

    @Provides
    @Singleton
    public AIUIRepository provideCenterRepository(Application application
            , @Named("AIUI cfg") JSONObject config, @Named("msc cfg") String mscCfg, MessageDao dao, SettingsRepo settings) {
        SpeechCreateIfExists(application, config);
        return new AIUIRepository(application, config, mscCfg, dao, settings);
    }

    private void SpeechCreateIfExists(Application application, JSONObject config) {
        try {
            //反射
            Class UtilityClass = getClass().getClassLoader().loadClass("com.iflytek.cloud.SpeechUtility");
            Method createMethod = UtilityClass.getDeclaredMethod("createUtility", Context.class, String.class);
            createMethod.invoke(null, application, "appid="
                    + config.optJSONObject("login").optString("appid") + ",engine_start=ivw");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    @Provides
    @Singleton
    public MessageDB provideMessageDB(Application application) {
        return Room.inMemoryDatabaseBuilder(application, MessageDB.class).build();
    }

    @Provides
    @Singleton
    public MessageDao provideMessageDao(MessageDB db) {
        return db.messageDao();
    }
}
