package com.fta.menlo.app;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.fta.menlo.chat.ChatViewModel;
import com.fta.menlo.common.ViewModelFactory;
import com.fta.menlo.repository.SettingsRepo;
import com.fta.menlo.ui.settings.SettingViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel.class)
    abstract ViewModel buildChatViewModel(ChatViewModel messagesViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SettingViewModel.class)
    abstract ViewModel buildSettingsViewModel(SettingViewModel settingViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory factory);
}
