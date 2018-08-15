package com.fta.menlo.app;

import com.fta.menlo.ChatActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ChatActivityModule {
    @ContributesAndroidInjector(modules = {FragmentBuildersModule.class})
    public abstract ChatActivity contributesChatActivity();
}
