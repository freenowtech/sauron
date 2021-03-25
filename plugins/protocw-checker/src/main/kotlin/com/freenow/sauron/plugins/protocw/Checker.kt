package com.freenow.sauron.plugins.protocw

import arrow.core.Either
import java.util.Optional

interface Checker {
    fun apply(
        repoPath: Optional<String>,
        protocwFileName: Optional<Any>,
        protocwPropertiesFileName: Optional<Any>
    ): Either<String, SemVer?>
}
