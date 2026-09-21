package com.example.wifianalyzeriot.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.wifianalyzeriot.model.WifiNetworkInfo

class ChannelGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private var channelCounts = mutableMapOf<Int, Int>()

    fun updateData(networks: List<WifiNetworkInfo>) {
        // Filtra apenas 2.4 GHz (canais 1 a 14)
        channelCounts = networks
            .filter { it.band == "2.4 GHz" && it.channel in 1..14 }
            .groupBy { it.channel }
            .mapValues { it.value.size }
            .toMutableMap()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (channelCounts.isEmpty()) {
            canvas.drawText("Nenhuma rede 2.4 GHz", width / 2f, height / 2f, textPaint)
            return
        }

        val maxCount = (channelCounts.values.maxOrNull() ?: 1).toFloat()
        val barWidth = width / 15f   // espaço para canais 1..14
        val bottom = height - 50f

        for (channel in 1..14) {
            val count = channelCounts[channel] ?: 0
            val barHeight = (count / maxCount) * (height - 80)
            val left = (channel - 1) * barWidth
            val right = left + barWidth - 2
            canvas.drawRect(left, bottom - barHeight, right, bottom, barPaint)

            // Desenha o número do canal
            canvas.drawText(
                channel.toString(),
                left + barWidth / 2,
                bottom + 30,
                textPaint
            )
        }
    }
}