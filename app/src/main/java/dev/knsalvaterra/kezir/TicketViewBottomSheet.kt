package dev.knsalvaterra.kezir

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class TicketViewBottomSheet(
    private val success: Boolean,
    private val message: String,
    private val order: Order?,
    private val onDismissed: () -> Unit = {}
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_ticket_sheet, container, true)
    }



    override fun onStart() {
        super.onStart()
      //  val bottomSheet = dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
      //  if (bottomSheet != null) {
      //      val behavior = BottomSheetBehavior.from(bottomSheet)
      //      behavior.state = BottomSheetBehavior.STATE_EXPANDED
      //      behavior.skipCollapsed = true // Always open fully
      //  }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.sheetHeaderBar)
        val dragHandle = view.findViewById<BottomSheetDragHandleView>(R.id.drag_handle)

        val statusText = view.findViewById<TextView>(R.id.sheetStatusText)
        val statusIcon = view.findViewById<ImageView>(R.id.statusIcon)
        val buyerName = view.findViewById<TextView>(R.id.sheetBuyerName)
        val sheetMsg = view.findViewById<TextView>(R.id.sheetMessage)
        val ticketDetailsContainer = view.findViewById<LinearLayout>(R.id.ticketDetailsContainer)
        val btn = view.findViewById<MaterialButton>(R.id.sheetCloseButton)
        val ticketsLabel = view.findViewById<TextView>(R.id.ticketsLabel)

        if (success && order != null) {
            // Success Style
            val successColor = "#31815C".toColorInt()
            header.setBackgroundColor(successColor)
            dragHandle.backgroundTintList = ColorStateList.valueOf(successColor)


            statusText.text = getString(R.string.status_ticket_valid)
            statusIcon.setImageResource(R.drawable.ic_success)
            statusIcon.setColorFilter(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#39966B"))
            btn.text = "CONFIRMAR"

            buyerName.text = order.buyer_name
            sheetMsg.text = message // "Código verificado e marcado como resgatado!"

            ticketDetailsContainer.removeAllViews()
            order.tickets.forEach { ticket ->
                val ticketView = layoutInflater.inflate(R.layout.ticket_item, ticketDetailsContainer, false)
                ticketView.findViewById<TextView>(R.id.ticketName).text = ticket.ticket_name
                ticketView.findViewById<TextView>(R.id.ticketQuantity).text = "x${ticket.quantity}"

                ticketDetailsContainer.addView(ticketView)
            }
        } else {
            // Error Style
            val errorColor = Color.parseColor("#E53935")
            header.setBackgroundColor(errorColor)
            dragHandle.backgroundTintList = ColorStateList.valueOf(errorColor)
            statusText.text = getString(R.string.status_ticket_invalid)
            statusIcon.setImageResource(R.drawable.ic_error)
            statusIcon.setColorFilter(Color.WHITE)
            btn.setBackgroundColor(errorColor)
            btn.text = "TENTAR NOVAMENTE"

            buyerName.text = "Erro na Validação"
            sheetMsg.text = message
            ticketsLabel.visibility = View.GONE
            ticketDetailsContainer.visibility = View.GONE
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