package ua.com.radiokot.photoprism.api.util

import android.content.Context
import android.security.KeyChain
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.X509KeyManager

/**
 * A key manager that provides private key and certificate chain of given [alias]
 * from the system [KeyChain] when a mutual TLS auth is requested.
 */
class KeyChainClientCertificateKeyManager(
    private val context: Context,
    private val alias: String,
) : X509KeyManager {
    override fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?
    ): String = alias

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
        if (alias == this.alias)
            KeyChain.getCertificateChain(context, alias)
        else
            null

    override fun getPrivateKey(alias: String?): PrivateKey? =
        if (alias == this.alias)
            KeyChain.getPrivateKey(context, alias)
        else
            null


    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        throw UnsupportedOperationException("getClientAliases")
    }

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        throw UnsupportedOperationException("getServerAliases")
    }

    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        socket: Socket?
    ): String {
        throw UnsupportedOperationException("chooseServerAlias")
    }
}