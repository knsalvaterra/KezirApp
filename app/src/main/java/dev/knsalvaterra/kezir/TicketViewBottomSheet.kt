package dev.knsalvaterra.kezir

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.knsalvaterra.kezir.api.Order

class TicketViewBottomSheet(
    private var success: Boolean? = null,
    private var message: String? = null,
    private var order: Order? = null,
    private val onDismissed: () -> Unit = {}
) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_ticket_sheet, container, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSheetBehaviour()
        setupUI(view)
    }

    private fun setupSheetBehaviour() {
        (dialog as? BottomSheetDialog)?.findViewById<FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.let { bottomSheet ->
            BottomSheetBehavior.from(bottomSheet).apply {
                isHideable = false
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    fun updateWithResult(success: Boolean, message: String, order: Order?) {
        this.success = success
        this.message = message
        this.order = order
        view?.let { setupUI(it) }
    }

    private fun setupUI(view: View) {
        val header = view.findViewById<LinearLayout>(R.id.sheetHeaderBar)
        val statusIconCard = view.findViewById<MaterialCardView>(R.id.statusIconCard)
        val statusIcon = view.findViewById<ImageView>(R.id.statusIcon)
        val statusText = view.findViewById<TextView>(R.id.sheetStatusText)
        val statusSubtitle = view.findViewById<TextView>(R.id.sheetStatusSubtitle)
        val buyerNameLabel = view.findViewById<TextView>(R.id.buyerNameLabel)
        val buyerLabelIcon = view.findViewById<ImageView>(R.id.buyerLabelIcon)
        val buyerName = view.findViewById<TextView>(R.id.sheetBuyerName)
        val buyerPhoneLabel = view.findViewById<TextView>(R.id.buyerPhoneLabel)
        val buyerPhoneIcon = view.findViewById<ImageView>(R.id.buyerPhoneIcon)
        val buyerPhone = view.findViewById<TextView>(R.id.sheetBuyerPhone)
        val ticketsLabel = view.findViewById<TextView>(R.id.ticketsLabel)
        val ticketDetailsContainer = view.findViewById<LinearLayout>(R.id.ticketDetailsContainer)
        val infoMessageContainer = view.findViewById<LinearLayout>(R.id.infoMessageContainer)
        val infoIcon = view.findViewById<ImageView>(R.id.infoIcon)
        val sheetMessage = view.findViewById<TextView>(R.id.sheetMessage)
        val actionButton = view.findViewById<MaterialButton>(R.id.sheetCloseButton)

        when (success) {
            true -> setupValidState(header, statusIconCard, statusIcon, statusText, statusSubtitle, buyerLabelIcon, buyerName, buyerNameLabel, buyerPhoneIcon, buyerPhone, buyerPhoneLabel, ticketsLabel, ticketDetailsContainer, infoMessageContainer, infoIcon, sheetMessage, actionButton)
            false -> setupInvalidState(header, statusIconCard, statusIcon, statusText, statusSubtitle, buyerLabelIcon, buyerName, buyerNameLabel, buyerPhoneIcon, buyerPhone, buyerPhoneLabel, ticketsLabel, ticketDetailsContainer, infoMessageContainer, infoIcon, sheetMessage, actionButton)
            null -> {}
        }

        actionButton.setOnClickListener {
            dismiss()
        }
    }

    private fun censorPhoneNumber(phone: String): String {
        val numbersToKeep = 3
        if (phone.length <= numbersToKeep) {
            return "*".repeat(phone.length)
        }
        val censoredLength = phone.length - numbersToKeep
        val lastFourDigits = phone.substring(censoredLength)
        val censoredPart = phone.substring(0, censoredLength).map {
            if (it.isDigit()) '*' else it
        }.joinToString("")
        return censoredPart + lastFourDigits
    }

    private fun setupValidState(
        header: LinearLayout,
        statusIconCard: MaterialCardView,
        statusIcon: ImageView,
        statusText: TextView,
        statusSubtitle: TextView,
        buyerLabelIcon: ImageView,
        buyerName: TextView,
        buyerNameLabel: TextView,
        buyerPhoneIcon: ImageView,
        buyerPhone: TextView,
        buyerPhoneLabel: TextView,
        ticketsLabel: TextView,
        ticketDetailsContainer: LinearLayout,
        infoMessageContainer: LinearLayout,
        infoIcon: ImageView,
        sheetMessage: TextView,
        actionButton: MaterialButton
    ) {
        header.setBackgroundResource(R.drawable.header_background_valid)

        statusIcon.setImageResource(R.drawable.ic_check_circle)
        statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green_600))

        statusText.text = getString(R.string.status_ticket_valid)
        statusSubtitle.text = getString(R.string.status_subtitle_success)

        buyerNameLabel.visibility = View.VISIBLE
        buyerLabelIcon.visibility = View.VISIBLE
        buyerName.visibility = View.VISIBLE
        buyerPhoneLabel.visibility = View.VISIBLE
        buyerPhoneIcon.visibility = View.VISIBLE
        buyerPhone.visibility = View.VISIBLE
        ticketsLabel.visibility = View.VISIBLE
        ticketDetailsContainer.visibility = View.VISIBLE
        
        order?.let {
            buyerName.text = it.buyer_name
            buyerPhone.text = censorPhoneNumber(it.buyer_phone)
        } ?: run {
            buyerName.text = getString(R.string.not_available)
            buyerPhone.text = getString(R.string.not_available)
        }

        ticketDetailsContainer.removeAllViews()
        order?.tickets?.forEach { ticket ->
            val inflater = LayoutInflater.from(requireContext())
            val detailItem = inflater.inflate(R.layout.ticket_detail_item, ticketDetailsContainer, false)

            val ticketName = detailItem.findViewById<TextView>(R.id.ticket_name)
            val ticketQuantity = detailItem.findViewById<TextView>(R.id.ticket_quantity)

            ticketName.text = ticket.ticket_name
            ticketQuantity.text = "x${ticket.quantity}"

            ticketDetailsContainer.addView(detailItem)
        }

        if (message?.isNotBlank() == true) {
            infoMessageContainer.visibility = View.VISIBLE
            infoMessageContainer.setBackgroundResource(R.drawable.info_message_background_valid)

            infoIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.chill_green))
            sheetMessage.text = message
        } else {
            infoMessageContainer.visibility = View.GONE
        }

        actionButton.text = getString(R.string.button_label_continue)
    }

    private fun setupInvalidState(
        header: LinearLayout,
        statusIconCard: MaterialCardView,
        statusIcon: ImageView,
        statusText: TextView,
        statusSubtitle: TextView,
        buyerLabelIcon: ImageView,
        buyerName: TextView,
        buyerNameLabel: TextView,
        buyerPhoneIcon: ImageView,
        buyerPhone: TextView,
        buyerPhoneLabel: TextView,
        ticketsLabel: TextView,
        ticketDetailsContainer: LinearLayout,
        infoMessageContainer: LinearLayout,
        infoIcon: ImageView,
        sheetMessage: TextView,
        actionButton: MaterialButton
    ) {
        header.setBackgroundResource(R.drawable.header_background_invalid)

        statusIcon.setImageResource(R.drawable.ic_error_circle)
        statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red_600))

        statusText.text = getString(R.string.status_ticket_invalid)
        statusSubtitle.text = getString(R.string.status_subtitle_failure)

        statusSubtitle.visibility = View.GONE

        if (order != null) {
            buyerNameLabel.visibility = View.VISIBLE
            buyerLabelIcon.visibility = View.VISIBLE
            buyerName.visibility = View.VISIBLE
            buyerName.text = order?.buyer_name

            buyerPhoneLabel.visibility = View.VISIBLE
            buyerPhoneIcon.visibility = View.VISIBLE
            buyerPhone.visibility = View.VISIBLE
            buyerPhone.text = censorPhoneNumber(order?.buyer_phone ?: "")

            ticketsLabel.visibility = View.VISIBLE
            ticketDetailsContainer.visibility = View.GONE
            ticketDetailsContainer.removeAllViews()
            order?.tickets?.forEach { ticket ->
                val inflater = LayoutInflater.from(requireContext())
                val detailItem = inflater.inflate(R.layout.ticket_detail_item, ticketDetailsContainer, false)
                detailItem.findViewById<TextView>(R.id.ticket_name).text = ticket.ticket_name
                detailItem.findViewById<TextView>(R.id.ticket_quantity).text = "x${ticket.quantity}"
                ticketDetailsContainer.addView(detailItem)
            }
        } else {
            buyerNameLabel.visibility = View.VISIBLE
            buyerLabelIcon.visibility = View.VISIBLE
            buyerName.visibility = View.VISIBLE
            buyerName.text = getString(R.string.not_available)

            buyerPhoneLabel.visibility = View.GONE
            buyerPhoneIcon.visibility = View.GONE
            buyerPhone.visibility = View.GONE

            ticketsLabel.visibility = View.GONE
            ticketDetailsContainer.visibility = View.GONE
        }

        infoMessageContainer.visibility = View.VISIBLE
        infoMessageContainer.setBackgroundResource(R.drawable.info_message_background_invalid)
        infoIcon.setImageResource(R.drawable.ic_error_circle)
        infoIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red_600))
        message?.let { sheetMessage.text = it }

        actionButton.text = getString(R.string.try_again_button)
        actionButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red_600))
        actionButton.icon = null
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onDetach() {
        super.onDetach()
        onDismissed()
    }
}
