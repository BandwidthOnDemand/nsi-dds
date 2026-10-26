package net.es.nsi.dds.util;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.Locale;
import org.apache.hc.client5.http.utils.DateUtils;

/**
 * HTTP date headers (RFC 9110 section 5.6.7): write RFC 1123, read all three formats.
 */
public final class HttpDate {
    // HttpClient's asctime formatter has no zone and pads the day with a zero, so it parses nothing.
    private static final DateTimeFormatter ASCTIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("EEE MMM ppd HH:mm:ss yyyy")
            .toFormatter(Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    private HttpDate() {
    }

    public static String format(Date date) {
        return DateUtils.formatStandardDate(date.toInstant());
    }

    public static String format(long epochMillis) {
        return format(new Date(epochMillis));
    }

    /**
     * @return the date, or null if the header is absent or unparseable (treated as no condition).
     */
    public static Date parse(String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }
        return DateUtils.toDate(DateUtils.parseDate(header,
                DateUtils.FORMATTER_RFC1123, DateUtils.FORMATTER_RFC1036, ASCTIME));
    }
}
