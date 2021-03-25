package com.freenow.sauron.plugins.protocw

import java.nio.file.Path

typealias SemVer = String

interface Service {
    fun check(protocw: Path, protocwProperties: Path): SemVer?
}