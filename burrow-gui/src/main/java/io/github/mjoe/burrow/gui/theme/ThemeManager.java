/*
 * Copyright 2026 Burrow Contributors
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

package io.github.mjoe.burrow.gui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Manages application theme (Look and Feel).
 */
public final class ThemeManager {
    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    private static final String PREF_KEY_THEME = "theme";

    public enum Theme {
        LIGHT("Light", FlatLightLaf.class),
        DARK("Dark", FlatDarkLaf.class);

        private final String displayName;
        private final Class<? extends FlatLaf> lafClass;

        Theme(String displayName, Class<? extends FlatLaf> lafClass) {
            this.displayName = displayName;
            this.lafClass = lafClass;
        }

        public String displayName() {
            return displayName;
        }

        public Class<? extends FlatLaf> lafClass() {
            return lafClass;
        }
    }

    private Theme currentTheme;

    public ThemeManager() {
        this.currentTheme = Theme.DARK; // Default to dark theme
    }

    public void applyTheme() {
        applyTheme(currentTheme);
    }

    public void applyTheme(Theme theme) {
        try {
            var laf = theme.lafClass().getDeclaredConstructor().newInstance();
            UIManager.setLookAndFeel(laf);
            currentTheme = theme;
            log.info("Applied theme: {}", theme.displayName());
        } catch (Exception e) {
            log.error("Failed to apply theme: {}", e.getMessage());
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void cycleTheme() {
        var themes = Theme.values();
        var nextIndex = (currentTheme.ordinal() + 1) % themes.length;
        applyTheme(themes[nextIndex]);
    }
}
