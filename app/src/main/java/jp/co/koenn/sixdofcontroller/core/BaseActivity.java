package jp.co.koenn.sixdofcontroller.core;

import android.support.v7.app.AppCompatActivity;

/**
 * Created by tsuji on 2016/07/11.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.getSimpleName();
    private final BaseActivity self = this;

    public abstract void replaceFragment(BaseFragment fragment, boolean recordBackstack);

    public abstract void popFragment();

    public abstract void clearFragment();

    public void onFragmentResumed(BaseFragment fragment) {
    }
}