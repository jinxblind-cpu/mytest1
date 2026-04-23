package com.hello.me

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private var lastNumeric: Boolean = false
    private var stateError: Boolean = false
    private var lastDot: Boolean = false

    private var firstOperand: Double = 0.0
    private var currentOperator: String? = null
    private var isNewOp: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvResult = findViewById(R.id.tvResult)
    }

    fun onDigit(view: View) {
        if (stateError) {
            tvResult.text = (view as Button).text
            stateError = false
        } else {
            if (isNewOp) {
                tvResult.text = (view as Button).text
            } else {
                tvResult.append((view as Button).text)
            }
        }
        lastNumeric = true
        isNewOp = false
    }

    fun onDecimalPoint(view: View) {
        if (lastNumeric && !stateError && !lastDot) {
            tvResult.append(".")
            lastNumeric = false
            lastDot = true
        }
    }

    fun onOperator(view: View) {
        if (lastNumeric && !stateError) {
            val currentVal = tvResult.text.toString().toDoubleOrNull() ?: 0.0
            if (currentOperator != null) {
                firstOperand = calculate(firstOperand, currentVal, currentOperator!!)
                tvResult.text = formatResult(firstOperand)
            } else {
                firstOperand = currentVal
            }
            currentOperator = (view as Button).text.toString()
            isNewOp = true
            lastDot = false
            lastNumeric = false
        }
    }

    fun onClear(view: View) {
        this.tvResult.text = "0"
        lastNumeric = false
        stateError = false
        lastDot = false
        firstOperand = 0.0
        currentOperator = null
        isNewOp = true
    }

    fun onEqual(view: View) {
        if (lastNumeric && !stateError && currentOperator != null) {
            val secondOperand = tvResult.text.toString().toDoubleOrNull() ?: 0.0
            val result = calculate(firstOperand, secondOperand, currentOperator!!)
            tvResult.text = formatResult(result)
            firstOperand = result
            currentOperator = null
            isNewOp = true
            lastDot = tvResult.text.contains(".")
        }
    }

    private fun calculate(op1: Double, op2: Double, operator: String): Double {
        return when (operator) {
            "+" -> op1 + op2
            "-" -> op1 - op2
            "*" -> op1 * op2
            "/" -> if (op2 != 0.0) op1 / op2 else {
                stateError = true
                0.0
            }
            else -> op2
        }
    }

    private fun formatResult(result: Double): String {
        return if (stateError) {
            "Error"
        } else {
            val df = DecimalFormat("#.##########")
            df.format(result)
        }
    }
}