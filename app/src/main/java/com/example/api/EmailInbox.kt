package com.example.api

import javax.mail.Folder
import javax.mail.Session
import javax.mail.internet.MimeMultipart

object EmailInbox {

    data class EmailThread(
        val from: String,
        val fromAddress: String,
        val subject: String,
        val sentDate: Long,
        val snippet: String,
        val text: String
    )

    fun defaultHost(address: String): String {
        val a = address.trim().lowercase()
        return when {
            a.endsWith("gmail.com") || a.endsWith("googlemail.com") -> "imap.gmail.com"
            a.endsWith("outlook.com") || a.endsWith("hotmail.com") || a.endsWith("live.com") || a.endsWith("msn.com") -> "outlook.office365.com"
            a.endsWith("yahoo.com") -> "imap.mail.yahoo.com"
            a.endsWith("icloud.com") || a.endsWith("me.com") || a.endsWith("mac.com") -> "imap.mail.me.com"
            else -> ""
        }
    }

    fun load(address: String, password: String, customHost: String, max: Int = 25): List<EmailThread> {
        val addr = address.trim()
        val host = customHost.trim().ifBlank { defaultHost(addr) }
        if (addr.isBlank() || password.isBlank() || host.isBlank()) return emptyList()
        val threads = mutableListOf<EmailThread>()
        val fmt = java.text.SimpleDateFormat("d MMM yyyy HH:mm", java.util.Locale.getDefault())
        try {
            val props = java.util.Properties()
            props["mail.store.protocol"] = "imaps"
            props["mail.imaps.host"] = host
            props["mail.imaps.port"] = "993"
            props["mail.imaps.ssl.enable"] = "true"
            val session = Session.getInstance(props, null)
            val store = session.getStore("imaps")
            store.connect(host, addr, password)
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            val count = inbox.messageCount
            val start = maxOf(1, count - max + 1)
            for (i in start..count) {
                try {
                    val m = inbox.getMessage(i)
                    val subject = m.subject ?: "(no subject)"
                    val ia = (m.from ?: emptyArray()).filterIsInstance<javax.mail.internet.InternetAddress>().firstOrNull()
                    val fromAddress = ia?.address ?: ""
                    val fromName = ia?.personal?.takeIf { p -> p.isNotBlank() } ?: fromAddress
                    val sent = m.sentDate?.time ?: m.receivedDate?.time ?: 0L
                    val body = extractText(m.content)
                    threads.add(
                        EmailThread(
                            from = fromName.ifBlank { "Unknown sender" },
                            fromAddress = fromAddress,
                            subject = subject,
                            sentDate = sent,
                            snippet = body.replace("\n+", " ").trim().take(90),
                            text = "Email from $fromName <$fromAddress>\nSubject: $subject\nDate: ${fmt.format(java.util.Date(sent))}\n\n${body.trim().take(12000)}"
                        )
                    )
                } catch (e: Exception) {
                }
            }
            inbox.close(false)
            store.close()
        } catch (e: Exception) {
            return emptyList()
        }
        return threads.sortedByDescending { it.sentDate }
    }

    private fun extractText(content: Any?): String {
        return try {
            when (content) {
                is String -> content
                is MimeMultipart -> {
                    val sb = StringBuilder()
                    for (i in 0 until content.count) {
                        val bp = content.getBodyPart(i)
                        if (bp.isMimeType("text/plain")) {
                            sb.append(bp.content as? String ?: "")
                        } else if (bp.isMimeType("multipart/*")) {
                            sb.append(extractText(bp.content))
                        }
                    }
                    sb.toString()
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
