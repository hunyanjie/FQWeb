package me.fycz.fqweb.entity

/**
 * @author fengyue
 * @date 2023/7/25 9:12
 * @description
 */
data class ServerConfig(
    var enable: Boolean? = null,
    var name: String? = null,
    var owner: String? = null,//可空
    var frpcConfig: String? = null,
    var customDomain: String? = null,
    var uploadDomainUrl: String? = null,
    var getDomainUrl: String? = null,
) {
    fun check(): Boolean {
        if (enable != true) return false
        if (name.isNullOrEmpty()) return false
        if (frpcConfig.isNullOrEmpty()) return false
        if (customDomain.isNullOrEmpty()) return false
        if (uploadDomainUrl.isNullOrEmpty()) return false
        if (getDomainUrl.isNullOrEmpty()) return false
        return true
    }
}
