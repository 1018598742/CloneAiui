package com.fta.menlo.app;

import com.fta.menlo.fragment.ChatFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract ChatFragment contributesChatFragment();

}
