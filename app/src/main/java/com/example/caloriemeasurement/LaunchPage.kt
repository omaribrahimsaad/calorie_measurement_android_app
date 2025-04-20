package com.example.caloriemeasurement

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.caloriemeasurement.databinding.LaunchPageBinding
import com.example.caloriemeasurement.DetectionMode
import com.example.caloriemeasurement.SharedRadioButtonViewModel

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LaunchPage : Fragment() {


    // Get the shared ViewModel instance (scoped to Activity or NavGraph)
    private val sharedViewModel: SharedRadioButtonViewModel by activityViewModels()
    // Store the selected option as enum
    private var selectedOption: DetectionMode = DetectionMode.NONE
    private var _binding: LaunchPageBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = LaunchPageBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.goToCamerView.setOnClickListener{
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.settingsButton.setOnClickListener{
            showRadioMenuPopup(it)
        }

    }

    private fun showRadioMenuPopup(anchorView: View) {
        // Inflate the popup layout
        val popupView = layoutInflater.inflate(R.layout.fab_radio_menu, null)

        // Create the popup window
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // Focusable
        )

        // Find views in popup
        val radioGroup = popupView.findViewById<RadioGroup>(R.id.radioGroup)
        val closeButton = popupView.findViewById<Button>(R.id.btnClose)

        // Set previous selection if available
        if (selectedOption != DetectionMode.NONE) {
            val viewId = DetectionMode.toViewId(selectedOption)
            if (viewId != -1) {
                val radioButton = radioGroup.findViewById<RadioButton>(viewId)
                radioButton?.isChecked = true
            }
        }

        // Add listener for radio button changes
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            // Convert the checked ID to enum and save it
            selectedOption = DetectionMode.fromViewId(checkedId)

            // Get the text of selected radio button
            val radioButton = group.findViewById<RadioButton>(checkedId)
            val selectedText = radioButton?.text.toString()


            // ✅ Send update to shared ViewModel
            sharedViewModel.updateDetectionMode(DetectionMode.fromStringLabel(requireContext(),selectedText))


            // Handle the option selection
            handleOptionSelected(selectedOption, selectedText)
        }

        // Set close button click listener
        closeButton.setOnClickListener {
            popupWindow.dismiss()
        }

        // Show the popup window above the FAB
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }

    private fun handleOptionSelected(option: DetectionMode, optionText: String) {
        // Handle the option selection using enum
        when (option) {
            DetectionMode.CALORIE_CAPTURE -> {
//                Toast.makeText(context, "Selected: $optionText", Toast.LENGTH_SHORT).show()
                // Do something with this selection
            }
            DetectionMode.SIGN_LANAGUAGE -> {
//                Toast.makeText(context, "Selected: $optionText", Toast.LENGTH_SHORT).show()
                // Do something with this selection
            }
            DetectionMode.NONE -> {
                // No option selected
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}