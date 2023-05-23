package com.example.mud

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.generated.Uint32
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.Provider
import java.security.Security


const val CONTRACT_ADDRESS = "0x5FbDB2315678afecb367f032d93F642f64180aa3"

class MainActivity : AppCompatActivity() {


    lateinit var web3j: Web3j
    lateinit var logTextView: TextView
    lateinit var spawn: Button
    lateinit var move: Button
    lateinit var leftMove:Button
    var currentX = 10L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBouncyCastle()


        web3j = Web3j.build(HttpService("http://10.0.2.2:8545"))

        move = findViewById<Button>(R.id.button2)
        move.setOnClickListener {
            move(true)
        }

        leftMove = findViewById(R.id.button3)
        leftMove.setOnClickListener {
            move(false)
        }

        spawn = findViewById<Button>(R.id.button)
        spawn.setOnClickListener {
            spawn()
        }

        logTextView = findViewById(R.id.log)

    }

    override fun onDestroy() {
        super.onDestroy()
        web3j.shutdown()
    }

    fun spawn(){

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("Babak" ,web3j.ethChainId().sendAsync().get().chainId.toString())

            val function: org.web3j.abi.datatypes.Function = org.web3j.abi.datatypes.Function(
                "spawn",
                listOf(Uint32(currentX), Uint32(14)), listOf())

            val encodedFunction = FunctionEncoder.encode(function)


            val credentials = Credentials.create("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80")

            val nounce = web3j.ethGetTransactionCount(credentials.address , DefaultBlockParameterName.LATEST).sendAsync().get()
            val gasPrice = web3j.ethGasPrice().sendAsync().get()

            val rawTransaction = RawTransaction.createTransaction(nounce.transactionCount,
                gasPrice.gasPrice, BigInteger("20000000"), CONTRACT_ADDRESS, encodedFunction
            )
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val hexValue = Numeric.toHexString(signedMessage)

            val response = web3j.ethSendRawTransaction(hexValue).sendAsync().get()

            val sBuilder = StringBuilder()

            sBuilder.append("Player 2 Spawned successfully \n")

            if (response.transactionHash != null){
                sBuilder.append("Transaction Hash: ${response.transactionHash}\n")
                sBuilder.append("Second Player Address: ${credentials.address}\n")
            }
            if (response.error != null){
                sBuilder.append("Error:")
                sBuilder.append(response.error.message)
            }

            runOnUiThread {
                logTextView.text = sBuilder.toString()
                move.visibility = View.VISIBLE
                leftMove.visibility = View.VISIBLE
            }



            Log.d("Babak2" , sBuilder.toString())

        }

    }

    fun move(isRight: Boolean) {

        CoroutineScope(Dispatchers.IO).launch {

            if (isRight) {
                currentX++
            }
            else{
                currentX--
            }

            val function: org.web3j.abi.datatypes.Function = org.web3j.abi.datatypes.Function(
                "move",
                listOf(Uint32(currentX), Uint32(14)), listOf())

            val encodedFunction = FunctionEncoder.encode(function)

            val credentials = Credentials.create("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80")

            val nounce = web3j.ethGetTransactionCount(credentials.address , DefaultBlockParameterName.LATEST).sendAsync().get()
            val gasPrice = web3j.ethGasPrice().sendAsync().get()
            val rawTransaction = RawTransaction.createTransaction(nounce.transactionCount,
                gasPrice.gasPrice, BigInteger("20000000"), CONTRACT_ADDRESS, encodedFunction
            )
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val hexValue = Numeric.toHexString(signedMessage)
            val response = web3j.ethSendRawTransaction(hexValue).sendAsync().get()

            runOnUiThread{
                val sBuilder = StringBuilder()
                sBuilder.append(logTextView.text)

                if (response.transactionHash != null){
                    sBuilder.append("Emoji moved right successfully\n")
                    sBuilder.append("Transaction hash: ${response.transactionHash}\n")
                }
                if (response.error != null){
                    sBuilder.append("Error:")
                    sBuilder.append(response.error.message)
                }
                logTextView.text = sBuilder.toString()
            }

        }

    }

    private fun setupBouncyCastle() {
        val provider: Provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
            ?: // Web3j will set up the provider lazily when it's first used.
            return
        if (provider.javaClass.equals(BouncyCastleProvider::class.java)) {
            // BC with same package name, shouldn't happen in real life.
            return
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

}