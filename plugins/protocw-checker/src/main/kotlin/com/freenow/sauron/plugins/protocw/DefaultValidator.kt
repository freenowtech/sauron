package com.freenow.sauron.plugins.protocw

import arrow.core.Either
import arrow.core.extensions.fx
import java.nio.file.Files
import java.nio.file.Path

class DefaultValidator : Validator {
    override fun check(repository: Path): Either<String, Unit> {

        return Either.fx {
            !checkRepositorExists(repository)
            !checkNeedsProtoc(repository)
            Unit
        }
    }

    private fun checkRepositorExists(repository: Path): Either<String, Unit> {
        return Either.cond(Files.isDirectory(repository),
            { Unit },
            { "Repository is not a directory" }
        )
    }

    private fun checkNeedsProtoc(repository: Path): Either<String, Unit> {
        return Either.fx {
            val pom = repository.resolve("pom.xml")
            val reader = !Either.cond(Files.exists(pom),
                { Files.newBufferedReader(pom) },
                { "POM does not exist" }
            )

            !Either.cond(reader.lines().anyMatch { it.contains("protobuf-maven-plugin") },
                { Unit },
                { "Has no protobuf-maven-plugin plugin" }
            )
        }
    }

}
