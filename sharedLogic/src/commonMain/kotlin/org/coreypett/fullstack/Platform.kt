package org.coreypett.fullstack

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform