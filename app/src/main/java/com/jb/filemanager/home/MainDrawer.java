package com.jb.filemanager.home;

import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jb.filemanager.BaseActivity;
import com.jb.filemanager.R;
import com.jb.filemanager.TheApplication;
import com.jb.filemanager.eventbus.IOnEventMainThreadSubscriber;
import com.jb.filemanager.function.about.AboutActivity;
import com.jb.filemanager.function.applock.activity.AppLockPreActivity;
import com.jb.filemanager.function.applock.manager.LockerFloatLayerManager;
import com.jb.filemanager.function.feedback.FeedbackActivity;
import com.jb.filemanager.function.permissionalarm.manager.PermissionAlarmManager;
import com.jb.filemanager.function.rate.RateManager;
import com.jb.filemanager.function.rate.dialog.AbsRateDialog;
import com.jb.filemanager.function.rate.presenter.RateContract;
import com.jb.filemanager.function.setting.SettingActivity;
import com.jb.filemanager.function.tip.manager.StorageTipManager;
import com.jb.filemanager.function.tip.manager.UsbStateManager;
import com.jb.filemanager.function.trash.CleanTrashActivity;
import com.jb.filemanager.home.dialog.IntroduceChargeDialog;
import com.jb.filemanager.home.event.DrawerStatusChangeEvent;
import com.jb.filemanager.home.event.SwitcherChgStateEvent;
import com.jb.filemanager.manager.spm.IPreferencesIds;
import com.jb.filemanager.manager.spm.SharedPreferencesManager;
import com.jb.filemanager.statistics.StatisticsConstants;
import com.jb.filemanager.statistics.StatisticsTools;
import com.jb.filemanager.statistics.bean.Statistics101Bean;
import com.jb.filemanager.util.AppUtils;
import com.jb.filemanager.util.Logger;
import com.jb.filemanager.util.QuickClickGuard;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by bill wang on 16/8/19.
 */
public class MainDrawer implements View.OnClickListener {

    public static final String FEED_BACK = "feed_back";
    private static final String ABOUT = "about";
    private static final String CLEAN_TRASH = "clean_trash";
    private static final String APPLOCK = "app_lock";
    private ActionBarDrawerToggle mDrawerToggle;
    private QuickClickGuard mQuickClickGuard;
    private DrawerLayout mDrawerLayout;
    private BaseActivity mActivity;
    public static final int CLI_OPEN = 1;
    private static final int FLING_OPEN = 2;
    private int mOpenType = FLING_OPEN;
    //监听外部的开关状态变化
    private IOnEventMainThreadSubscriber<SwitcherChgStateEvent> mSwitcherChgStateEventSubscriber = new IOnEventMainThreadSubscriber<SwitcherChgStateEvent>() {
        @Override
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEventMainThread(SwitcherChgStateEvent event) {
            if (mActivity == null || mActivity.isFinishing()) {
                return;
            }
            View item;
            if (event.isFreespace()) {
                item = mActivity.findViewById(R.id.tv_drawer_low_space);
            } else if (event.isLogger()) {
                item = mActivity.findViewById(R.id.tv_drawer_logger_notification);
            } else {
                item = mActivity.findViewById(R.id.tv_drawer_usb_plugin);
            }
            item.setSelected(event.isEnable());
        }
    };

    public MainDrawer(BaseActivity activity) {
        mActivity = activity;
        if (!TheApplication.getGlobalEventBus().isRegistered(mSwitcherChgStateEventSubscriber)) {
            TheApplication.getGlobalEventBus().register(mSwitcherChgStateEventSubscriber);
        }
    }

    public void initDrawer() {
        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.dl_drawer);

        // enableOrDisable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                mActivity,                  /* host Activity */
                mDrawerLayout,          /* DrawerLayout object */
                null,
                0,                     /* "open drawer" description for accessibility */
                0                      /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View view) {
                mOpenType = FLING_OPEN;
            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // drawer状态改变的回调
            }

            // an unknown crash: No drawer view found with gravity LEFT
            @Override
            public boolean onOptionsItemSelected(MenuItem item) {
                if (item != null && item.getItemId() == android.R.id.home) {
                    toggleMenu();
                    return true;
                }
                return super.onOptionsItemSelected(item);
            }
        };

        mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        initDrawerItems();

        mQuickClickGuard = new QuickClickGuard();
    }

    private void toggleMenu() {
        if (mDrawerLayout == null) {
            return;
        }
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            EventBus.getDefault().post(new DrawerStatusChangeEvent(false));
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START);
            EventBus.getDefault().post(new DrawerStatusChangeEvent(true));
        }
    }

    private void initDrawerItems() {
        TextView appLockerItem = (TextView) mActivity.findViewById(R.id.tv_drawer_app_locker);
        if (appLockerItem != null) {
            appLockerItem.getPaint().setAntiAlias(true);
            appLockerItem.setOnClickListener(this);
        }

        TextView smartChargeItem = (TextView) mActivity.findViewById(R.id.tv_drawer_smart_charge);
        if (smartChargeItem != null) {
            smartChargeItem.getPaint().setAntiAlias(true);
            smartChargeItem.setOnClickListener(this);
        }

        TextView usbPluginItem = (TextView) mActivity.findViewById(R.id.tv_drawer_usb_plugin);
        if (usbPluginItem != null) {
            usbPluginItem.getPaint().setAntiAlias(true);
            usbPluginItem.setOnClickListener(this);
        }
        usbPluginItem.setSelected(UsbStateManager.getInstance().isSwitchEnable());

        TextView lowSpaceItem = (TextView) mActivity.findViewById(R.id.tv_drawer_low_space);
        if (lowSpaceItem != null) {
            lowSpaceItem.getPaint().setAntiAlias(true);
            lowSpaceItem.setOnClickListener(this);
        }
        lowSpaceItem.setSelected(StorageTipManager.getInstance().isSwitchEnable());

        TextView loggerNotificationItem = (TextView) mActivity.findViewById(R.id.tv_drawer_logger_notification);
        if (loggerNotificationItem != null) {
            loggerNotificationItem.getPaint().setAntiAlias(true);
            loggerNotificationItem.setOnClickListener(this);
        }
        loggerNotificationItem.setSelected(PermissionAlarmManager.getInstance().isSwitchEnable());

        TextView ratingItem = (TextView) mActivity.findViewById(R.id.tv_drawer_rating);
        if (ratingItem != null) {
            ratingItem.getPaint().setAntiAlias(true);
            ratingItem.setOnClickListener(this);
        }

        TextView updateItem = (TextView) mActivity.findViewById(R.id.tv_drawer_update);
        if (updateItem != null) {
            updateItem.getPaint().setAntiAlias(true);
            updateItem.setOnClickListener(this);
        }

        TextView helpItem = (TextView) mActivity.findViewById(R.id.tv_drawer_help);
        if (helpItem != null) {
            helpItem.getPaint().setAntiAlias(true);
            helpItem.setOnClickListener(this);
        }

        TextView aboutItem = (TextView) mActivity.findViewById(R.id.tv_drawer_about);
        if (aboutItem != null) {
            aboutItem.getPaint().setAntiAlias(true);
            aboutItem.setOnClickListener(this);
        }
    }

    void openDrawerWithDelay(int delayMills) {
        if (mDrawerLayout == null) {
            mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.left_drawer);
        }
        mDrawerLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.openDrawer(GravityCompat.START);
                EventBus.getDefault().post(new DrawerStatusChangeEvent(true));
            }
        }, delayMills);
    }

    void closeDrawerWithDelay(int delayMills) {
        if (mDrawerLayout == null) {
            mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.left_drawer);
        }
        mDrawerLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawers();
                EventBus.getDefault().post(new DrawerStatusChangeEvent(false));
            }
        }, delayMills);
    }

    public boolean isDrawerOpened() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void onPostCreate() {
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void setDrawerEnable(boolean enable) {
        if (enable) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    void setOpenType(int openType) {
        this.mOpenType = openType;
    }

    @Override
    public void onClick(final View v) {
        if (mQuickClickGuard.isQuickClick(v.getId())) {
            return;
        }
        switch (v.getId()) {
            case R.id.tv_drawer_app_locker:
                boolean isAppLockerEnable = SharedPreferencesManager.getInstance(TheApplication.getAppContext()).getBoolean(IPreferencesIds.KEY_APP_LOCK_ENABLE, false);
                statisticsClickAppLocker(isAppLockerEnable);
                jumpToApplock();

                break;
            case R.id.tv_drawer_smart_charge:
                IntroduceChargeDialog introduceChargeDialog = new IntroduceChargeDialog(mActivity);
                introduceChargeDialog.show();

                boolean isSmartChargeEnable = SharedPreferencesManager.getInstance(TheApplication.getAppContext()).getBoolean(IPreferencesIds.KEY_SMART_CHARGE_ENABLE, false);
                statisticsClickSmartCharge(isSmartChargeEnable);
                break;
            case R.id.tv_drawer_usb_plugin: {
                boolean isSwitch = UsbStateManager.getInstance().changerSwitch();
                v.setSelected(isSwitch);
                AppUtils.showToast(TheApplication.getInstance(), isSwitch ? R.string.toast_usb_plugin_switch_enable : R.string.toast_usb_plugin_switch_disable);

                statisticsClickUSBSwitch(isSwitch);
            }
            break;
            case R.id.tv_drawer_low_space: {
                boolean isSwitch = StorageTipManager.getInstance().changerSwitch();
                v.setSelected(isSwitch);
                AppUtils.showToast(TheApplication.getInstance(), isSwitch ? R.string.toast_low_space_switch_enable : R.string.toast_low_space_switch_disable);

                statisticsClickLowSwitch(isSwitch);
            }
            break;
            case R.id.tv_drawer_logger_notification: {
                boolean isSwitch = PermissionAlarmManager.getInstance().changerSwitch();
                v.setSelected(isSwitch);

                AppUtils.showToast(TheApplication.getInstance(), isSwitch ? R.string.toast_logger_notification_switch_enable : R.string.toast_logger_notification_switch_disable);
                statisticsClickLoggerSwitch(isSwitch);
            }
            break;
            case R.id.tv_drawer_rating:
                final RateContract.View view = ((RateContract.View) mActivity);
                RateManager.getsInstance().commitRateSuccess();
                view.showLoveDialog(new AbsRateDialog.OnPressListener() {
                    @Override
                    public void pressBack() {
                        view.dismissLoveDialog();
                    }

                    @Override
                    public void pressYes() {
                        view.dismissLoveDialog();
                        view.gotoGp();
                    }

                    @Override
                    public void pressNo() {
                        view.dismissLoveDialog();
                    }
                });
                statisticsClickRating();
                break;
            case R.id.tv_drawer_update:
                // TODO @wangzq
                statisticsClickUpdate();
                break;
            case R.id.tv_drawer_help:
                jumpToFeedBack();
                statisticsClickFeedback();
                break;
            case R.id.tv_drawer_about:
                jumpToAbout();
                statisticsClickAbout();
                break;

            default:
                break;
        }
    }

    private void jumpToCleanCrash() {
        closeDrawerWithDelay(0);
        delayStartActivity(new Intent(mActivity, CleanTrashActivity.class));
    }

    private void jumpToAbout() {
        closeDrawerWithDelay(0);
        delayStartActivity(new Intent(mActivity, AboutActivity.class));
    }

    private void jumpToFeedBack() {
        closeDrawerWithDelay(0);
        delayStartActivity(new Intent(mActivity, FeedbackActivity.class));
    }

    private void jumpToSetting() {
        closeDrawerWithDelay(0);
        delayStartActivity(new Intent(mActivity, SettingActivity.class));
    }

    private void jumpToApplock() {
        SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(TheApplication.getAppContext());
        boolean isEnable = sharedPreferencesManager.getBoolean(IPreferencesIds.KEY_APP_LOCK_ENABLE, false);
        if (!isEnable) {
            Intent i = new Intent(mActivity, AppLockPreActivity.class);
            mActivity.startActivity(i);
        } else {
            LockerFloatLayerManager.getInstance().showFloatViewInSide();
        }
    }

    /**
     * 延时跳转
     */
    private void delayStartActivity(final Intent intent) {
        TheApplication.postRunOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (intent != null && mActivity != null) {
                    mActivity.startActivity(intent);
                }
            }
        }, 260);
    }

    private void statisticsClickAppLocker(boolean isEnable) {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_ENTER_APP_LOCKER;
        bean.mEntrance = isEnable ? "1" : "2";
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickSmartCharge(boolean isEnable) {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_ENTER_SMART_CHARGE;
        bean.mEntrance = isEnable ? "1" : "2";
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickUSBSwitch(boolean isEnable) {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_CLICK_USB_SWITCH;
        bean.mTab = isEnable ? "1" : "2";
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickLowSwitch(boolean isEnable) {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_CLICK_LOW_SWITCH;
        bean.mTab = isEnable ? "1" : "2";
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickLoggerSwitch(boolean isEnable) {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_CLICK_LOGGER_SWITCH;
        bean.mTab = isEnable ? "1" : "2";
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickRating() {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_CLICK_RATING;
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickUpdate() {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_CLICK_UPDATE;
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickFeedback() {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_CLICK_FEEDBACK;
        StatisticsTools.upload101InfoNew(bean);
    }

    private void statisticsClickAbout() {
        Statistics101Bean bean = Statistics101Bean.builder();
        bean.mOperateId = StatisticsConstants.DRAWER_CLICK_ABOUT;
        StatisticsTools.upload101InfoNew(bean);
    }
}