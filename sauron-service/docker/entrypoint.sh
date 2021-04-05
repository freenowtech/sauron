# Start the Java Process
echo "Starting with SIGTERM trap"
trap delay_sigterm SIGTERM
exec /usr/bin/java ${JAVA_OPTS} -jar -Dpf4j.pluginsDir=/sauron/plugins /sauron/sauron-service.jar &
export PID=$!
export EXIT_STATUS=$?

wait $PID
trap - TERM INT
wait $PID