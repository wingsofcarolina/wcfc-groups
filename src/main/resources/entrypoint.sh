#!/bin/sh

show_help() {
  echo "Usage : ./entrypoint.sh [-d] [-s] [-x ip-address] [/bin/sh]"
  echo "      : The -d flag enables debugging without suspending first"
  echo "      : The -s flag enables debugging suspending the JVM upon startup"
  echo "      : The -x flag enables JMX on port 1099 with the specified IP as host"
  echo "      : If the word '/bin/sh' is provided as a parameter, a Shell is executed instead."
}

OPTIND=1         # Reset in case getopts has been used previously in the shell.
jmx=0
debug=0
pause=0
DEBUG_ARGS=""
while getopts "h?dsx:" opt; do
    case "$opt" in
    h|\?)
        show_help
        exit 0
        ;;
    d)  debug=1
    	echo "Enabling debugging, no pause"
        ;;
    s)  pause=1
    	echo "Enabling debugging, pausing after boot"
        ;;
    x)  jmx=1
    	ip=${OPTARG}
    	echo "Enabling JMX with host $ip"
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

if [ $debug == 1 ]; then
  export LOGLEVEL=DEBUG  # If we are connecting a debugger, lets also have debug logging output
  DEBUG_ARGS="-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n"
elif [ $pause == 1 ]; then
  export LOGLEVEL=DEBUG  # If we are connecting a debugger, lets also have debug logging output
  DEBUG_ARGS="-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y"
fi

if [ $jmx == 1 ]; then
  DEBUG_ARGS="$DEBUG_ARGS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.rmi.port=1099 -Djava.rmi.server.hostname=$ip"
fi

if [ "$1" == "/bin/sh" ]; then
  exec /bin/sh
else
  exec java --add-opens java.base/java.lang=ALL-UNNAMED ${DEBUG_ARGS} -jar groups.jar
fi
