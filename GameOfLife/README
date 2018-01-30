Copyright David Petrie 2008

To build, type this:

% javac *.java

(Sorry - I should have included a make file but I never used one for the project).


Master Usage:

% java Master [master-port] [generations] [test-patter-file-path] [number of workers]

Example: java Master 10000 150 file.txt 4

NOTE: This usage pattern is different to that specified. This version dynamically creates a
grid for the workers - you only specify the number of workers incoming.


Worker Usage:

% java Worker [worker-port] [master-host-address] [master-port]

Example: java Worker 10001 localhost 10000


Note: For large numbers of workers (greater than 9), it may take a up to half a minute for all the worker neighbour connections to
initialise before the master "does anything." After this, it will output each state of the game to stdout.
