package com.github.dekobon.mixpanel.dump

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.cli.*
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.fluent.Request
import org.apache.http.client.utils.URIBuilder

import java.util.concurrent.TimeUnit

@CompileStatic
@Slf4j
class DumpMixpanelEvents {
    static final String API_BASE = "https://data.mixpanel.com/api/2.0/export/"
    static final long timeout = TimeUnit.DAYS.toSeconds(5) // 5 days

    static final Options OPTIONS = new Options()

    static {
        OPTIONS.addOption('h', 'help', false, 'Lists available OPTIONS')
        OPTIONS.addOption('d', 'from_date', true, 'Date to dump data from (YYYY-mm-dd)')
        OPTIONS.addOption('t', 'to_date', true, 'Date to dump data from (YYYY-mm-dd)')
        OPTIONS.addOption('k', 'api_key', true, 'Mixpanel API key')
        OPTIONS.addOption('s', 'api_secret', true, 'Mixpanel API secret')
        OPTIONS.addOption('f', 'file', true, 'File to write output to')
    }

    static void main(String[] argv) {
        // create the parser
        CommandLineParser parser = new PosixParser()

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(OPTIONS, argv)

            if (line.hasOption("help") || line.options.length == 0) {
                def hf = new HelpFormatter()
                hf.printHelp("dump_mixpanel_events", OPTIONS, true)
                System.exit(0)
            }

            def apiKey = line.getOptionValue("api_key")
            def apiSecret = line.getOptionValue("api_secret")
            def from = line.getOptionValue("from_date")
            def to = line.getOptionValue("to_date")
            def file = line.getOptionValue("file")

            println argv.inspect()
            println OPTIONS.inspect()

            dumpData(apiKey, apiSecret, from, to, file)
        }
        catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: ${exp.getMessage()}")
        }
    }

    static dumpData(String apiKey, String apiSecret, String from, String to,
                    String file) {
        def uri = generateUrl(apiKey, apiSecret, from, to)

        def response = Request.Get(uri).execute()

        if (file != null) {
            log.info("Requesting Mixpanel API: {}", uri)
            log.info("Writing data to: {}", file)
            response.saveContent(new File(file))
        } else {
            def handler = new StdOutResponseHandler()
            response.handleResponse(handler)
        }
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

    static class StdOutResponseHandler implements ResponseHandler<Void> {
        @Override
        Void handleResponse(HttpResponse response)
                throws ClientProtocolException, IOException {
            def input = response.entity.content

            ByteStreams.copy(input, System.out)

            return null
        }
    }
}
