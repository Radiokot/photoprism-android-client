package ua.com.radiokot.photoprism.di

import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.util.KeyChainClientCertificateKeyManager
import ua.com.radiokot.photoprism.api.util.SessionAwarenessInterceptor
import ua.com.radiokot.photoprism.api.util.SynchronizedSessionRenewalInterceptor
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.logic.PhotoPrismSessionCreator
import ua.com.radiokot.photoprism.env.logic.SessionCreator
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.util.CacheConstraints
import java.io.File

class EnvHttpClientParams(
    val sessionAwareness: SessionAwareness?,
    val clientCertificateAlias: String?,
    val withLogging: Boolean = true,
    val cache: Cache? = null,
) : SelfParameterHolder() {
    class SessionAwareness(
        val sessionIdProvider: () -> String,
        val renewal: Renewal?,
    ) {
        class Renewal(
            val authProvider: () -> EnvAuth,
            val sessionCreator: SessionCreator,
            val onSessionRenewed: ((newSession: EnvSession) -> Unit)?,
        )
    }
}

val envModule = module {
    includes(ioModules)

    // The factory must have a qualifier,
    // otherwise it is overridden by the scoped.
    factory(_q<EnvHttpClientParams>()) { envParams ->
        envParams as EnvHttpClientParams
        val builder = get<OkHttpClient.Builder>()

        if (envParams.sessionAwareness != null) {
            val sessionAwareness = envParams.sessionAwareness

            if (sessionAwareness.renewal != null) {
                val renewal = sessionAwareness.renewal

                builder.addInterceptor(
                    SynchronizedSessionRenewalInterceptor(
                        authProvider = renewal.authProvider,
                        onSessionRenewed = renewal.onSessionRenewed,
                        sessionCreator = renewal.sessionCreator,
                    )
                )
            }

            builder.addInterceptor(
                SessionAwarenessInterceptor(
                    sessionIdProvider = sessionAwareness.sessionIdProvider,
                    sessionIdHeaderName = "X-Session-ID"
                )
            )
        }

        if (envParams.clientCertificateAlias != null) {
            val clientCertKeyManager = KeyChainClientCertificateKeyManager(
                context = get(),
                alias = envParams.clientCertificateAlias,
            )
            val platformTrustManager = Platform.get().platformTrustManager()
            val sslContext = Platform.get().newSSLContext()
            sslContext.init(
                arrayOf(clientCertKeyManager),
                arrayOf(platformTrustManager),
                null,
            )
            builder.sslSocketFactory(sslContext.socketFactory, platformTrustManager)
        }

        if (envParams.withLogging) {
            builder.addInterceptor(get<HttpLoggingInterceptor>())
        }

        if (envParams.cache != null) {
            builder.cache(envParams.cache)
        }

        builder
            .build()
    } bind HttpClient::class

    single {
        SessionCreator.Factory { envConnectionParams: EnvConnectionParams ->
            PhotoPrismSessionCreator(
                sessionService = get(_q<EnvPhotoPrismSessionServiceParams>()) {
                    EnvPhotoPrismSessionServiceParams(
                        envConnectionParams = envConnectionParams,
                    )
                },
                envConnectionParams = envConnectionParams,
                jsonObjectMapper = get(),
            )
        }
    } bind SessionCreator.Factory::class

    scope<EnvSession> {
        scoped {
            val session = get<EnvSession>()
            val authPersistence = getOrNull<ObjectPersistence<EnvAuth>>(_q<EnvAuth>())
            val sessionPersistence = getOrNull<ObjectPersistence<EnvSession>>(_q<EnvSession>())

            val renewal: EnvHttpClientParams.SessionAwareness.Renewal? =
                if (authPersistence != null && authPersistence.hasItem())
                    EnvHttpClientParams.SessionAwareness.Renewal(
                        authProvider = {
                            authPersistence.loadItem().checkNotNull {
                                "There must be an auth data in order to renew the session"
                            }
                        },
                        sessionCreator = get<SessionCreator.Factory>().get(
                            envConnectionParams = session.envConnectionParams,
                        ),
                        onSessionRenewed = { newSession ->
                            session.id = newSession.id
                            session.downloadToken = newSession.downloadToken
                            session.previewToken = newSession.previewToken
                            sessionPersistence?.saveItem(session)
                        }
                    )
                else
                    null

            get<HttpClient>(_q<EnvHttpClientParams>()) {
                EnvHttpClientParams(
                    sessionAwareness = EnvHttpClientParams.SessionAwareness(
                        sessionIdProvider = session::id,
                        renewal = renewal,
                    ),
                    clientCertificateAlias = session.envConnectionParams.clientCertificateAlias,
                )
            }
        } bind HttpClient::class

        scoped {
            val session = get<EnvSession>()

            val cacheDir: File = get(named(IMAGE_CACHE_DIRECTORY))
            val httpClient = get<HttpClient>(_q<EnvHttpClientParams>()) {
                EnvHttpClientParams(
                    sessionAwareness = null,
                    clientCertificateAlias = session.envConnectionParams.clientCertificateAlias,
                    withLogging = false,
                    cache = Cache(cacheDir, CacheConstraints.getOptimalSize(cacheDir))
                )
            }

            // Note: Cache only works properly if there are no redirects in the library URL.
            // For example, https://try.photoprism.app redirects to https://demo.photoprism.app
            // so calls are always sent to the server to get the redirect and only then
            // the cache candidate is obtained and returned.
            val cacheControl = CacheControl.Builder()
                // Assumption: PhotoPrism content identified by hash is immutable.
                .immutable()
                .build()

            Picasso.Builder(get())
                .downloader(OkHttp3Downloader { request ->
                    httpClient.newCall(
                        request.newBuilder()
                            .cacheControl(cacheControl)
                            .build()
                    )
                })
                .build()
        } bind Picasso::class
    }
}
