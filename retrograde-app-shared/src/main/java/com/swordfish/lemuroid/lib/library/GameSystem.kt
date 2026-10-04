*
 * GameSystem.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.library

import androidx.annotation.StringRes
import com.swordfish.lemuroid.lib.R
import com.swordfish.lemuroid.lib.core.CoreVariable
import java.util.Locale

data class GameSystem(
    val id: SystemID,
    val libretroFullName: String,
    @StringRes
    val titleResId: Int,
    @StringRes
    val shortTitleResId: Int,
    val systemCoreConfigs: List<SystemCoreConfig>,
    val uniqueExtensions: List<String>,
    val scanOptions: ScanOptions = ScanOptions(),
    val supportedExtensions: List<String> = uniqueExtensions,
    val hasMultiDiskSupport: Boolean = false,
    val fastForwardSupport: Boolean = true,
    val hasTouchScreen: Boolean = false,
) {
    companion object {
        private val SYSTEMS =
            listOf(
                GameSystem(
                    SystemID.ATARI2600,
                    "Atari - 2600",
                    R.string.game_system_title_atari2600,
                    R.string.game_system_abbr_atari2600,
                    listOf(
                        SystemCoreConfig(
                            coreID = CoreID.STELLA,
                            exposedSettings =
                                listOf(
                                    ExposedSetting(
                                        "stella_filter",
                                        R.string.setting_stella_filter,
                                        arrayListOf(
                                            ExposedSetting.Value(
                                                "disabled",
                                                R.string.value_stella_filter_disabled,
                                            ),
                                            ExposedSetting.Value(
                                                "composite",
                                                R.string.value_stella_filter_composite,
                                            ),
                                            ExposedSetting.Value(
                                                "s-video",
                                                R.string.value_stella_filter_svideo,
                                            ),
                                            ExposedSetting.Value("rgb", R.string.value_stella_filter_rgb),
                                            ExposedSetting.Value(
                                                "badly adjusted",
                                                R.string.value_stella_filter_badlyadjusted,
                                            ),
                                        ),
                                    ),
                                    ExposedSetting(
                                        "stella_crop_hoverscan",
                                        R.string.setting_stella_crop_hoverscan,
                                    ),
                                ),
                            controllerConfigs =
                                hashMapOf(
                                    0 to arrayListOf(ControllerConfigs.ATARI_2600),
                                ),
                        ),
                    ),
                    uniqueExtensions = listOf("a26"),
                ),
                GameSystem(
                    SystemID.NES,
                    "Nintendo - Nintendo Entertainment System",
                    R.string.game_system_title_nes,
                    R.string.game_system_abbr_nes,
                    listOf(
                        SystemCoreConfig(
                            CoreID.FCEUMM,
                            exposedSettings =
                                listOf(
                                    ExposedSetting(
                                        "fceumm_overscan_h",
                                        R.string.setting_fceumm_overscan_h,
                                    ),
                                    ExposedSetting(
                                        "fceumm_overscan_v",
                                        R.string.setting_fceumm_overscan_v,
                                    ),
                                ),
                            exposedAdvancedSettings =
                                listOf(
                                    ExposedSetting(
                                        "fceumm_nospritelimit",
                                        R.string.setting_fceumm_nospritelimit,
                                    ),
                                ),
                            controllerConfigs =
                                hashMapOf(
                                    0 to arrayListOf(ControllerConfigs.NES),
                                ),
                        ),
                    ),
                    uniqueExtensions = listOf("nes"),
                ),
                GameSystem(
                    SystemID.SNES,
                    "Nintendo - Super Nintendo Entertainment System",
                    R.string.game_system_title_snes,
                    R.string.game_system_abbr_snes,
                    listOf(
                        SystemCoreConfig(
                            CoreID.SNES9X,
                            controllerConfigs =
                                hashMapOf(
                                    0 to arrayListOf(ControllerConfigs.SNES),
                                    1 to arrayListOf(ControllerConfigs.SNES),
                                ),
                        ),
                    ),
                    uniqueExtensions = listOf("smc", "sfc"),
                ),
                GameSystem(
                    SystemID.SMS,
                    "Sega - Master System - Mark III",
                    R.string.game_system_title_sms,
                    R.string.game_system_abbr_sms,
                    listOf(
                        SystemCoreConfig(
                            CoreID.GENESIS_PLUS_GX,
                            exposedSettings =
                                listOf(
                                    ExposedSetting(
                                        "genesis_plus_gx_blargg_ntsc_filter",
                                        R.string.setting_genesis_plus_gx_blargg_ntsc_filter,
                                        arrayListOf(
                                            ExposedSetting.Value(
                                                "disabled",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_disabled,
                                            ),
                                            ExposedSetting.Value(
                                                "monochrome",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_monochrome,
                                            ),
                                            ExposedSetting.Value(
                                                "composite",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_composite,
                                            ),
                                            ExposedSetting.Value(
                                                "svideo",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_svideo,
                                            ),
                                            ExposedSetting.Value(
                                                "rgb",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_rgb,
                                            ),
                                        ),
                                    ),
                                ),
                            exposedAdvancedSettings =
                                listOf(
                                    ExposedSetting(
                                        "genesis_plus_gx_no_sprite_limit",
                                        R.string.setting_genesis_plus_gx_no_sprite_limit,
                                    ),
                                    ExposedSetting(
                                        "genesis_plus_gx_overscan",
                                        R.string.setting_genesis_plus_gx_overscan,
                                        arrayListOf(
                                            ExposedSetting.Value(
                                                "disabled",
                                                R.string.value_genesis_plus_gx_overscan_disabled,
                                            ),
                                            ExposedSetting.Value(
                                                "top/bottom",
                                                R.string.value_genesis_plus_gx_overscan_topbottom,
                                            ),
                                            ExposedSetting.Value(
                                                "left/right",
                                                R.string.value_genesis_plus_gx_overscan_leftright,
                                            ),
                                            ExposedSetting.Value(
                                                "full",
                                                R.string.value_genesis_plus_gx_overscan_full,
                                            ),
                                        ),
                                    ),
                                ),
                            controllerConfigs =
                                hashMapOf(
                                    0 to arrayListOf(ControllerConfigs.SMS),
                                ),
                        ),
                    ),
                    uniqueExtensions = listOf("sms"),
                ),
                GameSystem(
                    SystemID.GENESIS,
                    "Sega - Mega Drive - Genesis",
                    R.string.game_system_title_genesis,
                    R.string.game_system_abbr_genesis,
                    listOf(
                        SystemCoreConfig(
                            CoreID.GENESIS_PLUS_GX,
                            exposedSettings =
                                listOf(
                                    ExposedSetting(
                                        "genesis_plus_gx_blargg_ntsc_filter",
                                        R.string.setting_genesis_plus_gx_blargg_ntsc_filter,
                                        arrayListOf(
                                            ExposedSetting.Value(
                                                "disabled",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_disabled,
                                            ),
                                            ExposedSetting.Value(
                                                "monochrome",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_monochrome,
                                            ),
                                            ExposedSetting.Value(
                                                "composite",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_composite,
                                            ),
                                            ExposedSetting.Value(
                                                "svideo",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_svideo,
                                            ),
                                            ExposedSetting.Value(
                                                "rgb",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_rgb,
                                            ),
                                        ),
                                    ),
                                ),
                            exposedAdvancedSettings =
                                listOf(
                                    ExposedSetting(
                                        "genesis_plus_gx_no_sprite_limit",
                                        R.string.setting_genesis_plus_gx_no_sprite_limit,
                                    ),
                                    ExposedSetting(
                                        "genesis_plus_gx_overscan",
                                        R.string.setting_genesis_plus_gx_overscan,
                                        arrayListOf(
                                            ExposedSetting.Value(
                                                "disabled",
                                                R.string.value_genesis_plus_gx_overscan_disabled,
                                            ),
                                            ExposedSetting.Value(
                                                "top/bottom",
                                                R.string.value_genesis_plus_gx_overscan_topbottom,
                                            ),
                                            ExposedSetting.Value(
                                                "left/right",
                                                R.string.value_genesis_plus_gx_overscan_leftright,
                                            ),
                                            ExposedSetting.Value(
                                                "full",
                                                R.string.value_genesis_plus_gx_overscan_full,
                                            ),
                                        ),
                                    ),
                                ),
                            controllerConfigs =
                                hashMapOf(
                                    0 to
                                        arrayListOf(
                                            ControllerConfigs.GENESIS_3,
                                            ControllerConfigs.GENESIS_6,
                                        ),
                                    1 to
                                        arrayListOf(
                                            ControllerConfigs.GENESIS_3,
                                            ControllerConfigs.GENESIS_6,
                                        ),
                                    2 to
                                        arrayListOf(
                                            ControllerConfigs.GENESIS_3,
                                            ControllerConfigs.GENESIS_6,
                                        ),
                                    3 to
                                        arrayListOf(
                                            ControllerConfigs.GENESIS_3,
                                            ControllerConfigs.GENESIS_6,
                                        ),
                                ),
                        ),
                    ),
                    uniqueExtensions = listOf("gen", "smd", "md"),
                ),
                GameSystem(
                    SystemID.SEGACD,
                    "Sega - Mega-CD - Sega CD",
                    R.string.game_system_title_scd,
                    R.string.game_system_abbr_scd,
                    listOf(
                        SystemCoreConfig(
                            CoreID.GENESIS_PLUS_GX,
                            exposedSettings =
                                listOf(
                                    ExposedSetting(
                                        "genesis_plus_gx_blargg_ntsc_filter",
                                        R.string.setting_genesis_plus_gx_blargg_ntsc_filter,
                                        arrayListOf(
                                            ExposedSetting.Value(
                                                "disabled",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_disabled,
                                            ),
                                            ExposedSetting.Value(
                                                "monochrome",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_monochrome,
                                            ),
                                            ExposedSetting.Value(
                                                "composite",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_composite,
                                            ),
                                            ExposedSetting.Value(
                                                "svideo",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_svideo,
                                            ),
                                            ExposedSetting.Value(
                                                "rgb",
                                                R.string.value_genesis_plus_gx_blargg_ntsc_filter_rgb,
                                            ),
                                        ),
                                    ),
                                ),
                            exposedAdvancedSettings =
                                listOf(
                                    ExposedSetting(
                                        "genesis_plus_gx_no_sprite_limit",
                                        R.string.setting_genesis_plus_gx_no_sprite_limit,
                                    ),
                                    ExposedSetting(
                                        "genesis_plus_gx_overscan",
                                        R.string.setting_genesis_plus_gx_overscan,
                                        arrayListOf(
                                            ExposedSetting.Value(
                                                "disabled",
                                                R.string.value_genesis_plus_gx_overscan_disabled,
                                            ),
                                            ExposedSetting.Value(
                                                "top/bottom",
                                                R.string.value_genesis_plus_gx_overscan_topbottom,
                                            ),
                                            ExposedSetting.Value(
                                                "left/right",
                                                R.string.value_genesis_plus_gx_overscan_leftright,
                                            ),
                                            ExposedSetting.Value(
                                                "full",
                                                R.string.value_genesis_plus_gx_overscan_full,
          
