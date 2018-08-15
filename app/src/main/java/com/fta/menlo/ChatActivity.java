package com.fta.menlo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import com.fta.menlo.fragment.ChatFragment;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;

public class ChatActivity extends AppCompatActivity implements HasSupportFragmentInjector {

    @Inject
    DispatchingAndroidInjector<Fragment> dispatchingAndroidInjector;

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return dispatchingAndroidInjector;
    }

    //矢量图兼容支持
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private ChatFragment mChatFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(ChatActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        onCreateFinish();
    }

    protected void onCreateFinish() {
        mChatFragment = new ChatFragment();
        //切换到ChatFragment聊天交互界面
        switchChats();
    }

    /**
     * 切换到聊天交互页面
     */
    public void switchChats() {
        switchFragment(mChatFragment, false);
    }


    protected void switchFragment(Fragment fragment, boolean backStack) {

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (backStack) {
            fragmentTransaction.addToBackStack(null);
        }
        //设置fragment切换的滑动动画
        if (fragment == mChatFragment) {
            fragmentTransaction.setCustomAnimations(
                    R.anim.slide_right_in, R.anim.slide_left_out,
                    R.anim.slide_left_in, R.anim.slide_right_out);
        } else {
            fragmentTransaction.setCustomAnimations(
                    R.anim.slide_left_in, R.anim.slide_right_out,
                    R.anim.slide_right_in, R.anim.slide_left_out);
        }

        fragmentTransaction.replace(R.id.container, fragment).commitAllowingStateLoss();
    }
}
