package org.pluschapluschat.russenger;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected boolean isAndroidTestEnv() {
        return true;
    }
}
