package com.freenow.sauron.plugins.protocw

import arrow.core.Either
import java.nio.file.Path

interface Validator {
    fun check(repository: Path): Either<String, Unit>

}
