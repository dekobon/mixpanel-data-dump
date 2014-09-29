Mixpanel Data Dump
==================

 This is a simple utility written in Groovy that dumps all Mixpanel data into
 a of JSON serialized objects delinated by new lines.
 
 For more information, see the (Mix Panel documentation)[https://mixpanel.com/docs/api-documentation/exporting-raw-data-you-inserted-into-mixpanel].
  
 To setup to run from Linux:
 
 ```bash
 chmod +x ./bin/dump_mixpanel_events
 chmod +x ./gradlew
 ```
 
 Then run as so to get help for the CLI:
 
 ```bash
 ./bin/dump_mixpanel_events -help
 usage: dump_mixpanel_events [-d <arg>] [-f <arg>] [-h] [-k <arg>] [-s
        <arg>] [-t <arg>]
  -d,--from_date <arg>    Date to dump data from (YYYY-mm-dd)
  -f,--file <arg>         File to write output to
  -h,--help               Lists available OPTIONS
  -k,--api_key <arg>      Mixpanel API key
  -s,--api_secret <arg>   Mixpanel API secret
  -t,--to_date <arg>      Date to dump data from (YYYY-mm-dd)

 ```

 Note that id a file name is not specified data will be written to STDOUT.
