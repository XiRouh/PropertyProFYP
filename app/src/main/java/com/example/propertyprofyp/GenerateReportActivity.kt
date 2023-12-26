package com.example.propertyprofyp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream

class GenerateReportActivity : AppCompatActivity() {

    private lateinit var spinnerReportType: Spinner
    private lateinit var graphContainer: FrameLayout
    private var currentChart: Chart<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_report)

        spinnerReportType = findViewById(R.id.spinnerReportType)
        graphContainer = findViewById(R.id.graphContainer)

        setupSpinner()
        findViewById<Button>(R.id.downloadButton).setOnClickListener {
            currentChart?.let { chart -> exportChartAsPDF(chart) }
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.report_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReportType.adapter = adapter

        spinnerReportType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                fetchDataFromFirebase(parent.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchDataFromFirebase(reportType: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("wishlist")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wishlists = snapshot.children.mapNotNull { it.getValue(Wishlist::class.java) }
                when (reportType) {
                    "Purchase Intention" -> generatePurchaseIntentionReport()
                    "Property Type" -> generatePropertyTypeReport(wishlists)
                    "Area" -> generateAreaReport(wishlists)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun generateAreaReport(wishlists: List<Wishlist>) {
        val areaData = wishlists.groupingBy { it.area }.eachCount()

        val entries = areaData.map { entry ->
            PieEntry(entry.value.toFloat(), entry.key)
        }
        val dataSet = PieDataSet(entries, "Area Report")
        val pieData = PieData(dataSet)
        val colors = listOf(Color.rgb(242, 148, 148), Color.rgb(242, 217, 148), Color.rgb(171, 200, 247), Color.rgb(179, 242, 148), Color.rgb(211, 148, 242), Color.rgb(240, 156, 205))
        dataSet.colors = colors

        val chart = PieChart(this).apply {
            data = pieData
            legend.textColor = Color.BLACK
            description.textColor = Color.BLACK

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        graphContainer.removeAllViews()
        graphContainer.addView(chart)
        currentChart = chart
    }

    private fun generatePropertyTypeReport(wishlists: List<Wishlist>) {
        val propertyTypeData = wishlists.groupingBy { it.propertyType }.eachCount()

        val entries = propertyTypeData.map { entry ->
            PieEntry(entry.value.toFloat(), entry.key)
        }
        val dataSet = PieDataSet(entries, "Property Type Report")
        val pieData = PieData(dataSet)
        val colors = listOf(Color.rgb(242, 148, 148), Color.rgb(242, 217, 148), Color.rgb(171, 200, 247), Color.rgb(179, 242, 148), Color.rgb(211, 148, 242), Color.rgb(240, 156, 205))
        dataSet.colors = colors

        val chart = PieChart(this).apply {
            data = pieData
            legend.textColor = Color.BLACK
            description.textColor = Color.BLACK

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        graphContainer.removeAllViews()
        graphContainer.addView(chart)
        currentChart = chart
    }

    private fun generatePurchaseIntentionReport() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("bookings")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val purchaseIntentionCount = snapshot.children.count {
                    it.child("status").getValue(String::class.java) == "Done - With Purchase Intention"
                }
                val noPurchaseIntentionCount = snapshot.children.count {
                    it.child("status").getValue(String::class.java) == "Done - Without Purchase Intention"
                }

                // Generate the Pie Chart
                val entries = listOf(
                    PieEntry(purchaseIntentionCount.toFloat(), "With Purchase Intention"),
                    PieEntry(noPurchaseIntentionCount.toFloat(), "Without Purchase Intention")
                )
                val dataSet = PieDataSet(entries, "Purchase Intention Report")
                val pieData = PieData(dataSet)
                val colors = listOf(Color.rgb(242, 148, 148), Color.rgb(242, 217, 148), Color.rgb(171, 200, 247), Color.rgb(179, 242, 148), Color.rgb(211, 148, 242), Color.rgb(240, 156, 205))
                dataSet.colors = colors

                val chart = PieChart(this@GenerateReportActivity).apply {
                    data = pieData
                    legend.textColor = Color.BLACK
                    description.textColor = Color.BLACK

                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }

                graphContainer.removeAllViews()
                graphContainer.addView(chart)
                currentChart = chart
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GenerateReportActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun exportChartAsPDF(chart: Chart<*>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(chart.width, chart.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val bitmap = Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
        val canvasBitmap = Canvas(bitmap)
        chart.draw(canvasBitmap)

        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (!storageDir?.exists()!!) {
                storageDir.mkdir()
            }
            val file = File(storageDir, "report.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            Toast.makeText(this, "PDF saved to ${file.path}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // Go back to the previous Activity
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
