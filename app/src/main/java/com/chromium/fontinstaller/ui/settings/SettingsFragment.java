/*
 * Copyright 2015 Priyesh Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chromium.fontinstaller.ui.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;

import com.chromium.fontinstaller.BuildConfig;
import com.chromium.fontinstaller.R;
import com.chromium.fontinstaller.SecretStuff;
import com.chromium.fontinstaller.ui.main.MainActivity;
import com.chromium.fontinstaller.util.Licenses;
import com.chromium.fontinstaller.util.PreferencesManager;
import com.chromium.fontinstaller.util.billing.IabHelper;
import com.chromium.fontinstaller.util.billing.Purchase;
import com.nispok.snackbar.Snackbar;

import de.psdev.licensesdialog.LicensesDialog;

public class SettingsFragment extends PreferenceFragment {

    private PreferencesManager prefs;
    private IabHelper billingHelper;
    private IabHelper.OnIabPurchaseFinishedListener purchaseListener;
    private IabHelper.OnConsumeFinishedListener consumeListener;
    private Preference donate;

    public static final String DONATE_SKU = "android.test.purchased";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        prefs = PreferencesManager.getInstance(getActivity());

        billingHelper = new IabHelper(getActivity(), SecretStuff.LICENSE_KEY);

        CheckBoxPreference trueFont = (CheckBoxPreference) findPreference("trueFont");
        trueFont.setOnPreferenceChangeListener((pref, newValue) -> handleTrueFont(newValue));

        Preference source = findPreference("viewSource");
        source.setOnPreferenceClickListener(pref -> viewSource());

        Preference licenses = findPreference("licenses");
        licenses.setOnPreferenceClickListener(pref -> openLicensesDialog());

        Preference appVersion = findPreference("appVersion");
        appVersion.setSummary(BuildConfig.VERSION_NAME + " - " + BuildConfig.BUILD_TYPE);

        donate = findPreference("donate");

        billingHelper.startSetup(result -> {
            if (result.isFailure()) {
                donate.setSummary("A problem was encountered while setting up In-App Billing");
                donate.setEnabled(false);
            }
        });

        donate.setOnPreferenceClickListener(pref -> makeDonation());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (billingHelper != null) billingHelper.dispose();
        billingHelper = null;
    }

    private boolean makeDonation() {
        billingHelper.launchPurchaseFlow(getActivity(), DONATE_SKU, 1, purchaseListener, "");
        purchaseListener = (result, purchase) -> {
            if (result.isFailure()) {
                Snackbar.with(getActivity()).text("Failed to make donation").show(getActivity());
            } else if (purchase.getSku().equals(DONATE_SKU)) {
                queryInventory();
            }
        };

        return true;
    }

    private void queryInventory() {
        billingHelper.queryInventoryAsync((result, inventory) -> {
            Purchase donation = inventory.getPurchase(DONATE_SKU);
            if (donate != null) consumePurchase(donation);
        });
    }

    private void consumePurchase(Purchase purchase) {
        billingHelper.consumeAsync(purchase, consumeListener);
        consumeListener = (consumedPurchase, result) -> {
            if (result.isSuccess())
                Snackbar.with(getActivity()).text("Donation made successfully").show(getActivity());
        };
    }

    private boolean handleTrueFont(Object newValue) {
        prefs.setBoolean(PreferencesManager.KEY_ENABLE_TRUEFONT, (boolean) newValue);
        showRestartDialog();
        return true;
    }

    private boolean viewSource() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://github.com/ItsPriyesh/FontInstaller"));
        startActivity(intent);
        return true;
    }

    private boolean openLicensesDialog() {
        new LicensesDialog.Builder(getActivity())
                .setNotices(Licenses.getNotices())
                .build().show();
        return true;
    }

    private void showRestartDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage("Restart the app for the change to take effect.")
                .setPositiveButton("Restart", (dialog, id) -> restartApp())
                .create().show();
    }

    private void restartApp() {
        ActivityCompat.finishAffinity(getActivity());
        startActivity(new Intent(getActivity(), MainActivity.class));
    }
}
