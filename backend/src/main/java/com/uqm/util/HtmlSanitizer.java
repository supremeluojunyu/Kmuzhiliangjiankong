package com.uqm.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("span")
            .addAttributes("a", "href", "title", "target")
            .addAttributes(":all", "class")
            .addProtocols("a", "href", "http", "https", "mailto");

    /**
     * 消毒富文本 HTML；纯文本原样返回。
     */
    public String sanitize(String html) {
        if (!StringUtils.hasText(html)) {
            return html;
        }
        if (!html.contains("<")) {
            return html;
        }
        return Jsoup.clean(html, SAFELIST);
    }
}
