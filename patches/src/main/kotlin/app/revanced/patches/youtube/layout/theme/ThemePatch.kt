package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.layout.theme.THEME_COLOR_OPTION_DESCRIPTION
import app.revanced.patches.shared.layout.theme.THEME_DEFAULT_DARK_COLOR_NAMES
import app.revanced.patches.shared.layout.theme.THEME_DEFAULT_LIGHT_COLOR_NAMES
import app.revanced.patches.shared.layout.theme.baseThemePatch
import app.revanced.patches.shared.layout.theme.baseThemeResourcePatch
import app.revanced.patches.shared.layout.theme.darkThemeBackgroundColorOption
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.shared.misc.settings.overrideThemeColors
import app.revanced.patches.shared.misc.settings.preference.InputType
import app.revanced.patches.shared.misc.settings.preference.ListPreference
import app.revanced.patches.shared.misc.settings.preference.PreferenceCategory
import app.revanced.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.shared.misc.settings.preference.TextPreference
import app.revanced.patches.youtube.layout.seekbar.seekbarColorPatch
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.featureflag.booleanFeatureFlagHookPatch
import app.revanced.patches.youtube.misc.featureflag.overrideBooleanFeatureFlag
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.findElementByAttributeValueOrThrow
import app.revanced.util.forEachChildElement
import app.revanced.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/theme/ThemePatch;"

val themePatch = baseThemePatch(
    extensionClassDescriptor = EXTENSION_CLASS_DESCRIPTOR,

    block = {
        val lightThemeBackgroundColor by stringOption(
            name = "Light theme background color",
            default = "@android:color/white",
            values = mapOf(
                "White" to "@android:color/white",
                "Material You" to "@android:color/system_neutral1_50",
                "Catppuccin (Latte)" to "#E6E9EF",
                "Light pink" to "#FCCFF3",
                "Light blue" to "#D1E0FF",
                "Light green" to "#CCFFCC",
                "Light yellow" to "#FDFFCC",
                "Light orange" to "#FFE6CC",
                "Light red" to "#FFD6D6",
            ),
            description = THEME_COLOR_OPTION_DESCRIPTION,
        )

        val themeResourcePatch = resourcePatch {
            dependsOn(resourceMappingPatch)

            apply {
                overrideThemeColors(
                    lightThemeBackgroundColor!!,
                    darkThemeBackgroundColorOption.value!!,
                )

                fun addColorResource(
                    resourceFile: String,
                    colorName: String,
                    colorValue: String,
                ) {
                    document(resourceFile).use { document ->
                        val resourcesNode =
                            document.getElementsByTagName("resources").item(0) as Element

                        resourcesNode.appendChild(
                            document.createElement("color").apply {
                                setAttribute("name", colorName)
                                setAttribute("category", "color")
                                textContent = colorValue
                            },
                        )
                    }
                }

                // Add a dynamic background color to the colors.xml file.
                val splashBackgroundColorKey = "revanced_splash_background_color"
                addColorResource(
                    "res/values/colors.xml",
                    splashBackgroundColorKey,
                    lightThemeBackgroundColor!!,
                )
                addColorResource(
                    "res/values-night/colors.xml",
                    splashBackgroundColorKey,
                    darkThemeBackgroundColorOption.value!!,
                )

                // Edit splash screen files and change the background color.
                arrayOf(
                    "res/drawable/quantum_launchscreen_youtube.xml",
                    "res/drawable-sw600dp/quantum_launchscreen_youtube.xml",
                ).forEach editSplashScreen@{ resourceFileName ->
                    document(resourceFileName).use { document ->
                        document.getElementsByTagName(
                            "layer-list",
                        ).item(0).forEachChildElement { node ->
                            if (node.hasAttribute("android:drawable")) {
                                node.setAttribute(
                                    "android:drawable",
                                    "@color/$splashBackgroundColorKey",
                                )
                                return@editSplashScreen
                            }
                        }

                        throw PatchException("Failed to modify launch screen")
                    }
                }

                // Change the dark mode splash screen background color.
                document("res/values-night/styles.xml").use { document ->
                    // Use the existing night mode specific style of the splash screen.
                    val style = document.childNodes.findElementByAttributeValueOrThrow(
                        "name",
                        "Theme.YouTube.Home",
                    )

                    // Fix status and navigation bar showing white on some Android devices,
                    // such as SDK 28 Android 10 medium tablet.
                    val colorSplashBackgroundColor = "@color/$splashBackgroundColorKey"
                    arrayOf(
                        "android:navigationBarColor" to colorSplashBackgroundColor,
                        "android:windowBackground" to colorSplashBackgroundColor,
                        "android:colorBackground" to colorSplashBackgroundColor,
                        "colorPrimaryDark" to colorSplashBackgroundColor,
                        "android:windowLightStatusBar" to "false",
                    ).forEach { (name, value) ->
                        val styleItem = document.createElement("item")
                        styleItem.setAttribute("name", name)
                        styleItem.textContent = value
                        style.appendChild(styleItem)
                    }
                }
            }
        }
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            addResourcesPatch,
            booleanFeatureFlagHookPatch,
            seekbarColorPatch,
            baseThemeResourcePatch(
                lightColorReplacement = { lightThemeBackgroundColor!! },
                getDarkColorNames = {
                    THEME_DEFAULT_DARK_COLOR_NAMES + setOf(
                        // yt_ref_color_constants_baseline_black_black0
                        "yt_sys_color_baseline_dark_menu_background",
                        // yt_ref_color_constants_baseline_black_black1
                        "yt_sys_color_baseline_dark_static_black",
                        "yt_sys_color_baseline_dark_raised_background",
                        // yt_ref_color_constants_baseline_black_black3
                        "yt_sys_color_baseline_dark_base_background",
                        "yt_sys_color_baseline_dark_static_black",
                        "yt_sys_color_baseline_light_inverted_background",
                        "yt_sys_color_baseline_light_static_black",
                    )
                },
                getLightColorNames = {
                    THEME_DEFAULT_LIGHT_COLOR_NAMES + setOf(
                        "yt_sys_color_baseline_light_base_background",
                        "yt_sys_color_baseline_light_raised_background",
                    )
                }
            ),
            themeResourcePatch,
        )

        compatibleWith(
            "com.google.android.youtube"(
            "21.41.525-SECONDARY",
        ),
        )
    },

    executeBlock = {
        addResources("youtube", "layout.theme.themePatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_gradient_loading_screen"),
        )

        val preferences = mutableSetOf(
            SwitchPreference("revanced_seekbar_custom_color"),
            TextPreference(
                "revanced_seekbar_custom_color_primary",
                tag = "app.revanced.extension.shared.settings.preference.ColorPickerPreference",
                inputType = InputType.TEXT_CAP_CHARACTERS,
            ),
            TextPreference(
                "revanced_seekbar_custom_color_accent",
                tag = "app.revanced.extension.shared.settings.preference.ColorPickerPreference",
                inputType = InputType.TEXT_CAP_CHARACTERS,
            ),
        )

        PreferenceScreen.SEEKBAR.addPreferences(
            PreferenceCategory(
                titleKey = null,
                sorting = Sorting.UNSORTED,
                tag = "app.revanced.extension.shared.settings.preference.NoTitlePreferenceCategory",
                preferences = preferences,
            ),
        )

        PreferenceScreen.GENERAL.addPreferences(
            ListPreference("revanced_splash_screen_animation_style"),
        )

        overrideBooleanFeatureFlag(
            GRADIENT_LOADING_SCREEN_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->gradientLoadingScreenEnabled(Z)Z",
        )

        splashScreenStyleMethodMatch.method.insertLiteralOverride(
            splashScreenStyleMethodMatch[0],
            "$EXTENSION_CLASS_DESCRIPTOR->getLoadingScreenType(I)I",
        )

        showSplashScreen1MethodMatch.let {
            it.method.apply {
                val index = it[1]
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, ${EXTENSION_CLASS_DESCRIPTOR}->showSplashScreen(I)I
                        move-result v$register
                    """
                )
            }
        }

        showSplashScreen2MethodMatch.let {
            val insertIndex = it[1]
            it.method.apply {
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val registerA = insertInstruction.registerA
                val registerB = insertInstruction.registerB

                addInstructions(
                    insertIndex,
                    """
                        invoke-static { v$registerA, v$registerB }, ${EXTENSION_CLASS_DESCRIPTOR}->showSplashScreen(II)I
                        move-result v$registerA
                    """
                )
            }
        }
    },
)
