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
 * 倒数日 / 主页 / 待办 共用一个 ViewPager2，跟手真实滑动，底部导航双向联动。
 * 设置已从底栏移出，入口在主页顶栏菜单；回收站入口同在顶栏菜单。
 */
public class MainActivity extends BaseActivity {
    private boolean tzDialogShown;
    private static final int PAGE_COUNTDOWN = 0;
    private static final int PAGE_HOME = 1;
    private static final int PAGE_TODO = 2;
    private ViewPager2 pager;
    private BottomNavigationView nav;
    private MaterialToolbar toolbar;
    private HomeFragment homeFragment;
    private TodoFragment todoFragment;
    private CountdownFragment countdownFragment;
    /** 尚未交付给主页的外部 .mori 文件 Uri */
    private Uri pendingUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerBackCallback();
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        nav = findViewById(R.id.bottom_nav);
        applyNavTheme();
        pager = findViewById(R.id.view_pager);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == PAGE_COUNTDOWN) {
                    countdownFragment = new CountdownFragment();
                    return countdownFragment;
                }
                if (position == PAGE_TODO) {
                    todoFragment = new TodoFragment();
                    return todoFragment;
                }
                homeFragment = new HomeFragment();
                return homeFragment;
            }
            @Override
            public int getItemCount() {
                return 3;
            }
        });
        pager.setOffscreenPageLimit(1);

        nav.setOnItemSelectedListener(item -> {
            int target = pageOf(item.getItemId());
            if (pager.getCurrentItem() != target) pager.setCurrentItem(target, true);
            return true;
        });
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int id = navIdOf(position);
                if (nav.getSelectedItemId() != id) nav.setSelectedItemId(id);
                updateToolbar(position);
            }
        });
        // 启动页由设置决定，默认主页（记事本）
        int startPage = UiPrefs.getStartPage(MainActivity.this);
        pager.setCurrentItem(startPage, false);
        nav.setSelectedItemId(navIdOf(startPage));
        updateToolbar(startPage);
        deliverOpenIntent(getIntent());
    }
    /** 底栏透明，图标/文字取当前主题色，切深色模式后同步。 */
    private void applyNavTheme() {
        if (nav == null) return;
        int primary = themeColor(com.google.android.material.R.attr.colorPrimary, 0xFF6750A4);
        nav.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        nav.setItemIconTintList(android.content.res.ColorStateList.valueOf(primary));
        nav.setItemTextColor(android.content.res.ColorStateList.valueOf(primary));
    }

    /** 底栏菜单项 → 页面序号 */
    private int pageOf(int menuItemId) {
        if (menuItemId == R.id.action_nav_countdown) return PAGE_COUNTDOWN;
        if (menuItemId == R.id.action_nav_todo) return PAGE_TODO;
        return PAGE_HOME;
    }
    /** 页面序号 → 底栏菜单项 */
    private int navIdOf(int position) {
        if (position == PAGE_COUNTDOWN) return R.id.action_nav_countdown;
        if (position == PAGE_TODO) return R.id.action_nav_todo;
        return R.id.action_nav_home;
    }
    private void updateToolbar(int position) {
        if (position == PAGE_COUNTDOWN) {
            if (nav != null) nav.setVisibility(View.VISIBLE);
            if (countdownFragment != null && countdownFragment.isAdded()) {
                countdownFragment.applyToolbar();
            } else {
                toolbar.getMenu().clear();
                toolbar.inflateMenu(R.menu.menu_countdown);
                toolbar.setTitle("倒数日");
            }
        } else if (position == PAGE_TODO) {
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
    /** Whether the given fragment is the page currently shown by the pager. */
    public boolean isCurrentPage(Fragment f) {
        if (pager == null || f == null) return false;
        Fragment cur = getSupportFragmentManager().findFragmentByTag("f" + pager.getCurrentItem());
        return cur == f;
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
    private void registerBackCallback() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (homeFragment != null && homeFragment.isAdded() && homeFragment.handleBack()) return;
                int startPage = UiPrefs.getStartPage(MainActivity.this);
                if (pager.getCurrentItem() != startPage) {
                    pager.setCurrentItem(startPage, true);
                    return;
                }
                setEnabled(false);
                MainActivity.this.getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        applyNavTheme();
        if (homeFragment != null && homeFragment.isAdded()) homeFragment.applyToolbar();
        checkTimeZoneChange();
    }

    /** 手机时区变更时询问是否同步应用时区。 */
    private void checkTimeZoneChange() {
        final String sys = java.util.TimeZone.getDefault().getID();
        String seen = CountdownPrefs.getSeenSystemTimeZone(this);
        if (seen.isEmpty()) {
            CountdownPrefs.setSeenSystemTimeZone(this, sys);
            return;
        }
        if (sys.equals(seen)) return;
        if (CountdownPrefs.isFollowSystem(this)) {
            CountdownPrefs.setSeenSystemTimeZone(this, sys);
            CountdownTime.setTimeZone(sys);
            return;
        }
        final String cur = CountdownPrefs.resolveTimeZoneId(this);
        if (cur.equals(sys)) {
            // 应用时区已经就是新系统时区，无需询问
            CountdownPrefs.setSeenSystemTimeZone(this, sys);
            CountdownTime.setTimeZone(sys);
            return;
        }
        if (tzDialogShown) return;
        tzDialogShown = true;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("手机时区已变更")
                .setMessage("检测到手机时区变更为 " + sys + "。\n是否把应用时区由 " + cur
                        + " 同步为 " + sys + "？")
                .setNegativeButton("保持当前", (d, w) -> {
                    tzDialogShown = false;
                    CountdownPrefs.setSeenSystemTimeZone(this, sys);
                })
                .setPositiveButton("同步", (d, w) -> {
                    tzDialogShown = false;
                    CountdownPrefs.setTimeZoneId(this, sys);
                    CountdownPrefs.setSeenSystemTimeZone(this, sys);
                    recreateWithFade();
                })
                .show();
    }
    /** 主页隐藏/显示底部导航（多选时隐藏） */
    public void setBottomNavVisible(boolean visible) {
        if (nav == null) return;
        nav.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
