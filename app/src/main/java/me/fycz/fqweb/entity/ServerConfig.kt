package me.fycz.fqweb.entity

/**
 * @author fengyue
 * @date 2023/7/25 9:12
 * @description
 */
data class ServerConfig(
    var enable: Boolean? = null,
    var name: String? = null,
    var owner: String? = null,
    var frpcConfig: String? = null,
    var customDomain: String? = null,
    var uploadDomainUrl: String? = null,
    var getDomainUrl: String? = null,
)
