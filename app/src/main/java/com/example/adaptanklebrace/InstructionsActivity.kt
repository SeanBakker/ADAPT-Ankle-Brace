package com.example.adaptanklebrace

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

class InstructionsActivity : BaseActivity() {

    private lateinit var quickLinksLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        // Initialize variables
        quickLinksLayout = findViewById(R.id.quick_links_layout)
    }

    // This method is triggered when a quick link button is clicked
    fun onQuickLinkClick(view: View) {
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val targetView: View = when (view.id) {
            R.id.section_1 -> scrollView.findViewById(R.id.section_intro)
            R.id.section_2 -> scrollView.findViewById(R.id.section_setup)
            R.id.section_3 -> scrollView.findViewById(R.id.section_tension)
            R.id.section_4 -> scrollView.findViewById(R.id.section_exercises)
            R.id.section_5 -> scrollView.findViewById(R.id.section_gait)
            R.id.section_6 -> scrollView.findViewById(R.id.section_rom)
            else -> scrollView.findViewById(R.id.section_rom)
        }
        targetView.let {
            scrollView.smoothScrollTo(0, it.top)
        }
    }
}
