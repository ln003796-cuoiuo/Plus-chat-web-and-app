package org.pluschat.ui.ActionBar.theme;

import org.pluschat.tgnet.TLRPC;

public interface ITheme {
    long getThemeId();

    TLRPC.ThemeSettings getThemeSettings(int settingsIndex);
    TLRPC.WallPaper getThemeWallPaper(int settingsIndex);
}
