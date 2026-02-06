package dev.knsalvaterra.kezir

import android.app.Dialog
import android.graphics.Typeface
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
//pop out anim
class TicketViewBottomSheet(
    private val success: Boolean,
    private val message: String,
    private val order: Order?,
    private val onDismissed: () -> Unit = {}
) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false //no touch outside
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
        return inflater.inflate(R.layout.layout_ticket_sheet, container, false)
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



    private fun setupUI(view: View) {


        val header = view.findViewById<LinearLayout>(R.id.sheetHeaderBar)
        val statusIconCard = view.findViewById<MaterialCardView>(R.id.statusIconCard)
        val statusIcon = view.findViewById<ImageView>(R.id.statusIcon)
        val statusText = view.findViewById<TextView>(R.id.sheetStatusText)
        val statusSubtitle = view.findViewById<TextView>(R.id.sheetStatusSubtitle)
        val buyerNameLabel = view.findViewById<TextView>(R.id.buyerNameLabel)
        val buyerLabelIcon = view.findViewById<ImageView>(R.id.buyerLabelIcon)
        val buyerName = view.findViewById<TextView>(R.id.sheetBuyerName)
        val ticketsLabel = view.findViewById<TextView>(R.id.ticketsLabel)
        val ticketDetailsContainer = view.findViewById<LinearLayout>(R.id.ticketDetailsContainer)
        val infoMessageContainer = view.findViewById<LinearLayout>(R.id.infoMessageContainer)
        val infoIcon = view.findViewById<ImageView>(R.id.infoIcon)
        val sheetMessage = view.findViewById<TextView>(R.id.sheetMessage)
        val actionButton = view.findViewById<MaterialButton>(R.id.sheetCloseButton)
        statusSubtitle.visibility = View.GONE


        if (success && order != null) {

            //todo, combine into a single method
            setupValidState(
                header,
                statusIconCard,
                statusIcon,
                statusText,
                statusSubtitle,
                buyerLabelIcon,
                buyerName,
                buyerNameLabel,
                ticketDetailsContainer,
                infoMessageContainer,
                infoIcon,
                sheetMessage,
                actionButton
            )
        } else {
            setupInvalidState(
                header,
                statusIconCard,
                statusIcon,
                statusText,
                statusSubtitle,
                buyerLabelIcon,
                buyerName,
                buyerNameLabel,
                ticketsLabel,
                ticketDetailsContainer,
                infoMessageContainer,
                infoIcon,
                sheetMessage,
                actionButton
            )
        }

        actionButton.setOnClickListener {
            dismiss()
        }
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
        ticketDetailsContainer: LinearLayout,
        infoMessageContainer: LinearLayout,
        infoIcon: ImageView,
        sheetMessage: TextView,
        actionButton: MaterialButton
    ) {

        header.setBackgroundResource(R.drawable.header_background_valid)

        // status icon
        statusIcon.setImageResource(R.drawable.ic_check_circle)
        statusIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.green_600)
        )
        // status text
        statusText.text = getString(R.string.status_ticket_valid)
        statusSubtitle.text = "Verificado com sucesso"



        // client info
        buyerNameLabel.setTextColor( ContextCompat.getColor(requireContext(), R.color.green_600))
        buyerLabelIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.green_600)

        )
        buyerName.text = order?.buyer_name ?: "N/A"

        // ticket details
        ticketDetailsContainer.removeAllViews()
        order?.tickets?.forEach { ticket ->
            val detailItem = createTicketDetailItem(ticket.ticket_name, "x${ticket.quantity}")
            detailItem.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            ticketDetailsContainer.addView(detailItem)
        }

        // ifo message
        if (message.isNotBlank()) {
            infoMessageContainer.visibility = View.VISIBLE
            infoMessageContainer.setBackgroundResource(R.drawable.info_message_background_valid)

            infoIcon.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.chill_green)
            )
            sheetMessage.text = message
        } else {
            infoMessageContainer.visibility = View.GONE
        }


        actionButton.text = getString(R.string.button_label_continue)

        actionButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.chill_green)
        )
      //  actionButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_forward)
      //  actionButton.iconGravity = MaterialButton.ICON_GRAVITY_END
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
        ticketsLabel: TextView,
        ticketDetailsContainer: LinearLayout,
        infoMessageContainer: LinearLayout,
        infoIcon: ImageView,
        sheetMessage: TextView,
        actionButton: MaterialButton
    ) {


        header.setBackgroundResource(R.drawable.header_background_invalid)


        statusIcon.setImageResource(R.drawable.ic_error_circle)
        statusIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.red_600)
        )

        // status text
        statusText.text = getString(R.string.status_ticket_invalid)
        statusSubtitle.text = "Não foi possível verificar"

        // client info
        buyerNameLabel.setTextColor( ContextCompat.getColor(requireContext(), R.color.red_600))
        buyerLabelIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.red_600)
        )
        buyerName.text = "Erro na Validação"

        //  ticket details
        ticketsLabel.visibility = View.GONE
        ticketDetailsContainer.visibility = View.GONE

        // error message
        infoMessageContainer.visibility = View.VISIBLE
        infoMessageContainer.setBackgroundResource(R.drawable.info_message_background_invalid)
        infoIcon.setImageResource(R.drawable.ic_error_circle)
        infoIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.red_600)
        )
        sheetMessage.text = message

        // button
        actionButton.text = "TENTAR NOVAMENTE"
        actionButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.red_600)
        )
        actionButton.icon = null
    }

    //todo, make a layot file instead
    private fun createTicketDetailItem(label: String, value: String): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT

            ).apply {
                if (parent is LinearLayout && (parent as LinearLayout).childCount > 0) {
                    topMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
                }
            }

            // ticket name
            addView(TextView(context).apply {
                text = label
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
                setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))

                textSize = 18f
            })

            // ticket amount
            addView(TextView(context).apply {
                text = value
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setTextColor(ContextCompat.getColor(context, R.color.on_surface))

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                textSize = 16.5f

                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 42)

            })
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onDetach() {
        super.onDetach()
        onDismissed()
    }
}