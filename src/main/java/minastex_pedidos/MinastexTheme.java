package minastex_pedidos;

import megalodonte.base.theme.ThemeBorder;
import megalodonte.base.theme.ThemeColors;
import megalodonte.theme.DefaultTheme;

/** Minastex palette: visible, neutral form borders and restrained focus feedback. */
public final class MinastexTheme extends DefaultTheme {
    @Override public ThemeColors colors() {
        ThemeColors base = super.colors();
        return new ThemeColors(base.background(), base.surface(), base.primary(), base.secondary(),
                base.textPrimary(), base.textSecondary(), "#b8c8d3", base.placeholder(),
                base.selection(), "#287fa3", base.hover(), base.success(), base.warning(), base.danger());
    }

    @Override public ThemeBorder border() {
        ThemeBorder base = super.border();
        return new ThemeBorder(1, base.radiusSm(), base.radiusMd(), base.radiusLg());
    }
}
