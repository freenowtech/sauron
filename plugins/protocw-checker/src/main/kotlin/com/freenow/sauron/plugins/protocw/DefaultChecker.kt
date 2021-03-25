package com.freenow.sauron.plugins.protocw

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.rightIfNotNull
import com.freenow.sauron.plugins.ProtocwChecker
import java.nio.file.Paths
import java.util.Optional

class DefaultChecker(
        private val service: Service,
        private val protocValidator: Validator
) : Checker {
    override fun apply(
        repoPath: Optional<String>,
        protocwFileName: Optional<Any>,
        protocwPropertiesFileName: Optional<Any>
    ): Either<String, SemVer?> {

        return Either.fx<String, SemVer?> {
            val repositoryPath = !repoPath.unWrap().rightIfNotNull { "${ProtocwChecker.INPUT_REPO_PATH} was not set" }
            val repository = Paths.get(repositoryPath)

            !protocValidator.check(repository).mapLeft { "Protocw not needed: $it" }

            val protocwFile = repository.resolve(
                protocwFileName.map { toString() }
                    .orElse("protocw")
            )
            val protocwPropertiesFile = repository.resolve(
                protocwPropertiesFileName.map { toString() }
                    .orElse("protocw.properties")
            )

            service.check(protocwFile, protocwPropertiesFile)
        }
    }

}

private fun <T> Optional<T>.unWrap(): T? {
    return orElse(null)
}
