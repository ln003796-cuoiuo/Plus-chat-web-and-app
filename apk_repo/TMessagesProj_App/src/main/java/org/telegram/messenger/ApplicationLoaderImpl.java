package org.pluschapluschat.russenger;

import org.pluschapluschat.russenger.regular.BuildConfig;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
