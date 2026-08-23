package io.github.seancheng.searchbyimage.data.db

import java.net.IDN
import java.net.InetAddress
import java.net.URI

object EndpointValidator {
    fun validate(rawUrl: String): Result<URI> = runCatching {
        val uri = URI(rawUrl.trim())
        require(uri.scheme.equals("https", ignoreCase = true)) { "仅支持 HTTPS 端点" }
        require(uri.userInfo == null) { "端点不能包含用户名或密码" }
        require(uri.fragment == null) { "端点不能包含片段" }
        val host = uri.host?.let(IDN::toASCII)?.lowercase()
        require(!host.isNullOrBlank()) { "端点缺少有效域名" }
        require(host != "localhost" && !host.endsWith(".localhost") && !host.endsWith(".local")) {
            "不能访问本机或局域网端点"
        }
        parseLiteralAddress(host)?.let { address ->
            require(isPublic(address)) { "不能访问回环、私网或链路本地地址" }
        }
        require(uri.port == -1 || uri.port == 443) { "仅允许标准 HTTPS 端口 443" }
        uri
    }

    fun isPublic(address: InetAddress): Boolean = !(
        address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        )

    private fun parseLiteralAddress(host: String): InetAddress? {
        val looksLikeIpv4 = host.matches(Regex("[0-9.]+"))
        val looksLikeIpv6 = ':' in host
        return if (looksLikeIpv4 || looksLikeIpv6) runCatching { InetAddress.getByName(host) }.getOrNull() else null
    }
}
