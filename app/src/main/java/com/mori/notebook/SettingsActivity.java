package com.mori.notebook;

import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;

/** 设置页独立入口：底栏改为待办后，设置从主页顶栏菜单进入 */
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.settings);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }
}