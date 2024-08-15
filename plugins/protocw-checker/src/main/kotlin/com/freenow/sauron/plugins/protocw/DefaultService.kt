package com.freenow.sauron.plugins.protocw

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class DefaultService : Service {
    override fun check(protocw: Path, protocwProperties: Path): SemVer? {
        if (!Files.exists(protocw)) {
            return null
        }

        return try {
            Files.newBufferedReader(protocwProperties)
                .lines()
                .map { it.split("=")[1] }
                .filter { it != null }
                .findFirst()
                .orElse(null)
        } catch (e: IOException) {
            return null
        } catch (e: IndexOutOfBoundsException) {
            return "malformed_version"
        }
    }
}