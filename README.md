# CNV-A07_Sudoku_Cloud

## Running server with instrumented solvers using our metric:
1. copy BIT/highBIT and BIT/lowBIT into BIT folder
2. Compile metrics class:
```
javac BIT/serverMetrics/MetricsTool.java 
```
3. Instrument solver classes:
```
java BIT.serverMetrics.MetricsTool pt/ulisboa/tecnico/cnv/solver/ pt/ulisboa/tecnico/cnv/solver
```
4. Compile server:
```
javac pt/ulisboa/tecnico/cnv/server/WebServer.java 
```
5. Run server:
```
java pt.ulisboa.tecnico.cnv.server.WebServer 
```
6. Instrumentation results are going to be on logs/ folder.
