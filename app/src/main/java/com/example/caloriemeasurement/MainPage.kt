package com.example.caloriemeasurement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.caloriemeasurement.databinding.ActivityMainPageBinding

class MainPage : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageBinding

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }


}