import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.cli.*
import org.apache.http.client.fluent.Request
import org.apache.http.client.utils.URIBuilder

import java.util.concurrent.TimeUnit

@CompileStatic
@Slf4j
class DumpMixpanelEvents {
    static final String API_BASE = "https://data.mixpanel.com/api/2.0/export/"
    static final long timeout = TimeUnit.DAYS.toSeconds(5) // 5 days

    static final List<Option> OPTION_LIST = [
            OptionBuilder.withArgName("h")
                         .withLongOpt("help")
                         .withDescription("Lists available options")
                         .isRequired(false)
                         .create(),
            OptionBuilder.withArgName("d")
                         .withLongOpt("from_date")
                         .withDescription("Date to dump data from (YYYY-mm-dd)")
                         .isRequired(true)
                         .create(),
            OptionBuilder.withArgName("t")
                         .withLongOpt("to_date")
                         .withDescription("Date to dump data to (YYYY-mm-dd)")
                         .isRequired(true)
                         .create(),
            OptionBuilder.withArgName("k")
                         .withLongOpt("api_key")
                         .withDescription("Mixpanel API key")
                         .isRequired(true)
                         .create(),
            OptionBuilder.withArgName("s")
                         .withLongOpt("api_secret")
                         .withDescription("Mixpanel API secret")
                         .isRequired(true)
                         .create(),
            OptionBuilder.withArgName("f")
                         .withLongOpt("file")
                         .withDescription("File to write output to")
                         .isRequired(false)
                         .create()
    ].asImmutable()

    static void main(String[] argv) {
        // create the parser
        def parser = new GnuParser()
        def options = new Options()
        OPTION_LIST.each { options.addOption(it) }

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, argv);
        }
        catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: ${exp.getMessage()}")
        }
    }

    static dumpData() {
        def apiKey = System.getProperty("api_key")
        def apiSecret = System.getProperty('api_secret')

        def from = '2014-09-27'
        def to = '2014-09-27'

        def uri = generateUrl(apiKey, apiSecret, from, to)
        log.info("Requesting Mixpanel API: {}", uri)

        def response = Request.Get(uri).execute()
        response.saveContent(new File("/tmp/mix_data.json"))
    }

    static long epoc() {
        def cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        (cal.getTimeInMillis() / 1000L).longValue()
    }

    static URI generateUrl(String apiKey, String apiSecret, String from,
                       String to) {
        def params = new TreeMap<String, String>(
                ["api_key" : apiKey,
                 "expire" : (epoc() + timeout).toString(),
                 "from_date" : from,
                 "to_date" : to]
        )

        def sig = generateSig(apiSecret, params)
        params.put("sig", sig)

        def builder = new URIBuilder(API_BASE)

        params.each { builder.addParameter(it.key, it.value) }

        builder.build()
    }

    static String generateSig(String apiSecret, SortedMap<String, String> params) {
        def argsConcat = params.collect { "${it.key}=${it.value}" }.join('') +
                         apiSecret

        Hashing.md5()
               .newHasher()
               .putString(argsConcat, Charsets.UTF_8)
               .hash()
               .toString()
    }
}
