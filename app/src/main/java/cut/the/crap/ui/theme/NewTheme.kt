package cut.the.crap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cut.the.crap.ui.content.settings.ThemePreference

val newDarkColorScheme = darkColorScheme(
    // Primary - Softer muted blue for better eye comfort
    primary = PrimaryBlueDark,                   // Softer blue (#4A90E2)
    onPrimary = Color(0xFF000000),               // Black text on primary
    primaryContainer = Color(0xFF2D5A7F),        // Deeper blue container
    onPrimaryContainer = Color(0xFFD5E5F5),      // Light blue text on container
    inversePrimary = PrimaryBlueLighter,         // Lighter blue for contrast

    // Secondary - Warm amber accent
    secondary = SecondaryAmberDark,              // Muted amber (#E8A547)
    onSecondary = Color(0xFF000000),             // Black text on secondary
    secondaryContainer = Color(0xFF7A5A2E),      // Darker amber container
    onSecondaryContainer = Color(0xFFF5E5C8),    // Light amber text

    // Tertiary - Elegant rose accent
    tertiary = TertiaryRoseDark,                 // Soft rose (#D8647C)
    onTertiary = Color(0xFF000000),              // Black text on tertiary
    tertiaryContainer = Color(0xFF6B3D4E),       // Dark rose container
    onTertiaryContainer = Color(0xFFF5D5DD),     // Light rose text

    // Backgrounds & Surfaces - Enhanced depth with layering
    background = DarkBackground,                 // Deep background (#0F0F0F)
    onBackground = Color(0xFFE8EAED),            // Light gray text
    surface = DarkSurface,                       // Card surface (#1A1A1A)
    onSurface = Color(0xFFE8EAED),               // Light gray text
    surfaceVariant = DarkSurfaceVariant,         // Variant surface (#252525)
    onSurfaceVariant = Color(0xFFC8CACD),        // Medium gray text
    surfaceTint = PrimaryBlueDark,               // Tinting with muted blue

    // Inverse colors
    inverseSurface = DarkInverseSurface,         // Light surface for contrast
    inverseOnSurface = DarkInverseOnSurface,     // Dark text on light

    // Error colors
    error = DarkError,                           // Softer error red (#E57373)
    onError = Color(0xFF000000),                 // Black text on error
    errorContainer = DarkErrorContainer,         // Dark error container
    onErrorContainer = Color(0xFFF5C5C5),        // Light red text

    // Outlines
    outline = DarkOutline,                       // Subtle outlines (#3D3D3D)
    outlineVariant = DarkOutlineVariant,         // Even more subtle (#2A2A2A)

    // Scrim
    scrim = DarkScrim,                           // Modal overlay

    // Surface variations for elevation
    surfaceBright = DarkSurfaceBright,           // Brightest surface (#303030)
    surfaceContainer = Color(0xFF1C1C1C),        // Container surface
    surfaceContainerHigh = Color(0xFF242424),    // Higher elevation
    surfaceContainerHighest = DarkSurfaceElevated, // Highest elevation (#2A2A2A)
    surfaceContainerLow = Color(0xFF161616),     // Lower elevation
    surfaceContainerLowest = Color(0xFF121212),  // Lowest elevation
    surfaceDim = DarkBackground                  // Dimmed surface
)

val newLightColorScheme = lightColorScheme(
    // Primary - Professional deep blue
    primary = PrimaryBlueLight,                  // Deep blue (#2B5B8F)
    onPrimary = Color(0xFFFFFFFF),               // White text on primary
    primaryContainer = Color(0xFFB8D4EC),        // Light blue container
    onPrimaryContainer = Color(0xFF0D2840),      // Dark blue text on container
    inversePrimary = PrimaryBlueDark,            // Lighter blue for contrast

    // Secondary - Rich amber accent
    secondary = SecondaryAmberLight,             // Golden amber (#D68910)
    onSecondary = Color(0xFFFFFFFF),             // White text on secondary
    secondaryContainer = SecondaryAmberLighter,  // Soft amber container (#F5C76B)
    onSecondaryContainer = Color(0xFF3D2800),    // Dark amber text

    // Tertiary - Deep rose accent
    tertiary = TertiaryRoseLight,                // Deep rose (#B13A5E)
    onTertiary = Color(0xFFFFFFFF),              // White text on tertiary
    tertiaryContainer = TertiaryRoseLighter,     // Light rose container (#E896AA)
    onTertiaryContainer = Color(0xFF3D1620),     // Dark rose text

    // Backgrounds & Surfaces - Clean and modern with better contrast
    background = LightBackground,                // Light gray background (#F5F5F5)
    onBackground = Color(0xFF1A1A1A),            // Near black text
    surface = LightSurface,                      // Pure white (#FFFFFF)
    onSurface = Color(0xFF1A1A1A),               // Near black text
    surfaceVariant = LightSurfaceVariant,        // Medium gray for better contrast (#E8E8E8)
    onSurfaceVariant = Color(0xFF424242),        // Dark gray text
    surfaceTint = PrimaryBlueLight,              // Tinting with deep blue

    // Inverse colors
    inverseSurface = LightInverseSurface,        // Dark surface for contrast
    inverseOnSurface = LightInverseOnSurface,    // Light text on dark

    // Error colors
    error = LightError,                          // Clear error red (#D32F2F)
    onError = Color(0xFFFFFFFF),                 // White text on error
    errorContainer = LightErrorContainer,        // Light error container (#FFDAD6)
    onErrorContainer = Color(0xFF410002),        // Dark red text

    // Outlines
    outline = LightOutline,                      // Refined outlines (#CACACA)
    outlineVariant = LightOutlineVariant,        // Lighter variant (#E0E0E0)

    // Scrim
    scrim = LightScrim,                          // Modal overlay

    // Surface variations for elevation with better contrast
    surfaceBright = LightSurface,                // Brightest (white)
    surfaceContainer = LightSurfaceContainer,    // Container (#EEEEEE) - more contrast
    surfaceContainerHigh = Color(0xFFE0E0E0),    // Higher elevation - darker gray
    surfaceContainerHighest = LightSurfaceDim,   // Highest elevation (#DCDCDC)
    surfaceContainerLow = Color(0xFFF5F5F5),     // Lower elevation
    surfaceContainerLowest = LightSurface,       // Lowest (white)
    surfaceDim = LightSurfaceDim                 // Dimmed surface (#DCDCDC)
)

@Composable
fun MyAppTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    // Determine if dark theme should be used based on preference
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (darkTheme) newDarkColorScheme else newLightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography, // You can define Typography.kt for custom fonts
        content = content
    )
}


