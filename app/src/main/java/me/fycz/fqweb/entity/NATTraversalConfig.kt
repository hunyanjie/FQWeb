package me.fycz.fqweb.entity

/**
 * @author fengyue
 * @date 2023/7/25 9:09
 * @description
 */
data class NATTraversalConfig(
    var enable: Boolean? = null,
    var minVersion: Int? = null,
    var minVersionName: String? = null,
    var servers: List<ServerConfig>? = null,
)
