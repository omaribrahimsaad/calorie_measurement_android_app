package com.example.caloriemeasurement

import android.content.Context
import androidx.annotation.StringRes

// Define the enum for option types
enum class DetectionMode {
    CALORIE_CAPTURE,
    SIGN_LANAGUAGE,
    NONE; // Default/unselected state

    // Helper function to convert from view ID to enum
    companion object {
        fun fromViewId(id: Int): DetectionMode {
            return when (id) {
                R.id.calorie_capture -> CALORIE_CAPTURE
                R.id.sign_language -> SIGN_LANAGUAGE
                else -> NONE
            }
        }

        // Helper function to convert from enum to view ID
        fun toViewId(option: DetectionMode): Int {
            return when (option) {
                CALORIE_CAPTURE -> R.id.calorie_capture
                SIGN_LANAGUAGE -> R.id.sign_language
                NONE -> -1
            }
        }


        fun fromStringLabel(context: Context, label: String): DetectionMode {
            return when (label) {
                context.getString(R.string.calorie_capture_title) -> CALORIE_CAPTURE
                context.getString(R.string.sign_language_title) -> SIGN_LANAGUAGE
                else -> NONE
            }
        }

    }
    @StringRes
    fun toStringRes(): Int {
        return when (this) {
            CALORIE_CAPTURE -> R.string.calorie_capture_title
            SIGN_LANAGUAGE -> R.string.sign_language_title
            else -> 0
        }
    }

    // New helper function to get the resolved string
    fun toStringValue(context: Context): String {
        return if (toStringRes() != 0) {
            context.getString(toStringRes())
        } else {
            "No mode selected"
        }
    }
}