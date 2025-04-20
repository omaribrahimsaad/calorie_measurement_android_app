package com.example.caloriemeasurement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.caloriemeasurement.DetectionMode // Import your Enum

class SharedRadioButtonViewModel : ViewModel() {

    // Initialize with a default value. Since calorie_capture is checked by default
    // in your XML, let's use that.
    private val _selectedMode = MutableLiveData<DetectionMode>(DetectionMode.CALORIE_CAPTURE)

    // Expose immutable LiveData for observation
    val selectedMode: LiveData<DetectionMode> = _selectedMode

    // Function for LaunchPage to call when selection changes
    fun updateDetectionMode(mode: DetectionMode) {
        // Optional: Check if the value is actually changing to avoid unnecessary updates
        if (_selectedMode.value != mode) {
            _selectedMode.value = mode
        }
    }
}