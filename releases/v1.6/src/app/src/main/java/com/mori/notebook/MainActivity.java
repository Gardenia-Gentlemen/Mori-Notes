package com.mori.notebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 主页 / 待办 共用一个 ViewPager2，跟手真实滑动，底部导航双向联动。
 * 设置已从底栏移出，入口在主页顶栏菜单；回收站入口同在顶栏菜单。
 */
public class MainActivity extends BaseActivity {

    private static final int PAGE_HOME = 0;
    private static final int PAGE_TODO = 1;

    private ViewPager2 pager;
    private BottomNavigationView nav;
    private MaterialToolbar toolbar;
    private HomeFragment homeFragment;
    private TodoFragment todoFragment;
    /** 尚未交付给主页的外部 .mori 文件 Uri */
    private Uri pendingUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        nav = findViewById(R.id.bottom_nav);
        pager = findViewById(R.id.view_pager);

        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == PAGE_TODO) {
                    todoFragment = new TodoFragment();
                    return todoFragment;
                }
                homeFragment = new HomeFragment();
                return homeFragment;
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });
        pager.setOffscreenPageLimit(1);

        nav.setOnItemSelectedListener(item -> {
            int target = item.getItemId() == R.id.action_nav_todo ? PAGE_TODO : PAGE_HOME;
            if (pager.getCurrentItem() != target) pager.setCurrentItem(target, true);
            return true;
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int id = position == PAGE_TODO ? R.id.action_nav_todo : R.id.action_nav_home;
                if (nav.getSelectedItemId() != id) nav.setSelectedItemId(id);
                updateToolbar(position);
            }
        });
        updateToolbar(pager.getCurrentItem());
        deliverOpenIntent(getIntent());
    }


    private void updateToolbar(int position) {
        if (position == PAGE_TODO) {
            if (nav != null) nav.setVisibility(View.VISIBLE);
            if (todoFragment != null && todoFragment.isAdded()) {
                todoFragment.applyToolbar();
            } else {
                toolbar.getMenu().clear();
                toolbar.inflateMenu(R.menu.menu_todo);
                toolbar.setTitle("待办");
            }
        } else if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.applyToolbar();
        } else {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.menu_main);
            toolbar.setTitle(R.string.app_name);
        }
    }

    /** 把外部打开的文件转交给主页 */
    private void deliverOpenIntent(Intent intent) {
        if (!MoriImporter.isOpenFileIntent(intent)) return;
        Uri uri = MoriImporter.uriFromIntent(intent);
        if (uri == null) return;
        if (homeFragment != null && homeFragment.isAdded()) {
            homeFragment.handleOpenUri(uri);
        } else {
            pendingUri = uri;
        }
    }

    /** 供主页取走尚未处理的外部文件 Uri */
    public Uri consumePendingUri() {
        Uri u = pendingUri;
        pendingUri = null;
        return u;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (MoriImporter.isOpenFileIntent(intent) && pager.getCurrentItem() != PAGE_HOME) {
            pager.setCurrentItem(PAGE_HOME, false);
        }
        deliverOpenIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (homeFragment != null && homeFragment.handleBack()) return;
        if (pager.getCurrentItem() != PAGE_HOME) {
            pager.setCurrentItem(PAGE_HOME, true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (homeFragment != null && homeFragment.isAdded()) homeFragment.applyToolbar();
    }

    /** 主页隐藏/显示底部导航（多选时隐藏） */
    public void setBottomNavVisible(boolean visible) {
        if (nav == null) return;
        nav.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}