package dev.knsalvaterra.kezir

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class ResultBottomSheet(
    private val name: String,
    private val success: Boolean,
    private val message: String,
    private val onDismissed: () -> Unit = {}
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Ensure this layout file exists in res/layout/
        return inflater.inflate(R.layout.layout_result_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.sheetHeaderBar)
        val statusText = view.findViewById<TextView>(R.id.sheetStatusText)
        val buyerName = view.findViewById<TextView>(R.id.sheetBuyerName)
        val btn = view.findViewById<MaterialButton>(R.id.sheetCloseButton)

        buyerName.text = name

        if (success) {
            header.setBackgroundColor(Color.parseColor("#31815C")) // Your Green
            statusText.text = "VERIFICADO"
            btn.setBackgroundColor(Color.parseColor("#31815C"))
        } else {
            header.setBackgroundColor(Color.parseColor("#E53935")) // Red
            statusText.text = "BILHETE INVÁLIDO"
            buyerName.text = message
            btn.setBackgroundColor(Color.parseColor("#E53935"))
        }

        btn.setOnClickListener {
            dismiss()
            onDismissed()
        }
    }

    // Required for the rounded corners at the top
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onDetach() {
        super.onDetach()
        onDismissed()
    }
}