package ufrn.br.poketlin

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

class ArchitectureTest {

    private val raiz = "ufrn.br.poketlin"

    private val proibidasNasCamadasInternas =
            listOf(
                    "io.ktor",
                    "org.jetbrains.exposed",
                    "org.flywaydb",
                    "org.postgresql",
                    "java.sql",
                    "javax.sql",
                    "io.grpc",
                    "com.google.protobuf",
                    "kotlinx.serialization",
            )

    @Test
    fun `camadas respeitam a regra de dependencia`() {
        Konsist.scopeFromProduction().assertArchitecture {
            val domain = Layer("Domain", "$raiz.domain..")
            val application = Layer("Application", "$raiz.application..")
            val infrastructure = Layer("Infrastructure", "$raiz.infrastructure..")
            val presentation = Layer("Presentation", "$raiz.presentation..")

            domain.dependsOnNothing()
            application.dependsOn(domain)
            infrastructure.dependsOn(domain, application)
            presentation.dependsOn(domain, application)
        }
    }

    @Test
    fun `dominio e aplicacao nao importam framework nem infraestrutura`() {
        val arquivos =
                Konsist.scopeFromProduction().files.filter { arquivo ->
                    val pacote = arquivo.packagee?.name.orEmpty()
                    pacote.startsWith("$raiz.domain") || pacote.startsWith("$raiz.application")
                }

        check(arquivos.isNotEmpty()) {
            "Nenhum arquivo em domain/application: o teste estaria vazio"
        }

        arquivos.assertTrue { arquivo ->
            arquivo.imports.none { import ->
                proibidasNasCamadasInternas.any { import.name.startsWith(it) }
            }
        }
    }
}
