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
    private val success: Boolean,
    private val message: String,
    private val order: Order?,
    private val onDismissed: () -> Unit = {}
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_result_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.sheetHeaderBar)
        val statusText = view.findViewById<TextView>(R.id.sheetStatusText)
        val buyerName = view.findViewById<TextView>(R.id.sheetBuyerName)
        val ticketDetails = view.findViewById<TextView>(R.id.sheetTicketDetails)
        val btn = view.findViewById<MaterialButton>(R.id.sheetCloseButton)

        if (success && order != null) {
            header.setBackgroundColor(Color.parseColor("#31815C")) // Green
            statusText.text = "VERIFICADO"
         //   btn.setBackgroundColor(Color.parseColor("#31815C"))

            buyerName.text = order.buyer_name

            // Combine ticket details into a single string
            val ticketsStr = order.tickets.joinToString("\n") {
                "${it.ticket_name} • Qnt: ${it.quantity}"
            }
            ticketDetails.text = ticketsStr

        } else {
            header.setBackgroundColor(Color.parseColor("#E53935")) // Red
            statusText.text = "BILHETE INVÁLIDO"
         //   btn.setBackgroundColor(Color.parseColor("#E53935"))

            buyerName.text = message
            ticketDetails.text = ""
        }

        btn.setOnClickListener {
            dismiss()
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onDetach() {
        super.onDetach()
        onDismissed()
    }
}