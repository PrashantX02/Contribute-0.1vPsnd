package com.example.billbook

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billbook.Adapters.ProductsAdapter
import com.example.billbook.Adapters.SuggestionAdapter
import com.example.billbook.Model.ProductData
import com.example.billbook.Model.ProductModel
import com.example.billbook.Utils.FirestoreHelper
import com.example.billbook.databinding.ActivityNewInvoiceBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot

class NewInvoice : AppCompatActivity(), (ProductData) -> Unit {
    lateinit var binding: ActivityNewInvoiceBinding
    lateinit var productList: ArrayList<ProductData>
    lateinit var progressDialog: ProgressDialog
    var pName = ""

    private var paymentDegree = 0
    private var strobe = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewInvoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarNewInvoice)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        // Visibility of recent recycler View
        binding.purchasedItemsRV.visibility = View.GONE
        binding.viewRecent.setOnClickListener {
            if (strobe == 0) {
                binding.purchasedItemsRV.visibility = View.VISIBLE
                strobe = 1
            } else {
                binding.purchasedItemsRV.visibility = View.GONE
                strobe = 0
            }
        }

        // Payment method selection
        binding.gpay.setOnClickListener {
            binding.ppe.alpha = 0.3f
            binding.gpay.alpha = 1f
            binding.bpe.alpha = 0.3f
            paymentDegree = 1
        }

        binding.ppe.setOnClickListener {
            binding.ppe.alpha = 1f
            binding.gpay.alpha = 0.3f
            binding.bpe.alpha = 0.3f
            paymentDegree = 2
        }

        binding.bpe.setOnClickListener {
            binding.ppe.alpha = 0.3f
            binding.gpay.alpha = 0.3f
            binding.bpe.alpha = 1f
            paymentDegree = 3
        }

        binding.addInvoice.setOnClickListener {
            progressDialog.show()
            val name = binding.customerName.text.toString().trim()
            val price = binding.overallTv.text.toString()
            if (name.isNotEmpty() && price.isNotEmpty()) {
                uploadInvoice(name)
                val intent = Intent(this, payment::class.java).apply {
                    putExtra("name", name)
                    putExtra("price", price)
                    putExtra("degree", paymentDegree.toString())
                }
                startActivity(intent)
                progressDialog.dismiss()
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Enter Customer Name and Add Products", Toast.LENGTH_SHORT).show()
            }
        }

        productList = ArrayList()
        binding.purchasedItemsRV.setHasFixedSize(true)
        binding.purchasedItemsRV.layoutManager = LinearLayoutManager(this)

        binding.addProduct.setOnClickListener {
            progressDialog.show()
            productList.add(
                ProductData(
                    pName,
                    binding.MRP.text.toString(),
                    binding.sellingPrice.text.toString(),
                    binding.discountPercent.text.toString(),
                    binding.productStock.text.toString()
                )
            )
            binding.purchasedItemsRV.adapter = ProductsAdapter(this, productList)
            calculateOverallPrice()
            progressDialog.dismiss()
        }

        binding.suggestionRV.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.suggestionRV.setHasFixedSize(true)
        getProduct()

        binding.productEditText.doOnTextChanged { text, start, before, count ->
            if (start < count || start > count) {
                val tempList = ArrayList<ProductData>()
                MainActivity.productList.forEach {
                    if (it.productName.lowercase().contains(text.toString().lowercase())) {
                        tempList.add(it)
                    }
                }
                val adapter = SuggestionAdapter(this, tempList, this)
                binding.suggestionRV.adapter = adapter
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun uploadInvoice(custname: String) {
        val invoiceId = System.currentTimeMillis().toString()
        if (productList.isNullOrEmpty()) {
            Log.e("UploadInvoice", "Product list is empty. Cannot upload invoice.")
            return
        }
        val invoiceData = ProductModel(productList)
        try {
            FirestoreHelper.fireDatabase.collection("Shops")
                .document("Shop Name")
                .collection("Customers")
                .document(custname)
                .collection("Invoices")
                .document(invoiceId)
                .set(invoiceData)
                .addOnSuccessListener {
                    Log.d("UploadInvoice", "Invoice uploaded successfully: $invoiceId")
                }
                .addOnFailureListener { e ->
                    Log.e("UploadInvoice", "Failed to upload invoice: $invoiceId", e)
                }
        } catch (e: Exception) {
            Log.e("UploadInvoice", "Exception while uploading invoice: $invoiceId", e)
        }
    }

    override fun invoke(productData: ProductData) {
        binding.sellingPrice.setText(productData.sellingPrice)
        binding.MRP.setText(productData.productMrp)
        pName = productData.productName
        binding.discountPercent.setText(productData.productDiscount)
        binding.productStock.setText(productData.productStock)
    }

    private fun getProduct() {
        progressDialog.show()
        MainActivity.productList = ArrayList()
        FirestoreHelper.fireDatabase.collection("Shops")
            .document("Shop Name").collection("Product")
            .get()
            .addOnCompleteListener(object : OnCompleteListener<QuerySnapshot> {
                override fun onComplete(task: Task<QuerySnapshot>) {
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        for (document in task.result) {
                            val pName = document.getString("Product Name").toString()
                            val pMRPStr = document.getString("Product MRP") ?: ""
                            val pPriceStr = document.getString("Product Price") ?: ""
                            val pStockStr = document.getString("Product Stock") ?: ""

                            val pMRP = pMRPStr.toDoubleOrNull()
                            val pPrice = pPriceStr.toDoubleOrNull()
                            val pStock = pStockStr.toIntOrNull()

                            if (pMRP != null && pPrice != null && pStock != null) {
                                val pDiscount = ((pMRP - pPrice) / pMRP) * 100
                                MainActivity.productList.add(
                                    ProductData(
                                        productName = pName,
                                        productMrp = pMRPStr,
                                        sellingPrice = pPriceStr,
                                        productDiscount = pDiscount.toString(),
                                        productStock = pStockStr
                                    )
                                )
                                Log.d("ListItems", MainActivity.productList.toString())
                            } else {
                                Log.e(
                                    "MainActivity",
                                    "Invalid product data: $pName, MRP: $pMRPStr, Price: $pPriceStr, Stock: $pStockStr"
                                )
                            }
                        }
                        calculateOverallPrice()
                    } else {
                        Log.e("MainActivity", "Error getting documents: ", task.exception)
                    }
                }
            })
    }

    private fun calculateOverallPrice() {
        var overallPrice = productList.sumOf { it.productMrp.toDoubleOrNull() ?: 0.0 }
        binding.overallTv.text = " ₹$overallPrice"
    }
}
